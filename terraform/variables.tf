variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-central-1"  # Frankfurt - close to Romania
}

variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "ssl-monitor"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "db_username" {
  description = "Database username"
  type        = string
  default     = "sslmonitor"
}

variable "db_password" {
  description = "Database password"
  type        = string
  default     = "password123"
  sensitive   = true
}

variable "notification_email" {
  description = "Email address for SSL certificate notifications"
  type        = string
}