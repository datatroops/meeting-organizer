# Meeting Management API

## Overview

This is a scalable RESTful API built with Play Framework and Scala for managing meetings and users. The application provides functionality for creating, retrieving, updating, and deleting meetings and users, with proper validation and notification capabilities.

## Features

- User management (CRUD operations)
- Meeting management (CRUD operations)
- Real-time notifications via WebSockets
- Webhook integration for Notification systems
- Validation for meeting data (e.g., start time must be before end time)

## API Endpoints

### User Endpoints

- `GET /users` - List all users
- `GET /users/:id` - Get a specific user by ID
- `POST /users` - Create a new user
- `PUT /users/:id` - Update an existing user
- `DELETE /users/:id` - Delete a user

### Meeting Endpoints

- `GET /meetings` - List all meetings
- `GET /meetings/:id` - Get a specific meeting by ID
- `POST /meetings` - Create a new meeting
- `PUT /meetings/:id` - Update an existing meeting
- `DELETE /meetings/:id` - Delete a meeting

## Models

### User

```scala
case class User(
  id: Option[Long],
  name: String,
  email: String
)
```

### Meeting

```scala
case class Meeting(
  id: Option[Long],
  title: String,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  organizerId: Long,
  participantIds: List[Long],
  createdAt: Option[LocalDateTime] = None,
  updatedAt: Option[LocalDateTime] = None
)
```

## Notification System

The application includes a comprehensive notification system that:

- Sends real-time updates via WebSockets to Meeting Organizer
- Dispatches webhook notifications to Meeting Organizer and Participants
- Supports various event types:
    - Meeting created
    - Meeting updated
    - Meeting deleted

## Architecture

### Controllers

- `UserController`: Handles HTTP requests for user operations
- `MeetingController`: Handles HTTP requests for meeting operations

### Services

- `UserService`: Business logic for user operations
- `MeetingService`: Business logic for meeting operations
- `NotificationService`: Handles sending notifications through different channels
- `WebSocketService`: Manages WebSocket connections and messages

## Development

### Prerequisites

- JDK 11 or higher
- SBT (Scala Build Tool)
- PostgreSQL (or configured database of choice)

### Setup

1. Clone the repository
2. Configure the database connection in `conf/application.conf`
3. Run `cd Infra`to checkout directory for Docker
4. Run `docker compose up` to start the container
5. Run `sbt compile` to compile the project
6. Run `sbt run` to start the development server
7. Access the API at `http://localhost:9000`

### Testing

The project uses ScalaTest with Mockito for comprehensive unit testing:

```bash
# Run all tests
sbt test

# Run specific test suite
sbt "testOnly controllers.MeetingControllerSpec"
```

### Configuration

Key configuration options in `application.conf`:

```hocon
# Database configuration
db {
  default {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:5432/meeting_db"
    username = "postgres"
    password = "postgres"
  }
}

# Webhook configuration
webhook {
  endpoint = "https://your-webhook-endpoint.com"
}
```

## Deployment

The application can be deployed as a Docker container or as a standalone JAR file:

```bash
# Create production build
sbt dist

# The packaged application will be in target/universal/meeting-api-1.0.0.zip
```

## License

[MIT License](LICENSE)