package com.hamhama.controller;

import com.hamhama.dto.UserProfile;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public Optional<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
    // Add the follow endpoint
    @PostMapping("/{followerId}/follow")
    public void followUser(@PathVariable Long followerId, @RequestParam Long followingId) {
        userService.followUser(followerId, followingId); // Call the follow function
    }

    // Add the unfollow endpoint
    @DeleteMapping("/{followerId}/unfollow")
    public void unfollowUser(@PathVariable Long followerId, @RequestParam Long followingId) {
        userService.unfollowUser(followerId, followingId); // Call the unfollow function
    }
    // Get list of users the given user is following
    @GetMapping("/{id}/following")
    public List<User> getFollowing(@PathVariable Long id) {
        return userService.getUserById(id).map(User::getFollowing).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Get list of followers of a user
    @GetMapping("/{id}/followers")
    public List<User> getFollowers(@PathVariable Long id) {
        return userService.getFollowers(id);
    }
    @PostMapping("/{userId}/like/{recipeId}")
    public void likeRecipe(@PathVariable Long userId, @PathVariable Long recipeId) {
        userService.likeRecipe(userId, recipeId); // Call the service method to like the recipe
    }
    @DeleteMapping("/{userId}/unlike/{recipeId}")
    public void unlikeRecipe(@PathVariable Long userId, @PathVariable Long recipeId) {
        userService.unlikeRecipe(userId, recipeId); // Call the service method to unlike the recipe
    }
    @GetMapping("/{id}/liked-recipes")
    public List<Recipe> getUserLikedRecipes(@PathVariable Long id) {
        User user = userService.findUserById(id); // Find the user by ID
        return user.getLikedRecipes(); // Return the list of recipes liked by the user
    }
    @GetMapping("/{id}/profile")
    public UserProfile getUserProfile(@PathVariable Long id) {
        return userService.getUserProfile(id); // Get the profile details from the service
    }

    // Block a user
    @PostMapping("/{userId}/block/{blockedUserId}")
    public ResponseEntity<String> blockUser(
            @PathVariable Long userId,
            @PathVariable Long blockedUserId) {
        try {
            userService.blockUser(userId, blockedUserId); // Call the service method
            return ResponseEntity.ok("User blocked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage()); // Handle errors
        }
    }

    // Unblock a user
    @DeleteMapping("/{userId}/unblock/{blockedUserId}")
    public ResponseEntity<String> unblockUser(
            @PathVariable Long userId,
            @PathVariable Long blockedUserId) {
        try {
            userService.unblockUser(userId, blockedUserId); // Call the service method
            return ResponseEntity.ok("User unblocked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage()); // Handle errors
        }
    }

    // Get blocked users
    @GetMapping("/{userId}/blocked-users")
    public ResponseEntity<List<User>> getBlockedUsers(@PathVariable Long userId) {
        try {
            List<User> blockedUsers = userService.getBlockedUsers(userId); // Call the service method
            return ResponseEntity.ok(blockedUsers); // Return the list of blocked users
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null); // Handle errors
        }
    }
    // Update privacy setting
    @PutMapping("/{userId}/privacy")
    public ResponseEntity<String> updatePrivacySetting(
            @PathVariable Long userId,
            @RequestParam Boolean isPrivate) {
        try {
            userService.updatePrivacySetting(userId, isPrivate); // Call the service method
            return ResponseEntity.ok("Privacy setting updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage()); // Handle errors
        }
    }

    // Fetch privacy setting
    @GetMapping("/{userId}/privacy")
    public ResponseEntity<Boolean> getPrivacySetting(@PathVariable Long userId) {
        try {
            Boolean isPrivate = userService.getPrivacySetting(userId); // Call the service method
            return ResponseEntity.ok(isPrivate); // Return the privacy setting
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null); // Handle errors
        }
    }

    // Fetch profile (with privacy checks)
    @GetMapping("/{profileUserId}/profile/{requestingUserId}")
    public ResponseEntity<UserProfile> getProfile(
            @PathVariable Long profileUserId,
            @PathVariable Long requestingUserId) {
        try {
            // Check if the profile is accessible to the requesting user
            if (!userService.isProfileAccessible(profileUserId, requestingUserId)) {
                return ResponseEntity.status(403).body(null); // Return 403 Forbidden if access is denied
            }

            UserProfile profile = userService.getUserProfile(profileUserId); // Fetch the profile
            return ResponseEntity.ok(profile); // Return the profile
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null); // Handle errors
        }
    }

    @PutMapping(value = "/{id}/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            userService.updateProfilePicture(id, file);
            return ResponseEntity.ok("Profile picture updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/profile-picture")
    public ResponseEntity<String> deleteProfilePicture(@PathVariable Long id) {
        try {
            userService.deleteProfilePicture(id); // Call the service method to delete the image
            return ResponseEntity.ok("Profile picture deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage()); // Handle errors
        }
    }
}