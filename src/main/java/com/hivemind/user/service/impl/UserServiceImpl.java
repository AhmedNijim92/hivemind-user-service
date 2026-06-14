package com.hivemind.user.service.impl;

import com.hivemind.user.dto.UpdateProfileRequest;
import com.hivemind.user.dto.UserProfileDto;
import com.hivemind.user.entity.Follow;
import com.hivemind.user.entity.UserProfile;
import com.hivemind.user.repository.FollowRepository;
import com.hivemind.user.repository.UserProfileRepository;
import com.hivemind.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService
{
    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;

    @Override
    public UserProfileDto getUserById(UUID userId)
    {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return toDto(profile);
    }

    @Override
    public UserProfileDto updateProfile(UUID userId, UpdateProfileRequest request)
    {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (request.getName() != null) profile.setName(request.getName());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getProfilePictureUrl() != null) profile.setProfilePictureUrl(request.getProfilePictureUrl());
        if (request.getCoverPictureUrl() != null) profile.setCoverPictureUrl(request.getCoverPictureUrl());
        if (request.getShowContactInfo() != null) profile.setShowContactInfo(request.getShowContactInfo());
        profile.setUpdatedAt(LocalDate.now());

        userProfileRepository.save(profile);
        return toDto(profile);
    }

    @Override
    public void followUser(UUID followerId, UUID targetUserId)
    {
        followRepository.findByFollowerIdAndFollowingId(followerId, targetUserId)
                .ifPresent(f -> { throw new RuntimeException("Already following this user"); });

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(targetUserId)
                .createdAt(LocalDateTime.now())
                .build();
        followRepository.save(follow);
        log.info("User {} followed user {}", followerId, targetUserId);
    }

    @Override
    public void unfollowUser(UUID followerId, UUID targetUserId)
    {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Not following this user"));
        followRepository.delete(follow);
        log.info("User {} unfollowed user {}", followerId, targetUserId);
    }

    @Override
    public List<UserProfileDto> getFollowers(UUID userId)
    {
        return followRepository.findByFollowingId(userId).stream()
                .map(f -> userProfileRepository.findById(f.getFollowerId()).map(this::toDto).orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserProfileDto> getFollowing(UUID userId)
    {
        return followRepository.findByFollowerId(userId).stream()
                .map(f -> userProfileRepository.findById(f.getFollowingId()).map(this::toDto).orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    private UserProfileDto toDto(UserProfile profile)
    {
        return UserProfileDto.builder()
                .userId(profile.getUserId())
                .mobileNumber(profile.getMobileNumber())
                .name(profile.getName())
                .email(profile.getEmail())
                .bio(profile.getBio())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .coverPictureUrl(profile.getCoverPictureUrl())
                .showContactInfo(profile.getShowContactInfo() != null ? profile.getShowContactInfo() : false)
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
