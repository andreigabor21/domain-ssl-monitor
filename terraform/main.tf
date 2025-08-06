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

  # Simple startup script
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