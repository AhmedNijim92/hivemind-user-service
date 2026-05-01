package com.hivemind.user.consumer;

import com.hivemind.common.event.UserCreatedEvent;
import com.hivemind.user.entity.UserProfile;
import com.hivemind.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer
{
    private final UserProfileRepository userProfileRepository;

    @KafkaListener(topics = "user-created-topic", groupId = "user-service")
    public void handleUserCreated(UserCreatedEvent event)
    {
        log.info("Received UserCreatedEvent for userId: {}", event.getUserId());

        userProfileRepository.findById(event.getUserId()).ifPresentOrElse(
                existing -> log.info("UserProfile already exists for userId: {}", event.getUserId()),
                () -> {
                    UserProfile profile = UserProfile.builder()
                            .userId(event.getUserId())
                            .mobileNumber(event.getMobileNumber())
                            .name(event.getName())
                            .email(event.getEmail())
                            .createdAt(LocalDate.now())
                            .updatedAt(LocalDate.now())
                            .build();
                    userProfileRepository.save(profile);
                    log.info("Created UserProfile for userId: {}", event.getUserId());
                }
        );
    }
}
