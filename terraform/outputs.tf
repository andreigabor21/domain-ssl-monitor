output "database_endpoint" {
  description = "RDS database endpoint"
  value       = aws_db_instance.ssl_monitor_db.endpoint
}

output "database_name" {
  description = "Database name"
  value       = aws_db_instance.ssl_monitor_db.db_name
}

output "database_port" {
  description = "Database port"
  value       = aws_db_instance.ssl_monitor_db.port
}

output "region" {
  description = "AWS region"
  value       = var.aws_region
}

output "sns_topic_arn" {
  description = "SNS topic ARN for SSL alerts"
  value       = aws_sns_topic.ssl_alerts.arn
}

output "lambda_function_name" {
  description = "Lambda function name"
  value       = aws_lambda_function.ssl_monitor_checker.function_name
}