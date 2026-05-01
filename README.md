# User Service

User profile and social graph service for the HiveMind platform. Manages user profiles, follow/unfollow relationships, and user search.

## Details

| Property | Value |
|----------|-------|
| **Port** | `8082` |
| **Database** | Cassandra |
| **Cache** | Redis |
| **Messaging** | Kafka |
| **Role** | User Profiles + Follow |

## Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/*.jar

# Docker
docker build -t hivemind/user-service .
```

## Links

- [Main Repository](https://github.com/AhmedNijim92/hivemind-backend)
