# HiveMind User Service

> Manages user profiles, avatars, privacy settings, and the follower/following social graph.

## Overview

The User Service handles all user profile operations including CRUD, avatar and cover photo management, bio updates, and a privacy toggle (`showContactInfo`). It powers the follower/following system and uses Cassandra for persistence with Redis caching. A `CassandraMigrationInitializer` automatically adds `cover_picture_url` and `show_contact_info` columns on startup for seamless schema evolution.

## Features

- User profile CRUD (display name, bio, avatar, cover photo)
- Privacy toggle: `showContactInfo` controls contact visibility
- Follow / unfollow system
- Followers and following lists
- Schema auto-migration on startup via `CassandraMigrationInitializer`
- Redis caching for profile lookups

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/users/{id}` | JWT | Get user profile |
| PUT | `/api/v1/users/{id}` | JWT | Update user profile |
| POST | `/api/v1/users/{id}/follow` | JWT | Follow a user |
| DELETE | `/api/v1/users/{id}/follow` | JWT | Unfollow a user |
| GET | `/api/v1/users/{id}/followers` | JWT | List followers |
| GET | `/api/v1/users/{id}/following` | JWT | List following |

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | Service port | `8082` |
| `spring.cassandra.contact-points` | Cassandra hosts | `localhost` |
| `spring.cassandra.keyspace-name` | Cassandra keyspace | `hivemind` |
| `spring.data.redis.host` | Redis host | `localhost` |
| `eureka.client.serviceUrl.defaultZone` | Eureka registry URL | `http://localhost:8761/eureka` |

## Tech Stack

- Java 17
- Spring Boot 3.x
- Apache Cassandra
- Redis
- Eureka Client
- Maven

## Docker

```
Port: 8082
Base image: eclipse-temurin:17-jre-alpine
JVM flags: -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC
User: non-root (spring)
```

## CI/CD

- **Build**: Maven `clean package` with JDK 17 (Temurin)
- **Test**: Unit tests run during build phase
- **Docker**: Build and push to Docker Hub on `main` branch merge
- **Security**: Trivy vulnerability scan (CRITICAL, HIGH) on built image
