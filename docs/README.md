# User Service

> HiveMind User Profile & Social Graph Microservice

## Overview

The user-service manages user profiles and the follow/unfollow social graph. It consumes Kafka events from auth-service to auto-create profiles when users register, and provides profile CRUD plus follower/following queries.

## Service Info

| Property | Value |
|----------|-------|
| Port | 8082 |
| Service Name | `user-service` |
| Database | Apache Cassandra + Redis |
| Keyspace | `user_keyspace` |
| Spring Boot | 3.3.5 |
| Spring Cloud | 2023.0.3 |
| Java | 17 |

## Architecture

```
Client (via Gateway)
  │
  ▼
UserController
  │
  ├── IUserService (getUserById, updateProfile, follow, unfollow, getFollowers, getFollowing)
  │       ├── UserProfileRepository (Cassandra)
  │       └── FollowRepository (Cassandra)
  │
  └── UserEventConsumer
        └── Kafka Consumer ← user-created-topic
```

## API Endpoints

Base path: `/api/v1/users`
All endpoints require JWT (X-User-Id header injected by gateway).

| Method | Path | Description |
|--------|------|-------------|
| GET | `/{userId}` | Get user profile |
| PUT | `/{userId}` | Update user profile |
| POST | `/{userId}/follow/{targetUserId}` | Follow a user |
| DELETE | `/{userId}/follow/{targetUserId}` | Unfollow a user |
| GET | `/{userId}/followers` | List user's followers |
| GET | `/{userId}/following` | List users being followed |

### Request/Response Examples

#### GET /api/v1/users/{userId}
```json
// Response (200)
{
  "userId": "uuid",
  "mobileNumber": "+46701234567",
  "name": "Ahmed",
  "email": "ahmed@example.com",
  "bio": "Software developer",
  "profilePictureUrl": "https://...",
  "createdAt": "2025-06-01",
  "updatedAt": "2025-06-04"
}
```

#### PUT /api/v1/users/{userId}
```json
// Request
{
  "name": "Ahmed Updated",
  "email": "newemail@example.com",
  "bio": "Updated bio",
  "profilePictureUrl": "https://..."
}

// Response (200) — updated UserProfileDto
```

#### POST /api/v1/users/{userId}/follow/{targetUserId}
```json
// Response (200)
{ "message": "Followed successfully" }
```

## Data Model

### UserProfile (Cassandra table: `user_profiles`)

| Column | Type | Description |
|--------|------|-------------|
| user_id | UUID | Primary key |
| mobile_number | String | Phone number |
| name | String | Display name |
| email | String | Email address |
| bio | String | User biography |
| profile_picture_url | String | Avatar URL |
| created_at | LocalDate | Profile creation |
| updated_at | LocalDate | Last update |

### Follow (Cassandra table: `follows`)

| Column | Type | Key Type | Description |
|--------|------|----------|-------------|
| follower_id | UUID | PARTITION | Who is following |
| following_id | UUID | CLUSTERED | Who is being followed |
| created_at | LocalDateTime | — | When followed |

## Kafka Events

### Consumes: `user-created-topic`

When a user registers (from auth-service), the `UserEventConsumer` creates a UserProfile automatically with the name, email, and mobile number from the event.

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| CASSANDRA_HOST | localhost | Cassandra contact point |
| CASSANDRA_PORT | 9042 | Cassandra port |
| CASSANDRA_DATACENTER | datacenter1 | Cassandra datacenter |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 | Kafka brokers |
| REDIS_HOST | localhost | Redis host |
| REDIS_PORT | 6379 | Redis port |
| EUREKA_SERVER | http://localhost:8761/eureka | Eureka URL |

## Dependencies

- spring-boot-starter-web
- spring-boot-starter-data-cassandra
- spring-boot-starter-data-redis
- spring-boot-starter-cache
- spring-cloud-starter-netflix-eureka-client
- spring-cloud-starter-openfeign
- spring-kafka
- hivemind-common (1.0.0)
- lombok

## Running Locally

```bash
# Prerequisites: Cassandra, Kafka, Redis running
cd microservices/user-service
mvn spring-boot:run
```

Auto-creates `user_keyspace`, `user_profiles`, and `follows` tables on startup.

## Known Issues

1. Redis caching temporarily disabled — `GenericJackson2JsonRedisSerializer` cannot serialize `LocalDate`. Fix: register `JavaTimeModule`.
2. `ALLOW FILTERING` used in `findByMobileNumber` and `findByFollowingId` — needs secondary indexes or materialized views at scale.
