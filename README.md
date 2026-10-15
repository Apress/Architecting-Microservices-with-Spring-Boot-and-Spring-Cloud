# Spring Boot and Spring Cloud Microservices

This repository contains the complete code examples for the book "Spring Boot and Spring Cloud Microservices" using an Electric Vehicle (EV) charging platform as the practical implementation context.

## Book Overview

Learn how to build enterprise-grade microservices using Spring Boot 3.5.6 and Spring Cloud 2025.0.0 through the development of a real-world EV charging management system called **ChargeRoute**.

## Book Structure (12 Chapters)

The book is organized into four parts with streamlined content focusing on essential microservices patterns:

### Part I: Foundations (Chapters 1-3)
- **Chapter 1**: Microservices Introduction
- **Chapter 2**: Spring Boot Microservices
- **Chapter 3**: Business Case - EV Charging Platform

### Part II: Core Infrastructure (Chapters 4-7)
- **Chapter 4**: Service Discovery with Netflix Eureka
  - `v1/`: Skeleton services with hardcoded URLs (demonstrates the problem)
  - `v2/`: Eureka-enabled services with service discovery (demonstrates the solution)
- **Chapter 5**: API Gateway with Spring Cloud Gateway
- **Chapter 6**: Configuration Management with Spring Cloud Config
- **Chapter 7**: Security Basics

### Part III: Communication and Data (Chapters 8-10)
- **Chapter 8**: Data Management and Persistence
- **Chapter 9**: RESTful APIs with WebFlux (Reactive)
- **Chapter 10**: Event-Driven Architecture

### Part IV: Operations and Deployment (Chapters 11-12)
- **Chapter 11**: Observability Essentials
- **Chapter 12**: Local Kubernetes Deployment

## ChargeRoute Platform Services

The example application demonstrates a complete EV charging management platform with four core microservices:

- **User Management Service** (Port 8082) - Handles user accounts and memberships
- **Charge Unit Management Service** (Port 8081) - Manages charging station units
- **Session Management Service** (Port 8083) - Manages active charging sessions
- **Notification Management Service** (Port 8084) - Sends notifications to users

## Prerequisites

- Java 21 or later
- Maven 3.8+ or Gradle 7+
- Docker and Docker Compose
- Your favorite IDE (IntelliJ IDEA recommended)

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/brianeno/sc-book.git
   cd sc-book
   ```

2. **Start with Chapter 4** (Service Discovery)
   ```bash
   cd chapter-04-service-discovery
   
   # Version 1: Skeleton services with hardcoded URLs (demonstrates the problem)
   cd v1/scripts
   ./build-all.sh
   ./start-all.sh
   
   # Version 2: Eureka-enabled services (demonstrates the solution)
   cd ../../v2
   docker-compose up -d
   ```

3. **Follow the chapter README** for specific setup instructions

## Technology Stack

- **Spring Boot**: 3.5.6
- **Spring Cloud**: 2025.0.0
- **Java**: 21 with virtual threads support
- **Build Tool**: Maven 3.8+
- **Database**: PostgreSQL, MongoDB
- **Message Broker**: Spring Cloud Stream
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Configuration**: Spring Cloud Config
- **Monitoring**: Micrometer, Prometheus, Grafana
- **Tracing**: Spring Cloud Sleuth, Zipkin
- **Deployment**: Docker, Kubernetes (kind/minikube)

## Progressive Learning Approach

The 12-chapter structure builds progressively:

1. **Part I - Foundations** (Ch 1-3): Understand microservices architecture and business context
2. **Part II - Core Infrastructure** (Ch 4-7): Build essential infrastructure components
3. **Part III - Communication & Data** (Ch 8-10): Implement data persistence and communication patterns
4. **Part IV - Operations** (Ch 11-12): Add observability and deploy to Kubernetes

## Running the Complete Platform

After working through all chapters, you can run the complete ChargeRoute platform:

```bash
# From the repository root
cd chapter-12-kubernetes
docker-compose up -d

# Or with Kubernetes
cd chapter-12-kubernetes
kubectl apply -f k8s/
```

## Support

For questions about the code examples or book content, please create an issue in this repository.

## License

This code is provided for educational purposes in conjunction with the book "Microservices with Spring Boot and Spring Cloud."
