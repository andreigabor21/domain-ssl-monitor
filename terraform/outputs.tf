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