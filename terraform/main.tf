terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Security Group for Application
resource "aws_security_group" "ssl_monitor_app_sg" {
  name        = "ssl-monitor-app-sg"
  description = "Security group for SSL Monitor application"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "ssl-monitor-app-sg"
  }
}

# Security Group for Database
resource "aws_security_group" "ssl_monitor_db_sg" {
  name        = "ssl-monitor-db-sg"
  description = "Security group for SSL Monitor database"
  vpc_id      = data.aws_vpc.default.id

  # Only allow access from application
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ssl_monitor_app_sg.id]
  }

  tags = {
    Name = "ssl-monitor-db-sg"
  }
}

resource "aws_db_instance" "ssl_monitor_db" {
  identifier = "ssl-monitor-db"
  engine         = "postgres"
  engine_version = "15"
  instance_class = "db.t3.micro"
  allocated_storage = 20
  storage_type      = "gp2"
  db_name  = "sslmonitor"
  username = var.db_username
  password = var.db_password
  vpc_security_group_ids = [aws_security_group.ssl_monitor_db_sg.id]
  publicly_accessible = true
  backup_retention_period = 0
  skip_final_snapshot    = true

  tags = {
    Name = "ssl-monitor-db"
  }
}

resource "aws_iam_role" "ec2_session_manager_role" {
  name = "ssl-monitor-ec2-session-manager-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "ssl-monitor-ec2-session-manager-role"
  }
}

resource "aws_iam_role_policy_attachment" "session_manager_policy" {
  role       = aws_iam_role.ec2_session_manager_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "ssl-monitor-ec2-profile"
  role = aws_iam_role.ec2_session_manager_role.name
}

# EC2 instance to run the Java app
resource "aws_instance" "ssl_monitor_app" {
  ami                    = "ami-0e04bcbe83a83792e"
  instance_type          = "t3.micro"
  associate_public_ip_address = true

  vpc_security_group_ids = [aws_security_group.ssl_monitor_app_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  # Startup script which install Docker
  user_data = <<-EOF
              #!/bin/bash
              yum update -y
              yum install -y docker
              systemctl start docker
              systemctl enable docker
              usermod -a -G docker ec2-user

              # Install Docker Compose
              curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              chmod +x /usr/local/bin/docker-compose
              EOF

  tags = {
    Name = "ssl-monitor-app"
  }
}


### Lambda implementation


# SNS Topic
resource "aws_sns_topic" "ssl_alerts" {
  name = "ssl-certificate-alerts"

  tags = {
    Name = "ssl-certificate-alerts"
  }
}

# email provided only in terraform apply -var="notification_email=example@domain.com"
resource "aws_sns_topic_subscription" "ssl_alerts_email" {
  topic_arn = aws_sns_topic.ssl_alerts.arn
  protocol  = "email"
  endpoint  = var.notification_email
}

resource "aws_iam_role" "lambda_ssl_monitor_role" {
  name = "lambda-ssl-monitor-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "lambda-ssl-monitor-role"
  }
}

resource "aws_iam_role_policy" "lambda_ssl_monitor_policy" {
  name = "lambda-ssl-monitor-policy"
  role = aws_iam_role.lambda_ssl_monitor_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = aws_sns_topic.ssl_alerts.arn
      }
    ]
  })
}

# Lambda
resource "aws_lambda_function" "ssl_monitor_checker" {
  filename         = "ssl_monitor_lambda.zip"
  function_name    = "ssl-monitor-checker"
  role            = aws_iam_role.lambda_ssl_monitor_role.arn
  handler         = "lambda_function.lambda_handler"
  runtime         = "python3.9"
  timeout         = 30

  environment {
    variables = {
      SSL_MONITOR_URL = "http://3.122.216.133:8080/api/v1/domains/expiring?days=60"
      SNS_TOPIC_ARN   = aws_sns_topic.ssl_alerts.arn
    }
  }

  depends_on = [data.archive_file.lambda_zip]

  tags = {
    Name = "ssl-monitor-checker"
  }
}

data "archive_file" "lambda_zip" {
  type        = "zip"
  output_path = "ssl_monitor_lambda.zip"
  source {
    content = <<EOF
import json
import urllib3
import boto3
import os

def lambda_handler(event, context):
    print("Starting SSL certificate check...")

    try:
        # Get environment variables
        ssl_monitor_url = os.environ['SSL_MONITOR_URL']
        sns_topic_arn = os.environ['SNS_TOPIC_ARN']

        # Call your SSL Monitor API
        http = urllib3.PoolManager()
        response = http.request('GET', ssl_monitor_url, timeout=10)

        print(f"API Response status: {response.status}")

        if response.status == 200:
            # Parse the response
            domains_data = json.loads(response.data.decode('utf-8'))
            print(f"Domains data: {domains_data}")

            # Check if there are expiring domains
            if domains_data and len(domains_data) > 0:
                # Send SNS notification
                sns = boto3.client('sns')

                domain_count = len(domains_data)
                domain_names = [domain.get('domain', 'Unknown') for domain in domains_data]

                message = f"""SSL Certificate Alert!

{domain_count} domain(s) expiring soon:
{', '.join(domain_names)}

Please check and renew certificates as needed.
                """

                sns.publish(
                    TopicArn=sns_topic_arn,
                    Message=message,
                    Subject=f'SSL Alert: {domain_count} certificates expiring soon'
                )

                print(f"Alert sent for {domain_count} expiring domains")
                return {
                    'statusCode': 200,
                    'body': json.dumps(f'Alert sent for {domain_count} expiring domains')
                }
            else:
                print("No domains expiring soon")
                return {
                    'statusCode': 200,
                    'body': json.dumps('No domains expiring soon')
                }
        else:
            print(f"Failed to call SSL Monitor API: {response.status}")
            return {
                'statusCode': response.status,
                'body': json.dumps('Failed to call SSL Monitor API')
            }

    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps(f'Error: {str(e)}')
        }
EOF
    filename = "lambda_function.py"
  }
}

# trigger the Lambda every 2 days
resource "aws_cloudwatch_event_rule" "ssl_monitor_schedule" {
  name                = "ssl-monitor-schedule"
  description         = "Trigger SSL monitor check every 2 days"
  schedule_expression = "rate(2 days)"

  tags = {
    Name = "ssl-monitor-schedule"
  }
}

resource "aws_cloudwatch_event_target" "lambda_target" {
  rule      = aws_cloudwatch_event_rule.ssl_monitor_schedule.name
  target_id = "ssl-monitor-lambda-target"
  arn       = aws_lambda_function.ssl_monitor_checker.arn
}

resource "aws_lambda_permission" "allow_cloudwatch" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.ssl_monitor_checker.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.ssl_monitor_schedule.arn
}