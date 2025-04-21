package com.hamhama.controller;

import com.hamhama.dto.UserProfile;
import com.hamhama.model.Recipe; // Assuming you have a Recipe model
import com.hamhama.model.User; // Assuming you have a User model
import com.hamhama.service.UserService;
import io.swagger.v3.oas.annotations.Operation; // For describing endpoints
import io.swagger.v3.oas.annotations.Parameter; // For describing parameters
import io.swagger.v3.oas.annotations.media.Content; // For describing response/request body content
import io.swagger.v3.oas.annotations.media.Schema; // For describing data structures (like DTOs or entities)
import io.swagger.v3.oas.annotations.responses.ApiResponse; // For describing a single response
import io.swagger.v3.oas.annotations.responses.ApiResponses; // For grouping multiple responses
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // To mark endpoints needing authentication
import io.swagger.v3.oas.annotations.tags.Tag; // To group endpoints in Swagger UI
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Keep if used in getCurrentUserFromController
import org.springframework.security.core.context.SecurityContextHolder; // Keep if used in getCurrentUserFromController
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
// Remove unused imports if any after adding annotations

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing users, profiles, and interactions (follow, like, block).") // Groups endpoints in Swagger UI
@SecurityRequirement(name = "bearerAuth") // Apply JWT security requirement to ALL endpoints in this controller
public class UserController {

    private final UserService userService;

    // --- Admin accessible endpoints ---

    @Operation(summary = "Get all users (Admin Only)", description = "Retrieves a list of all registered users. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))), // Schema could be more specific if using UserDTO
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    @GetMapping
    public List<User> getAllUsers() {
        // Note: Consider returning List<UserDTO> instead of the User entity for security and clarity.
        return userService.getAllUsers();
    }

    @Operation(summary = "Get user by ID (Admin Only)", description = "Retrieves details for a specific user by their ID. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class))), // Consider UserDTO
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @Parameter(description = "ID of the user to retrieve") @PathVariable Long id) {
        // Note: Consider returning UserDTO
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new user (Admin Only)", description = "Creates a new user record. Password should be sent raw and will be encoded by the service. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class))), // Consider UserDTO
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid user data (e.g., duplicate username/email, missing fields)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping // Requires ADMIN role (as per SecurityConfig)
    public ResponseEntity<User> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User object for the new user. Ensure password is provided raw.")
            @RequestBody User user) {
        // Note: Consider using a UserCreationDTO.
        try {
            User createdUser = userService.createUser(user);
            // Note: Consider returning UserDTO
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Operation(summary = "Update user details (Admin or Self)", description = "Updates details for a specific user. Admins can update any user. Regular users can only update their own profile (enforced in service).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = User.class))), // Consider UserDTO
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid user data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to update this user"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}") // Requires ADMIN or self (handled in service)
    public ResponseEntity<User> updateUser(
            @Parameter(description = "ID of the user to update") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated user data. **Warning: Should use a DTO to prevent unintended updates (e.g., password, roles)!**")
            @RequestBody User user) { // Use UserUpdateDTO!
        try {
            User updatedUser = userService.updateUser(id, user);
            // Consider returning UserDTO
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            if (e instanceof org.springframework.security.access.AccessDeniedException) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
            } else if (e.getMessage() != null && e.getMessage().contains("not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
            }
        }
    }

    @Operation(summary = "Delete a user (Admin Only)", description = "Deletes a user record by ID. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}") // Requires ADMIN (as per SecurityConfig)
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to delete") @PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    // --- Authenticated user actions ---

    @Operation(summary = "Follow another user", description = "Allows the authenticated user to follow another user specified by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully followed user"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Cannot follow self, already following, user blocked, etc."),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User to follow not found")
    })
    @PostMapping("/follow/{followingId}")
    public ResponseEntity<Void> followUser(
            @Parameter(description = "ID of the user to follow") @PathVariable Long followingId) {
        try {
            userService.followUser(followingId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Operation(summary = "Unfollow a user", description = "Allows the authenticated user to unfollow another user specified by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully unfollowed user"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Cannot unfollow self, not currently following, etc."),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User to unfollow not found")
    })
    @DeleteMapping("/unfollow/{followingId}")
    public ResponseEntity<Void> unfollowUser(
            @Parameter(description = "ID of the user to unfollow") @PathVariable Long followingId) {
        try {
            userService.unfollowUser(followingId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Operation(summary = "Like a recipe", description = "Allows the authenticated user to like a recipe specified by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recipe liked successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Recipe already liked, etc."),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Recipe not found")
    })
    @PostMapping("/like/{recipeId}")
    public ResponseEntity<Void> likeRecipe(
            @Parameter(description = "ID of the recipe to like") @PathVariable Long recipeId) {
        try {
            userService.likeRecipe(recipeId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Operation(summary = "Unlike a recipe", description = "Allows the authenticated user to remove their like from a recipe specified by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recipe unliked successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Recipe not liked previously, etc."),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Recipe not found")
    })
    @DeleteMapping("/unlike/{recipeId}")
    public ResponseEntity<Void> unlikeRecipe(
            @Parameter(description = "ID of the recipe to unlike") @PathVariable Long recipeId) {
        try {
            userService.unlikeRecipe(recipeId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Operation(summary = "Block another user", description = "Allows the authenticated user to block another user, preventing interactions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User blocked successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - Cannot block self, user already blocked, etc."),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User to block not found")
    })
    @PostMapping("/block/{blockedUserId}")
    public ResponseEntity<String> blockUser(
            @Parameter(description = "ID of the user to block") @PathVariable Long blockedUserId) {
        try {
            userService.blockUser(blockedUserId);
            return ResponseEntity.ok("User blocked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Unblock a user", description = "Allows the authenticated user to unblock a previously blocked user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User unblocked successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - User not currently blocked, etc."),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User to unblock not found")
    })
    @DeleteMapping("/unblock/{blockedUserId}")
    public ResponseEntity<String> unblockUser(
            @Parameter(description = "ID of the user to unblock") @PathVariable Long blockedUserId) {
        try {
            userService.unblockUser(blockedUserId);
            return ResponseEntity.ok("User unblocked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // --- Read operations for specific user data ---

    @Operation(summary = "Get following list", description = "Retrieves the list of users that the user specified by ID is following. Requires authentication. Access may be restricted based on the target user's privacy settings or blocking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved following list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))), // Consider List<UserDTO>
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Profile is private or user is blocked"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}/following")
    public ResponseEntity<List<User>> getFollowing(
            @Parameter(description = "ID of the user whose following list is requested") @PathVariable Long id) {
        // Note: Service layer should ideally handle privacy/blocking checks. Documenting potential 403.
        try {
            User user = userService.findUserById(id); // Basic fetch
            // Consider returning List<UserDTO>
            return ResponseEntity.ok(user.getFollowing());
        } catch (RuntimeException e) { // Assuming service throws exception if user not found
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found", e);
            // Or handle AccessDeniedException if service throws it
        }
    }

    @Operation(summary = "Get followers list", description = "Retrieves the list of users following the user specified by ID. Requires authentication. Access may be restricted based on the target user's privacy settings or blocking.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved followers list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))), // Consider List<UserDTO>
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Profile is private or user is blocked"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}/followers")
    public ResponseEntity<List<User>> getFollowers(
            @Parameter(description = "ID of the user whose followers list is requested") @PathVariable Long id) {
        try {
            // Assuming userService.getFollowers includes accessibility checks
            List<User> followers = userService.getFollowers(id);
            // Consider returning List<UserDTO>
            return ResponseEntity.ok(followers);
        } catch (RuntimeException e) {
            if (e instanceof org.springframework.security.access.AccessDeniedException) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or access denied", e);
        }
    }

    @Operation(summary = "Get my liked recipes", description = "Retrieves the list of recipes liked by the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved liked recipes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))), // Consider List<RecipeResponseDTO>
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/liked-recipes")
    public ResponseEntity<List<Recipe>> getMyLikedRecipes() {
        try {
            User user = userService.getCurrentUser();
            // Consider returning List<RecipeResponseDTO>
            return ResponseEntity.ok(user.getLikedRecipes());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated", e);
        }
    }

    @Operation(summary = "Get my profile", description = "Retrieves the profile details of the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved profile",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getMyProfile() {
        try {
            User user = userService.getCurrentUser();
            UserProfile profile = userService.getUserProfile(user.getId());
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated", e);
        }
    }

    @Operation(summary = "Get my blocked users", description = "Retrieves the list of users blocked by the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved blocked users list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))), // Consider List<UserDTO>
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/blocked-users")
    public ResponseEntity<List<User>> getMyBlockedUsers() {
        try {
            User user = userService.getCurrentUser();
            List<User> blockedUsers = userService.getBlockedUsers(user.getId());
            // Consider returning List<UserDTO>
            return ResponseEntity.ok(blockedUsers);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated", e);
        }
    }

    @Operation(summary = "Update my privacy setting", description = "Updates the privacy setting (public/private) for the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Privacy setting updated successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid value provided"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/privacy")
    public ResponseEntity<String> updateMyPrivacySetting(
            @Parameter(description = "'true' to set profile to private, 'false' for public", required = true)
            @RequestParam Boolean isPrivate) {
        try {
            User user = userService.getCurrentUser();
            userService.updatePrivacySetting(user.getId(), isPrivate);
            return ResponseEntity.ok("Privacy setting updated successfully");
        } catch (RuntimeException e) { // Catch Unauthorized from getCurrentUser
            if (e.getMessage() != null && e.getMessage().contains("User not authenticated")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Operation(summary = "Get my privacy setting", description = "Retrieves the current privacy setting (true=private, false=public) for the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved privacy setting",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/privacy")
    public ResponseEntity<Boolean> getMyPrivacySetting() {
        try {
            User user = userService.getCurrentUser();
            Boolean isPrivate = userService.getPrivacySetting(user.getId());
            return ResponseEntity.ok(isPrivate);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("User not authenticated")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
            }
            // Assuming service might throw if setting not found for a valid user (unlikely but possible)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve privacy setting", e);
        }
    }

    @Operation(summary = "Get another user's profile", description = "Retrieves the profile of a user specified by ID. Access is subject to the target user's privacy settings and whether the requesting user (if authenticated) is blocked.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user profile",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Profile is private or the user is blocked"),
            @ApiResponse(responseCode = "404", description = "User profile not found")
            // 401 is implicitly handled by the class-level security requirement if checks require auth
    })
    @GetMapping("/{profileUserId}/profile")
    public ResponseEntity<UserProfile> getPublicUserProfile(
            @Parameter(description = "ID of the user whose profile is being requested") @PathVariable Long profileUserId) {
        Long requestingUserId = null;
        try {
            // Try to get the current user ID. If it fails, requestingUserId remains null (anonymous).
            requestingUserId = userService.getCurrentUser().getId();
        } catch (RuntimeException ex) {
            // Ignore exception here, signifies anonymous user for the accessibility check.
            // Log the exception if necessary for debugging non-auth errors.
            // log.debug("Could not get current user, proceeding as anonymous for profile access check.", ex);
        }

        try {
            // Check accessibility *before* fetching the full profile DTO
            if (!userService.isProfileAccessible(profileUserId, requestingUserId)) {
                // Use FORBIDDEN (403) if access denied due to privacy/blocking
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            UserProfile profile = userService.getUserProfile(profileUserId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            // Catch user not found from getUserProfile or isProfileAccessible if it throws for bad ID
            // Log the specific exception if needed
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found", e);
        }
    }

    @Operation(summary = "Update my profile picture", description = "Updates the profile picture for the currently authenticated user. Requires sending image data as multipart/form-data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile picture updated successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid file type, size limit exceeded, or other processing error."),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateMyProfilePicture(
            @Parameter(description = "The image file to upload (JPEG/PNG recommended)", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file) {
        try {
            User user = userService.getCurrentUser();
            userService.updateProfilePicture(user.getId(), file);
            return ResponseEntity.ok("Profile picture updated successfully");
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("User not authenticated")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    @Operation(summary = "Delete my profile picture", description = "Deletes the profile picture for the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile picture deleted successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - No profile picture to delete or other error."),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/profile-picture")
    public ResponseEntity<String> deleteMyProfilePicture() {
        try {
            User user = userService.getCurrentUser();
            userService.deleteProfilePicture(user.getId());
            return ResponseEntity.ok("Profile picture deleted successfully");
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("User not authenticated")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Example helper if needed directly in controller (kept private)
    private User getCurrentUserFromController() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return (User) authentication.getPrincipal();
    }
}