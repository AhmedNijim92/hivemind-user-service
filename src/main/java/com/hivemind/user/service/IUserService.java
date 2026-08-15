package com.hivemind.user.service;

import com.hivemind.user.dto.UpdateProfileRequest;
import com.hivemind.user.dto.UserProfileDto;

import java.util.List;
import java.util.UUID;

public interface IUserService
{
    UserProfileDto getUserById(UUID userId);

    UserProfileDto updateProfile(UUID userId, UpdateProfileRequest request);

    void followUser(UUID followerId, UUID targetUserId);

    void unfollowUser(UUID followerId, UUID targetUserId);

    List<UserProfileDto> getFollowers(UUID userId);

    List<UserProfileDto> getFollowing(UUID userId);

    List<UserProfileDto> searchUsers(String query);
}
