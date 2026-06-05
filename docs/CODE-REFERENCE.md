# User Service — Code-Level Reference

## UserServiceApplication

**Package:** `com.hivemind.user`

**Annotations:**
- `@SpringBootApplication` — Enables auto-configuration, component scanning, and configuration properties
- `@EnableDiscoveryClient` — Registers with Eureka service registry
- `@EnableFeignClients` — Enables Feign declarative HTTP clients for inter-service communication
- `@EnableCaching` — Enables Spring's annotation-driven cache management (backed by Redis)
- `@EnableKafka` — Enables Kafka listener annotations

**Design Pattern:** Application Entry Point (Spring Boot convention)

### Methods

#### `main(String[] args)`
- **Signature:** `public static void main(String[] args)`
- **Logic:** `SpringApplication.run(UserServiceApplication.class, args)`
- **Returns:** void

---

## CassandraConfig

**Package:** `com.hivemind.user.config`

**Extends:** `AbstractCassandraConfiguration`

**Annotations:**
- `@Configuration`

**Design Pattern:** Template Method — overrides hook methods from abstract parent

### Overridden Methods

#### `getKeyspaceName()`
- **Returns:** `"user_keyspace"`

#### `getContactPoints()`
- **Returns:** Cassandra contact points (from configuration or default `"localhost"`)

#### `getPort()`
- **Returns:** Cassandra port (default `9042`)

#### `getLocalDataCenter()`
- **Returns:** `"datacenter1"`

#### `getSchemaAction()`
- **Returns:** `SchemaAction.CREATE_IF_NOT_EXISTS` — auto-creates tables on startup

#### `getEntityBasePackages()`
- **Returns:** `new String[] { "com.hivemind.user.entity" }`

#### `getKeyspaceCreations()`
- **Logic:** Creates keyspace with:
  - Replication: `SimpleStrategy`, replication factor = 1
  - `DURABLE_WRITES = true`
- **Returns:** `List<CreateKeyspaceSpecification>`

---

## CacheConfig

**Package:** `com.hivemind.user.config`

**Annotations:**
- `@Configuration`
- `@EnableCaching`

**Design Pattern:** Factory Method — creates configured cache manager

### Beans

#### `cacheManager(RedisConnectionFactory connectionFactory)`
- **Signature:** `@Bean public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory)`
- **Logic:**
  1. Creates `RedisCacheConfiguration.defaultCacheConfig()`
  2. Sets entry TTL to 1 hour (`Duration.ofHours(1)`)
  3. Configures value serialization with `GenericJackson2JsonRedisSerializer` (JSON format in Redis)
  4. Builds `RedisCacheManager` with the connection factory and config
- **Returns:** `RedisCacheManager`

---

## KafkaConsumerConfig

**Package:** `com.hivemind.user.config`

**Annotations:**
- `@Configuration`

**Design Pattern:** Factory Method — creates Kafka consumer infrastructure

### Beans

#### `consumerFactory()`
- **Signature:** `@Bean public ConsumerFactory<String, UserCreatedEvent> consumerFactory()`
- **Logic:** Configures consumer with:
  - `bootstrap.servers` from application properties
  - `group.id`: `"user-service"`
  - `auto.offset.reset`: `"earliest"`
  - Key deserializer: `StringDeserializer`
  - Value deserializer: `JsonDeserializer<UserCreatedEvent>`
  - Trusted packages: `"*"` (all packages trusted for deserialization)
  - `spring.json.remove.type.headers`: `false` (preserves type information)
- **Returns:** `DefaultKafkaConsumerFactory<String, UserCreatedEvent>`

#### `kafkaListenerContainerFactory()`
- **Signature:** `@Bean public ConcurrentKafkaListenerContainerFactory<String, UserCreatedEvent> kafkaListenerContainerFactory()`
- **Logic:** Creates factory, sets the `consumerFactory()`
- **Returns:** `ConcurrentKafkaListenerContainerFactory<String, UserCreatedEvent>`

---

## UserEventConsumer

**Package:** `com.hivemind.user.consumer`

**Annotations:**
- `@Component`

**Design Pattern:** Event-Driven Consumer — reacts to domain events from Kafka

### Fields (Constructor Injection)

| Field | Type |
|-------|------|
| userProfileRepository | UserProfileRepository |

### Methods

#### `handleUserCreated(UserCreatedEvent event)`
- **Signature:** `@KafkaListener(topics = "user-created-topic", groupId = "user-service") public void handleUserCreated(UserCreatedEvent event)`
- **Logic:**
  1. Checks if a UserProfile already exists for `event.getUserId()` (idempotency guard)
  2. If profile already exists → logs and returns (no-op)
  3. If not exists: creates new `UserProfile` from event data:
     - `userId` = event.getUserId()
     - `mobileNumber` = event.getMobileNumber()
     - `name` = event.getName()
     - `email` = event.getEmail()
     - `createdAt` = LocalDate.now()
     - `updatedAt` = LocalDate.now()
  4. Saves the UserProfile via repository
- **Returns:** void
- **Idempotency:** Safe to replay — checks existence before creating

---

## UserController

**Package:** `com.hivemind.user.controller`

**Annotations:**
- `@RestController`
- `@RequestMapping("/api/v1/users")`

**Design Pattern:** Façade — exposes simplified API over service layer

### Fields (Constructor Injection)

| Field | Type |
|-------|------|
| userService | IUserService |

### Endpoints

#### `GET /{userId}`
- **Signature:** `public ResponseEntity<UserProfileDto> getUserById(@PathVariable UUID userId)`
- **Logic:** Delegates to `userService.getUserById(userId)`
- **Returns:** `UserProfileDto`

#### `PUT /{userId}`
- **Signature:** `public ResponseEntity<UserProfileDto> updateProfile(@PathVariable UUID userId, @RequestBody UpdateProfileRequest request)`
- **Logic:** Delegates to `userService.updateProfile(userId, request)`
- **Returns:** Updated `UserProfileDto`

#### `POST /{userId}/follow/{targetUserId}`
- **Signature:** `public ResponseEntity<ApiResponse> followUser(@PathVariable UUID userId, @PathVariable UUID targetUserId)`
- **Logic:** Delegates to `userService.followUser(userId, targetUserId)`
- **Returns:** `ApiResponse` with success message

#### `DELETE /{userId}/follow/{targetUserId}`
- **Signature:** `public ResponseEntity<ApiResponse> unfollowUser(@PathVariable UUID userId, @PathVariable UUID targetUserId)`
- **Logic:** Delegates to `userService.unfollowUser(userId, targetUserId)`
- **Returns:** `ApiResponse` with success message

#### `GET /{userId}/followers`
- **Signature:** `public ResponseEntity<List<UserProfileDto>> getFollowers(@PathVariable UUID userId)`
- **Logic:** Delegates to `userService.getFollowers(userId)`
- **Returns:** `List<UserProfileDto>` — all users following the given userId

#### `GET /{userId}/following`
- **Signature:** `public ResponseEntity<List<UserProfileDto>> getFollowing(@PathVariable UUID userId)`
- **Logic:** Delegates to `userService.getFollowing(userId)`
- **Returns:** `List<UserProfileDto>` — all users the given userId is following

---

## UserProfile (Entity)

**Package:** `com.hivemind.user.entity`

**Annotations:**
- `@Table("user_profiles")` — Maps to Cassandra `user_profiles` table

### Fields

| Field | Type | Annotation | Description |
|-------|------|------------|-------------|
| userId | UUID | `@PrimaryKey` | Unique user identifier |
| mobileNumber | String | | E.164 phone number |
| name | String | | Display name |
| email | String | | Email address |
| bio | String | | User biography/description |
| profilePictureUrl | String | | URL to profile picture |
| createdAt | LocalDate | | Profile creation date |
| updatedAt | LocalDate | | Last update timestamp |

---

## Follow (Entity)

**Package:** `com.hivemind.user.entity`

**Annotations:**
- `@Table("follows")` — Maps to Cassandra `follows` table

**Design Pattern:** Composite Key pattern for Cassandra wide-row modeling

### Fields

| Field | Type | Key Type | Description |
|-------|------|----------|-------------|
| followerId | UUID | `PARTITIONED` | The user who is following |
| followingId | UUID | `CLUSTERED` | The user being followed |
| createdAt | LocalDateTime | | When the follow was established |

---

## UserProfileRepository

**Package:** `com.hivemind.user.repository`

**Extends:** `CassandraRepository<UserProfile, UUID>`

**Design Pattern:** Repository pattern

### Methods

#### `findByMobileNumber(String mobileNumber)`
- **Signature:** `@Query(allowFiltering = true) Optional<UserProfile> findByMobileNumber(String mobileNumber)`
- **Logic:** CQL query with ALLOW FILTERING
- **Returns:** `Optional<UserProfile>`

---

## FollowRepository

**Package:** `com.hivemind.user.repository`

**Extends:** `CassandraRepository<Follow, Object>`

**Design Pattern:** Repository pattern

### Methods

#### `findByFollowerId(UUID followerId)`
- **Signature:** `@Query List<Follow> findByFollowerId(UUID followerId)`
- **Returns:** `List<Follow>` — all follow relationships where user is the follower

#### `findByFollowingId(UUID followingId)`
- **Signature:** `@Query(allowFiltering = true) List<Follow> findByFollowingId(UUID followingId)`
- **Logic:** Uses ALLOW FILTERING since followingId is a clustering column
- **Returns:** `List<Follow>` — all follow relationships where user is being followed

#### `findByFollowerIdAndFollowingId(UUID followerId, UUID followingId)`
- **Signature:** `Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId)`
- **Returns:** `Optional<Follow>` — specific follow relationship if exists

---

## IUserService (Interface)

**Package:** `com.hivemind.user.service`

### Method Signatures

| Method | Parameters | Returns |
|--------|-----------|---------|
| `getUserById` | `UUID userId` | `UserProfileDto` |
| `updateProfile` | `UUID userId, UpdateProfileRequest request` | `UserProfileDto` |
| `followUser` | `UUID userId, UUID targetUserId` | `void` |
| `unfollowUser` | `UUID userId, UUID targetUserId` | `void` |
| `getFollowers` | `UUID userId` | `List<UserProfileDto>` |
| `getFollowing` | `UUID userId` | `List<UserProfileDto>` |

---

## UserServiceImpl

**Package:** `com.hivemind.user.service.impl`

**Annotations:**
- `@Service`

**Implements:** `IUserService`

**Design Pattern:** Service Layer — encapsulates business logic for user profile and social graph

### Fields (Constructor Injection)

| Field | Type |
|-------|------|
| userProfileRepository | UserProfileRepository |
| followRepository | FollowRepository |

### Methods

#### `getUserById(UUID userId)`
- **Signature:** `@Override public UserProfileDto getUserById(UUID userId)`
- **Logic:**
  1. Calls `userProfileRepository.findById(userId)`
  2. If not found → throws `RuntimeException` ("User not found")
  3. Maps entity to DTO via `toDto()`
- **Returns:** `UserProfileDto`
- **Exceptions:** RuntimeException if user not found

#### `updateProfile(UUID userId, UpdateProfileRequest request)`
- **Signature:** `@Override public UserProfileDto updateProfile(UUID userId, UpdateProfileRequest request)`
- **Logic:**
  1. Loads existing UserProfile by ID (throws if not found)
  2. Patches non-null fields from request:
     - If `request.getName()` != null → sets name
     - If `request.getEmail()` != null → sets email
     - If `request.getBio()` != null → sets bio
     - If `request.getProfilePictureUrl()` != null → sets profilePictureUrl
  3. Sets `updatedAt` = `LocalDate.now()`
  4. Saves updated entity
  5. Maps to DTO
- **Returns:** `UserProfileDto`
- **Design:** Partial update (PATCH semantics) — only overwrites non-null fields

#### `followUser(UUID userId, UUID targetUserId)`
- **Signature:** `@Override public void followUser(UUID userId, UUID targetUserId)`
- **Logic:**
  1. Checks if follow relationship already exists via `followRepository.findByFollowerIdAndFollowingId(userId, targetUserId)`
  2. If already following → throws exception ("Already following this user")
  3. Creates new `Follow` entity (followerId=userId, followingId=targetUserId, createdAt=now)
  4. Saves via repository
- **Returns:** void
- **Exceptions:** RuntimeException if already following

#### `unfollowUser(UUID userId, UUID targetUserId)`
- **Signature:** `@Override public void unfollowUser(UUID userId, UUID targetUserId)`
- **Logic:**
  1. Finds follow relationship via `followRepository.findByFollowerIdAndFollowingId(userId, targetUserId)`
  2. If not found → throws exception ("Not following this user")
  3. Deletes the follow entity
- **Returns:** void
- **Exceptions:** RuntimeException if not following

#### `getFollowers(UUID userId)`
- **Signature:** `@Override public List<UserProfileDto> getFollowers(UUID userId)`
- **Logic:**
  1. Finds all follow records where `followingId = userId` (people following this user)
  2. For each follow record: loads the follower's UserProfile by `follow.getFollowerId()`
  3. Maps each profile to DTO
- **Returns:** `List<UserProfileDto>`

#### `getFollowing(UUID userId)`
- **Signature:** `@Override public List<UserProfileDto> getFollowing(UUID userId)`
- **Logic:**
  1. Finds all follow records where `followerId = userId` (people this user follows)
  2. For each follow record: loads the followed user's UserProfile by `follow.getFollowingId()`
  3. Maps each profile to DTO
- **Returns:** `List<UserProfileDto>`

#### `toDto(UserProfile profile)` (Private)
- **Signature:** `private UserProfileDto toDto(UserProfile profile)`
- **Logic:** Maps all fields from entity to DTO (userId, mobileNumber, name, email, bio, profilePictureUrl, createdAt)
- **Returns:** `UserProfileDto`

---

## DTOs

**Package:** `com.hivemind.user.dto`

### UpdateProfileRequest

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| name | String | Optional (nullable) | New display name |
| email | String | Optional (nullable) | New email |
| bio | String | Optional (nullable) | New biography |
| profilePictureUrl | String | Optional (nullable) | New avatar URL |

**Note:** All fields are optional — only non-null values are applied (PATCH semantics)

### UserProfileDto

| Field | Type | Description |
|-------|------|-------------|
| userId | UUID | User's unique identifier |
| mobileNumber | String | Phone number |
| name | String | Display name |
| email | String | Email address |
| bio | String | Biography |
| profilePictureUrl | String | Avatar URL |
| createdAt | LocalDate | Profile creation date |

### UserCreatedEvent (Kafka Event — consumed)

| Field | Type | Description |
|-------|------|-------------|
| userId | UUID | New user's ID |
| mobileNumber | String | Phone number |
| name | String | Display name |
| email | String | Email address |

### ApiResponse

| Field | Type | Description |
|-------|------|-------------|
| message | String | Success/error message |
| success | boolean | Operation result |
