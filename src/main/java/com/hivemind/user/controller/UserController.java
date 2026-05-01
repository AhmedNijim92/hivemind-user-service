package com.hivemind.user.controller;

import com.hivemind.common.dto.ApiResponse;
import com.hivemind.user.dto.UpdateProfileRequest;
import com.hivemind.user.dto.UserProfileDto;
import com.hivemind.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController
{
    private final IUserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable UUID userId)
    {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileDto> updateProfile(
            @PathVariable UUID userId,
            @RequestBody UpdateProfileRequest request)
    {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PostMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<ApiResponse> followUser(
            @PathVariable UUID userId,
            @PathVariable UUID targetUserId)
    {
        userService.followUser(userId, targetUserId);
        return ResponseEntity.ok(new ApiResponse("Followed successfully"));
    }

    @DeleteMapping("/{userId}/follow/{targetUserId}")
    public ResponseEntity<ApiResponse> unfollowUser(
            @PathVariable UUID userId,
            @PathVariable UUID targetUserId)
    {
        userService.unfollowUser(userId, targetUserId);
        return ResponseEntity.ok(new ApiResponse("Unfollowed successfully"));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<UserProfileDto>> getFollowers(@PathVariable UUID userId)
    {
        return ResponseEntity.ok(userService.getFollowers(userId));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<List<UserProfileDto>> getFollowing(@PathVariable UUID userId)
    {
        return ResponseEntity.ok(userService.getFollowing(userId));
    }
}
