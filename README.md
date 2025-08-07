# SSL Domain Monitor

A Spring Boot application that checks SSL certificate expiry and manages domain health.

## Features

- SSL certificate expiry monitoring
- Configurable alert thresholds (7, 30, 90 days)
- Historical tracking and database persistence

## Tech Stack

- **Java 21** with Spring Boot 3.x
- **PostgreSQL** for data persistence
- **Docker** for containerization
- **Terraform** for infrastructure as code

## Quick Start

### Prerequisites

- Java 21+
- Docker and Docker Compose

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/andreigabor21/domain-ssl-monitor.git
   cd domain-ssl-monitor
   ```

2. **Start the application with Docker Compose**
   ```bash
   docker compose up
   ```

3. **Access the application**
    - Health check endpoint: http://localhost:8080/api/v1/domains/health

   
## API Documentation

### Base URL
```
http://localhost:8080/api/v1/domains
```

### Endpoints

#### 1. Health Check
```http
GET /api/v1/domains/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "Domain SSL Monitor"
}
```

#### 2. Check SSL Certificates (Synchronous)
```http
POST /api/v1/domains/check
Content-Type: application/json

{
  "domains": ["google.com", "facebook.com"]
}
```

**Response:**
```json
[
   {
      "domain": "google.com",
      "expiryDate": "2025-09-29T08:34:02",
      "daysUntilExpiry": 52,
      "alertLevel": "INFO",
      "lastChecked": "2025-08-07T11:52:38.375228798",
      "error": null,
      "valid": true
   },
   {
      "domain": "facebook.com",
      "expiryDate": "2025-08-14T23:59:59",
      "daysUntilExpiry": 7,
      "alertLevel": "CRITICAL",
      "lastChecked": "2025-08-07T11:52:38.55992134",
      "error": null,
      "valid": true
   }
]
```

#### 3. Check SSL Certificates (Asynchronous)
```http
POST /api/v1/domains/check-async
Content-Type: application/json

{
  "domains": ["example.com", "test.com", "another.com"]
}
```

**Response:** Same format as synchronous check, but processed asynchronously for better performance with large domain lists.

#### 4. Get Domains Expiring Soon
```http
GET /api/v1/domains/expiring?days=N
```

**Query Parameters:**
- `days` (optional): Number of days (default is 30)

**Response:**
```json
[
   {
      "domain": "facebook.com",
      "expiryDate": "2025-08-14T23:59:59",
      "daysUntilExpiry": 7,
      "alertLevel": "CRITICAL",
      "lastChecked": "2025-08-07T11:52:38.570394",
      "error": null,
      "valid": true
   }
]
```

#### 5. Get Domain Certificate History (Paginated)
```http
GET /api/v1/domains/{domainName}/history?page=0&size=20
```

**Path Parameters:**
- `domainName`: The domain name to get history for

**Query Parameters:**
- `page` (optional): Page number (default is 0)
- `size` (optional): Page size (default is 20)

**Response:**
```json
{
   "content": [
      {
         "id": 7,
         "checkTime": "2025-08-06T13:09:57.306606",
         "expiryDate": "2025-09-24T15:49:08",
         "issuer": "CN=DigiCert Secure Site ECC CA-1,OU=www.digicert.com,O=DigiCert Inc,C=US",
         "subject": "CN=www.netflix.com,O=Netflix,L=Los Gatos,ST=California,C=US",
         "errorMessage": null,
         "valid": true,
         "daysUntilExpiry": 48,
         "alertLevel": "INFO"
      }
   ],
   "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
         "empty": true,
         "sorted": false,
         "unsorted": true
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
   },
   "last": true,
   "totalElements": 1,
   "totalPages": 1,
   "size": 20,
   "number": 0,
   "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
   },
   "first": true,
   "numberOfElements": 1,
   "empty": false
}
```

### Error Responses

**400 Bad Request:**
```json
{
   "error": "Validation Failed",
   "errors": {
      "domains": "Domains list cannot be empty"
   },
   "timestamp": "2025-08-07T11:55:43.041489092",
   "status": 400
}
```

**404 Not Found:**
```json
{
   "error": "Not Found",
   "message": "Domain not found: asdf.com",
   "timestamp": "2025-08-07T11:56:40.016708091",
   "status": 404
}
```

**500 Internal Server Error:**
```json
{
  "error": "SSL check failed",
  "message": "Unable to connect to domain: connection timeout",
  "timestamp": "2025-08-07T10:30:00Z",
  "status": 500
}
```


## AWS Infrastructure

### Live Deployment
The SSL Monitor application is deployed on AWS infrastructure and accessible at:
**http://3.122.216.133:8080**

### Infrastructure Components

#### Core Services
- **EC2 Instance**: Hosts the Java application
- **RDS PostgreSQL**: Database for persistence

#### Monitoring & Alerts
- **AWS Lambda**: Python function for automated certificate monitoring
- **Amazon SNS**: Email notification service
- **CloudWatch Events**: Scheduled triggers (every 2 days)

### Automated Alert System

The system includes a fully functional automated alerting mechanism:

![SSL Certificate Alert Email](/pictures/ssl-alert-email-screenshot.png)

**Alert Configuration:**
- **Trigger**: Every 2 days
- **API Endpoint**: `/api/v1/domains/expiring?days=60`
- **Notification**: AWS SNS email
- **Example Alert**: "SSL Alert: 6 certificates expiring soon"