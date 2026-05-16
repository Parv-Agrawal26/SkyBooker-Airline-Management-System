# ✈️ SkyBooker Airline Management System

A complete microservices-based airline reservation and management system built using Spring Boot, REST APIs, JWT authentication, API Gateway architecture, and distributed backend services.

SkyBooker is designed as a scalable airline ecosystem where every major module is separated into independent microservices for better scalability, maintainability, and deployment flexibility.

---

# 🚀 System Overview

SkyBooker follows a distributed microservices architecture where each service handles a dedicated business domain.

The project includes:

- Authentication Service
- Airline Management Service
- Flight Management Service
- Passenger Management Service
- Seat Management Service
- Payment Service
- Notification Service
- Flight Booking Service
- API Gateway

---

# 🧱 Microservices Architecture

```text
Client Application
        │
        ▼
   API Gateway
        │
 ┌──────┼────────┬────────┬────────┬────────┐
 ▼      ▼        ▼        ▼        ▼        ▼
Auth  Flight   Seat    Payment  Passenger Notification
Svc   Svc      Svc     Svc      Svc        Svc
        │
        ▼
  Booking Service
        │
        ▼
 Airline Service
```

---

# 📦 Services Included

## 🔐 Auth Service

Handles authentication and authorization.

### Features

- User registration
- JWT token generation
- Login authentication
- Role-based access control
- Secure password handling
- Protected API support
- Authorization filters

### Supported Roles

- ADMIN
- STAFF
- PASSENGER

---

## ✈️ Flight Service

Responsible for managing flights and schedules.

### Features

- Add flights
- Update flight details
- Delete flights
- Flight status management
- Source and destination handling
- Flight search APIs
- Flight timing management
- Airline linking support

---

## 💺 Seat Service

Handles seat inventory and reservation logic.

### Features

- Add seats to flights
- Seat availability management
- Seat class support
- Seat hold system
- Seat confirmation
- Seat release mechanism
- Dynamic seat pricing

### Seat Categories

- Economy
- Business
- First Class

---

## 👤 Passenger Service

Manages passenger-related operations.

### Features

- Passenger creation
- Passenger retrieval
- Passenger booking mapping
- Passenger lookup APIs
- Passenger detail management

---

## 💳 Payment Service

Responsible for handling airline booking payments.

### Features

- Payment processing
- Payment verification
- Booking payment integration
- Transaction management
- Payment status tracking
- Razorpay integration support

---

## 📩 Notification Service

Handles user notifications and communication.

### Features

- Booking notifications
- Payment confirmation notifications
- Email notification support
- Event-driven communication

---

## 🏢 Airline Service

Handles airline management operations.

### Features

- Add airlines
- Airline data management
- Airline retrieval APIs
- Country support
- IATA and ICAO support

---

## 🎫 Flight Booking Service

Core booking orchestration service.

### Features

- Flight booking workflow
- Passenger-seat mapping
- Booking confirmation
- Payment integration
- Booking history management
- Booking status tracking

---

## 🌐 API Gateway

Central entry point for all services.

### Features

- Centralized routing
- API forwarding
- Security integration
- Authentication validation
- Request filtering
- Gateway-based architecture

---

# 🛠️ Tech Stack

## Backend

- Java
- Spring Boot
- Spring Security
- Spring Web
- Spring Data JPA
- Maven

## Security

- JWT Authentication
- Role-Based Authorization
- API Security Filters

## Database

- MySQL
- JPA/Hibernate

## API Architecture

- REST APIs
- Microservices Architecture
- API Gateway Pattern

## Build Tools

- Maven
- Maven Wrapper

---

# 📂 Project Structure

```bash
SkyBooker-Airline-Management-System/
│
├── Airline-Service/
├── Flight-Service/
├── Notification-Service/
├── Passenger-Service/
├── Payment-Service/
├── Seat-Service/
├── api-gateway/
├── auth-service/
└── flight-booking/
```

---

# 🔄 Complete Booking Workflow

```text
User Login
    ↓
Search Flights
    ↓
Select Flight
    ↓
Seat Selection
    ↓
Passenger Details
    ↓
Seat Hold
    ↓
Payment Processing
    ↓
Booking Confirmation
    ↓
Notification Service Trigger
```

---

# 🔐 Security Features

- JWT token authentication
- Secure route protection
- Role-based authorization
- API Gateway validation
- Protected service communication
- Authentication filters
- Secure login flow

---

# 🌟 Key Highlights

- Distributed microservices architecture
- Independent deployable services
- Scalable backend design
- Modular airline ecosystem
- Centralized API Gateway
- Booking orchestration workflow
- Payment integration support
- Secure authentication system
- Airline management system
- Real-time seat management support

---

# ⚙️ Installation & Setup

## Clone Repository

```bash
git clone https://github.com/your-username/skybooker-airline-management-system.git
```

---

## Navigate to Project

```bash
cd SkyBooker-Airline-Management-System
```

---

## Run Services Individually

Example:

```bash
cd auth-service
mvn spring-boot:run
```

Repeat for all services.

---

# 📦 Maven Commands

## Install Dependencies

```bash
mvn clean install
```

## Run Service

```bash
mvn spring-boot:run
```

## Build JAR

```bash
mvn clean package
```

---

# 🧪 Backend Capabilities

## Authentication APIs

- Register users
- Login users
- Generate JWT tokens
- Validate authentication

## Flight APIs

- Create flights
- Update flights
- Search flights
- Delete flights

## Seat APIs

- Create seats
- Manage availability
- Confirm bookings

## Booking APIs

- Create bookings
- Retrieve bookings
- Track booking status

## Payment APIs

- Process payments
- Verify transactions
- Track payment status

---

# 📈 Future Improvements

- Docker containerization
- Kubernetes deployment
- Service discovery
- Circuit breaker implementation
- Kafka/RabbitMQ messaging
- Distributed tracing
- Centralized logging
- Monitoring dashboards
- AI-based pricing system
- Real-time flight tracking
- Refund management
- Ticket PDF generation

---

# ☁️ Deployment Ready Architecture

The system architecture is suitable for:

- Docker Deployment
- Kubernetes Clusters
- Render Deployment
- Railway Deployment
- AWS Cloud Deployment
- Azure Deployment
- GCP Deployment

---

# 👨‍💻 Development Highlights

This project demonstrates:

- Enterprise backend architecture
- Real-world airline reservation workflow
- Microservices communication
- Secure API development
- Distributed system design
- Spring Boot backend engineering

---

# 📄 License

This project is developed for educational and development purposes.
