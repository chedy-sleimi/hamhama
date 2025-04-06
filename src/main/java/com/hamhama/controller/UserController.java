package com.hamhama.controller;

import com.hamhama.dto.UserProfile;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.service.UserService;
import lombok.RequiredArgsConstructor; // Import RequiredArgsConstructor
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// Import HttpMethod if needed for finer grained SecurityConfig (already done there)
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException; // For better error responses
import org.springframework.http.HttpStatus; // Import HttpStatus

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor // Use Lombok for constructor injection
public class UserController {

    private final UserService userService;

    // --- Admin accessible endpoints (Assuming SecurityConfig handles auth) ---

    @GetMapping
    public List<User> getAllUsers() {
        // Consider returning UserDTO instead of User entity
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // Consider returning UserDTO instead of User entity
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping // Requires ADMIN role (as per SecurityConfig)
    public ResponseEntity<User> createUser(@RequestBody User user) {
        // Consider UserDTO here too. Ensure password encoding is handled in service.
        try {
            User createdUser = userService.createUser(user);
            // Consider returning UserDTO
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            // Handle potential exceptions like duplicate username/email if not caught earlier
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping("/{id}") // Requires ADMIN or self (handled in service)
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) { // Use UserUpdateDTO!
        // IMPORTANT: Passing the raw User entity is risky. Use a DTO.
        try {
            User updatedUser = userService.updateUser(id, user);
            // Consider returning UserDTO
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) { // Catch service exceptions (like not found, access denied)
            if (e instanceof org.springframework.security.access.AccessDeniedException) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
            } else if (e.getMessage() != null && e.getMessage().contains("not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
            }
        }
    }

    @DeleteMapping("/{id}") // Requires ADMIN (as per SecurityConfig)
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) { // Catch not found etc.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    // --- Authenticated user actions ---

    // NOTE: The {followerId} in the path often represents the *actor*.
    // Since the service now gets the actor from the context, we might not need it in the path.
    // Option 1: Keep path as is, but ignore {followerId} in the method body.
    // Option 2: Change path to /api/users/follow/{followingId} (simpler). Let's go with Option 2.

    // @PostMapping("/{followerId}/follow") // Old path
    @PostMapping("/follow/{followingId}") // New simpler path
    public ResponseEntity<Void> followUser(@PathVariable Long followingId) {
        try {
            // userService.followUser(followerId, followingId); // Old call
            userService.followUser(followingId); // CORRECTED CALL
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // @DeleteMapping("/{followerId}/unfollow") // Old path
    @DeleteMapping("/unfollow/{followingId}") // New simpler path
    public ResponseEntity<Void> unfollowUser(@PathVariable Long followingId) {
        try {
            // userService.unfollowUser(followerId, followingId); // Old call
            userService.unfollowUser(followingId); // CORRECTED CALL
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // Like/Unlike actions also belong to the logged-in user acting on a recipe
    // The {userId} path variable is redundant if the service uses the authenticated principal.
    // Let's adjust the paths similarly.

    // @PostMapping("/{userId}/like/{recipeId}") // Old path
    @PostMapping("/like/{recipeId}") // New simpler path
    public ResponseEntity<Void> likeRecipe(@PathVariable Long recipeId) {
        try {
            // userService.likeRecipe(userId, recipeId); // Old call
            userService.likeRecipe(recipeId); // CORRECTED CALL
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // @DeleteMapping("/{userId}/unlike/{recipeId}") // Old path
    @DeleteMapping("/unlike/{recipeId}") // New simpler path
    public ResponseEntity<Void> unlikeRecipe(@PathVariable Long recipeId) {
        try {
            // userService.unlikeRecipe(userId, recipeId); // Old call
            userService.unlikeRecipe(recipeId); // CORRECTED CALL
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // Blocking/Unblocking: Logged-in user blocks another user.
    // Path should reflect this: /api/users/block/{blockedUserId}

    // @PostMapping("/{userId}/block/{blockedUserId}") // Old path
    @PostMapping("/block/{blockedUserId}") // New simpler path
    public ResponseEntity<String> blockUser(@PathVariable Long blockedUserId) {
        try {
            // userService.blockUser(userId, blockedUserId); // Old call
            userService.blockUser(blockedUserId); // CORRECTED CALL
            return ResponseEntity.ok("User blocked successfully");
        } catch (RuntimeException e) {
            // Return specific status codes based on error if needed
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // @DeleteMapping("/{userId}/unblock/{blockedUserId}") // Old path
    @DeleteMapping("/unblock/{blockedUserId}") // New simpler path
    public ResponseEntity<String> unblockUser(@PathVariable Long blockedUserId) {
        try {
            // userService.unblockUser(userId, blockedUserId); // Old call
            userService.unblockUser(blockedUserId); // CORRECTED CALL
            return ResponseEntity.ok("User unblocked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // --- Read operations for specific user data ---
    // These often need context about *who* is requesting.

    // Get list of users the given user *is* following (requires auth, maybe privacy check)
    @GetMapping("/{id}/following")
    public ResponseEntity<List<User>> getFollowing(@PathVariable Long id) {
        // Add privacy checks if necessary - can the requester see this?
        // For simplicity, assume authenticated users can see following list for accessible profiles.
        try {
            // Need a service method that potentially checks accessibility first
            User user = userService.findUserById(id); // Basic fetch
            // Consider returning List<UserDTO>
            return ResponseEntity.ok(user.getFollowing());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    // Get list of followers of a user (requires auth, maybe privacy check)
    @GetMapping("/{id}/followers")
    public ResponseEntity<List<User>> getFollowers(@PathVariable Long id) {
        try {
            // Service method already includes accessibility check
            List<User> followers = userService.getFollowers(id);
            // Consider returning List<UserDTO>
            return ResponseEntity.ok(followers);
        } catch (RuntimeException e) { // Catch not found or access denied from service
            if (e instanceof org.springframework.security.access.AccessDeniedException) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    // Get liked recipes for the logged-in user (or maybe any user if profile public?)
    // Let's assume it's for the currently logged-in user for simplicity. Path: /api/users/liked-recipes
    // @GetMapping("/{id}/liked-recipes") // Old path assumes ID passed in
    @GetMapping("/liked-recipes") // New path: Get *my* liked recipes
    public ResponseEntity<List<Recipe>> getMyLikedRecipes() { // Changed method name
        try {
            User user = userService.getCurrentUser(); // Get current user from service helper
            // Consider returning List<RecipeResponseDTO>
            return ResponseEntity.ok(user.getLikedRecipes());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e); // If not authenticated
        }
    }

    // Get *own* profile details. Path: /api/users/profile
    // @GetMapping("/{id}/profile") // Old: Get profile by ID
    @GetMapping("/profile") // New: Get *my* profile
    public ResponseEntity<UserProfile> getMyProfile() { // Changed method name
        try {
            User user = userService.getCurrentUser();
            UserProfile profile = userService.getUserProfile(user.getId()); // Call service with own ID
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        }
    }

    // Get blocked users list for the *logged-in user*. Path: /api/users/blocked-users
    // @GetMapping("/{userId}/blocked-users") // Old path
    @GetMapping("/blocked-users") // New path
    public ResponseEntity<List<User>> getMyBlockedUsers() { // Changed method name
        try {
            User user = userService.getCurrentUser();
            List<User> blockedUsers = userService.getBlockedUsers(user.getId()); // Call service with own ID
            // Consider returning List<UserDTO>
            return ResponseEntity.ok(blockedUsers);
        } catch (RuntimeException e) { // Catch AccessDenied if service checks fail (shouldn't for self)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        }
    }

    // Update *own* privacy setting. Path: /api/users/privacy
    // @PutMapping("/{userId}/privacy") // Old path
    @PutMapping("/privacy") // New path
    public ResponseEntity<String> updateMyPrivacySetting(@RequestParam Boolean isPrivate) { // Changed method name
        try {
            User user = userService.getCurrentUser();
            userService.updatePrivacySetting(user.getId(), isPrivate); // Call service with own ID
            return ResponseEntity.ok("Privacy setting updated successfully");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // Get *own* privacy setting. Path: /api/users/privacy
    // @GetMapping("/{userId}/privacy") // Old path
    @GetMapping("/privacy") // New path
    public ResponseEntity<Boolean> getMyPrivacySetting() { // Changed method name
        try {
            User user = userService.getCurrentUser();
            Boolean isPrivate = userService.getPrivacySetting(user.getId()); // Call service with own ID
            return ResponseEntity.ok(isPrivate);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e); // Or UNAUTHORIZED
        }
    }

    // Get *another* user's profile (respects privacy). Path: /api/users/{profileUserId}/profile
    // @GetMapping("/{profileUserId}/profile/{requestingUserId}") // Old path included requester
    @GetMapping("/{profileUserId}/profile") // New path, requester comes from context
    public ResponseEntity<UserProfile> getPublicUserProfile(@PathVariable Long profileUserId) { // Renamed method
        try {
            User requestingUser = userService.getCurrentUser(); // Get optional current user
            Long requestingUserId = (requestingUser != null) ? requestingUser.getId() : null;

            // Check accessibility *before* fetching the full profile DTO
            if (!userService.isProfileAccessible(profileUserId, requestingUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profile is private or user is blocked.");
            }

            UserProfile profile = userService.getUserProfile(profileUserId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            // Catch specific exceptions if needed (e.g., user not found)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found or inaccessible.", e);
        }
    }

    // Update *own* profile picture. Path: /api/users/profile-picture
    // @PutMapping(value = "/{id}/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // Old Path
    @PutMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // New Path
    public ResponseEntity<String> updateMyProfilePicture(@RequestParam("file") MultipartFile file) { // Changed method name
        try {
            User user = userService.getCurrentUser();
            userService.updateProfilePicture(user.getId(), file); // Call service with own ID
            return ResponseEntity.ok("Profile picture updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Delete *own* profile picture. Path: /api/users/profile-picture
    // @DeleteMapping("/{id}/profile-picture") // Old path
    @DeleteMapping("/profile-picture") // New path
    public ResponseEntity<String> deleteMyProfilePicture() { // Changed method name
        try {
            User user = userService.getCurrentUser();
            userService.deleteProfilePicture(user.getId()); // Call service with own ID
            return ResponseEntity.ok("Profile picture deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // We need getCurrentUserOptional() in UserService for the public profile check
    // Add this helper to UserService:
    /*
    @Transactional(readOnly = true)
    public Optional<User> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
             return Optional.of((User) principal);
        } else if (principal instanceof String) {
            // If principal is just the username string after initial load
            return userRepository.findByUsername((String) principal);
        }
        return Optional.empty();
    }
    */
    // Also need getCurrentUser() in UserController if not using the one from service
    private User getCurrentUserFromController() { // Example if needed directly in controller
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return (User) authentication.getPrincipal();
    }
}