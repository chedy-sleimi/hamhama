package com.hamhama.service;

import com.hamhama.dto.UserProfile;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.repository.RecipeRepository;
import com.hamhama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize; // Import
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder; // Import for password updates
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    // Inject PasswordEncoder if allowing password updates via updateUser
    private final PasswordEncoder passwordEncoder;

    // --- Admin Operations (Access controlled primarily by SecurityConfig) ---

    @Transactional(readOnly = true)
    // @PreAuthorize("hasRole('ADMIN')") // Redundant if SecurityConfig covers GET /api/users
    public List<User> getAllUsers() {
        log.debug("Admin retrieving all users.");
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    // @PreAuthorize("hasRole('ADMIN')") // Redundant if SecurityConfig covers GET /api/users/{id}
    public Optional<User> getUserById(Long id) {
        // This is the admin view - fetches the raw User entity
        log.debug("Admin retrieving user by ID: {}", id);
        return userRepository.findById(id);
    }

    // @PreAuthorize("hasRole('ADMIN')") // Redundant if SecurityConfig covers POST /api/users
    public User createUser(User user) {
        // Admin creating a user - ensure password encoding if provided directly
        log.info("Admin creating user: {}", user.getUsername());
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) { // Basic check if not encoded
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        // TODO: Assign roles as needed for admin creation
        return userRepository.save(user);
    }

    // @PreAuthorize("hasRole('ADMIN')") // Redundant if SecurityConfig covers DELETE /api/users/{id}
    public void deleteUser(Long id) {
        log.warn("Admin deleting user ID: {}", id); // Log as warning due to destructive nature
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found for deletion with ID: " + id);
        }
        userRepository.deleteById(id);
        log.info("Admin successfully deleted user ID: {}", id);
    }


    // --- Authenticated User Operations (Self or Admin) ---

    /**
     * Updates user details. Allows self-update of certain fields (e.g., email)
     * or admin update of more fields. Password update should have a dedicated flow.
     *
     * @param id          ID of the user to update.
     * @param updatedUser Object containing updated details (use a DTO!).
     * @return The updated User.
     */
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public User updateUser(Long id, User updatedUser) { // VERY IMPORTANT: Use a DTO here
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User '{}' attempting to update user ID: {}", currentUser.getUsername(), id);

        // --- Field-level authorization ---
        // Allow self-update of email (example)
        if (Objects.equals(currentUser.getId(), id)) {
            if (updatedUser.getEmail() != null) {
                log.debug("User {} updating own email to {}", id, updatedUser.getEmail());
                userToUpdate.setEmail(updatedUser.getEmail());
            }
            // Add other self-updatable fields here (e.g., maybe username if allowed)
            if (updatedUser.getUsername() != null && !updatedUser.getUsername().equals(userToUpdate.getUsername()) ) {
                // Check for username uniqueness if allowing change
                if (userRepository.existsByUsername(updatedUser.getUsername())) {
                    throw new RuntimeException("Username " + updatedUser.getUsername() + " is already taken.");
                }
                log.debug("User {} updating own username to {}", id, updatedUser.getUsername());
                userToUpdate.setUsername(updatedUser.getUsername());
            }

            // **DO NOT** allow self-update of password or roles here. Use dedicated methods.

        }

        // Allow Admin update of more fields (e.g., username, email, potentially roles - be careful!)
        if (isAdmin) {
            log.debug("Admin updating user ID: {}", id);
            if (updatedUser.getUsername() != null) {
                // Check uniqueness if changing username
                if (!updatedUser.getUsername().equals(userToUpdate.getUsername()) && userRepository.existsByUsername(updatedUser.getUsername())) {
                    throw new RuntimeException("Username " + updatedUser.getUsername() + " is already taken.");
                }
                userToUpdate.setUsername(updatedUser.getUsername());
            }
            if (updatedUser.getEmail() != null) {
                // Check uniqueness if changing email
                if (!updatedUser.getEmail().equals(userToUpdate.getEmail()) && userRepository.existsByEmail(updatedUser.getEmail())) {
                    throw new RuntimeException("Email " + updatedUser.getEmail() + " is already in use.");
                }
                userToUpdate.setEmail(updatedUser.getEmail());
            }
            // Example: Admin changing roles (handle with care!)
            // if (updatedUser.getRoles() != null) {
            //    log.warn("Admin changing roles for user ID: {}", id);
            //    userToUpdate.setRoles(updatedUser.getRoles());
            // }

            // **DO NOT** allow admin update of password here without specific intent/logging.
        }

        // Handle password update: ONLY if explicitly intended and `updatedUser.getPassword()` is not null.
        // This should ideally be a separate endpoint/method like `changePassword`.
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            if (Objects.equals(currentUser.getId(), id)) {
                log.warn("User {} attempting self-password update via updateUser method. Redirect to changePassword.", id);
                throw new AccessDeniedException("Password updates should use a dedicated endpoint.");
            } else if (isAdmin) {
                log.warn("Admin updating password for user ID: {} via updateUser method.", id);
                userToUpdate.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
        }


        User savedUser = userRepository.save(userToUpdate);
        log.info("User ID {} updated successfully.", id);
        return savedUser;
    }

    // --- User Interaction Methods ---

    public void followUser(Long followingId) {
        User follower = getCurrentUser(); // Action performer is the logged-in user
        User following = findUserById(followingId);

        if (follower.getId().equals(followingId)) {
            throw new RuntimeException("You cannot follow yourself.");
        }
        // Optional: Check if already blocked by the target user?

        if (!follower.getFollowing().contains(following)) {
            follower.getFollowing().add(following);
            userRepository.save(follower);
            log.info("User '{}' started following user '{}'", follower.getUsername(), following.getUsername());
        } else {
            log.debug("User '{}' already follows user '{}'", follower.getUsername(), following.getUsername());
        }
    }

    public void unfollowUser(Long followingId) {
        User follower = getCurrentUser(); // Action performer
        User following = findUserById(followingId);

        if (follower.getFollowing().contains(following)) {
            follower.getFollowing().remove(following);
            userRepository.save(follower);
            log.info("User '{}' unfollowed user '{}'", follower.getUsername(), following.getUsername());
        } else {
            log.debug("User '{}' was not following user '{}'", follower.getUsername(), following.getUsername());
        }
    }

    public void likeRecipe(Long recipeId) {
        User user = getCurrentUser(); // Action performer
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (!user.getLikedRecipes().contains(recipe)) {
            user.getLikedRecipes().add(recipe);
            userRepository.save(user);
            log.info("User '{}' liked recipe ID {}", user.getUsername(), recipeId);
        } else {
            log.debug("User '{}' already liked recipe ID {}", user.getUsername(), recipeId);
        }
    }

    public void unlikeRecipe(Long recipeId) {
        User user = getCurrentUser(); // Action performer
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (user.getLikedRecipes().contains(recipe)) {
            user.getLikedRecipes().remove(recipe);
            userRepository.save(user);
            log.info("User '{}' unliked recipe ID {}", user.getUsername(), recipeId);
        } else {
            log.debug("User '{}' had not liked recipe ID {}", user.getUsername(), recipeId);
        }
    }

    public void blockUser(Long blockedUserId) {
        User user = getCurrentUser(); // Action performer
        User blockedUser = findUserById(blockedUserId);

        if (user.getId().equals(blockedUserId)) {
            throw new RuntimeException("You cannot block yourself");
        }

        if (!user.getBlockedUsers().contains(blockedUser)) {
            user.getBlockedUsers().add(blockedUser);
            // Also remove follow relationships upon blocking
            user.getFollowing().remove(blockedUser);
            blockedUser.getFollowing().remove(user);
            userRepository.save(user);
            userRepository.save(blockedUser); // Save both sides of follow removal
            log.info("User '{}' blocked user '{}'", user.getUsername(), blockedUser.getUsername());
        } else {
            log.debug("User '{}' already blocked user '{}'", user.getUsername(), blockedUser.getUsername());
            throw new RuntimeException("User is already blocked"); // Maintain original behavior
        }
    }

    public void unblockUser(Long blockedUserId) {
        User user = getCurrentUser(); // Action performer
        User blockedUser = findUserById(blockedUserId);

        if (user.getBlockedUsers().contains(blockedUser)) {
            user.getBlockedUsers().remove(blockedUser);
            userRepository.save(user);
            log.info("User '{}' unblocked user '{}'", user.getUsername(), blockedUser.getUsername());
        } else {
            log.debug("User '{}' had not blocked user '{}'", user.getUsername(), blockedUser.getUsername());
            throw new RuntimeException("User is not blocked"); // Maintain original behavior
        }
    }

    // --- Read Operations with Potential Privacy/Auth Checks ---

    @Transactional(readOnly = true)
    public List<User> getFollowers(Long userId) {
        // Check if profile is accessible before returning followers
        User profileUser = findUserById(userId);
        User requestingUser = getCurrentUser(); // May be null if anonymous access needs checking
        if (!isProfileAccessible(userId, requestingUser != null ? requestingUser.getId() : null)) {
            log.warn("User '{}' denied access to followers of user ID {}",
                    requestingUser != null ? requestingUser.getUsername() : "anonymous", userId);
            throw new AccessDeniedException("You do not have permission to view this user's followers.");
        }
        log.debug("Fetching followers for accessible profile ID: {}", userId);
        return profileUser.getFollowers(); // Assuming relation is mapped correctly
    }

    // Consider similar accessibility check for getFollowing if needed

    @Transactional(readOnly = true)
    public UserProfile getUserProfile(Long userId) {
        // Profile accessibility is typically checked in the *controller* before calling this,
        // or via isProfileAccessible. Let's assume the check happens before this call.
        User user = findUserById(userId);

        // If called directly, we might add a check here, but better at entry point
        // User requestingUser = getCurrentUser();
        // if (!isProfileAccessible(userId, requestingUser != null ? requestingUser.getId() : null)) {
        //     throw new AccessDeniedException("Profile is private.");
        // }

        int followersCount = user.getFollowers() != null ? user.getFollowers().size() : 0;
        int followingCount = user.getFollowing() != null ? user.getFollowing().size() : 0;
        List<Recipe> likedRecipes = user.getLikedRecipes(); // Might be large - consider pagination or limited view
        Boolean isPrivate = user.getIsPrivate();
        String profilePictureUrl = "/profile-pictures/" + user.getId() + ".jpg"; // Adjust path as needed

        log.debug("Generating profile DTO for user ID: {}", userId);
        return new UserProfile(
                user.getUsername(), user.getEmail(),
                followersCount, followingCount, likedRecipes,
                profilePictureUrl, isPrivate
        );
    }


    @Transactional(readOnly = true)
    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public List<User> getBlockedUsers(Long userId) {
        // @PreAuthorize ensures only self or admin can view
        log.debug("Fetching blocked users for user ID: {}", userId);
        // The custom query userRepository.findBlockedUsers(userId) is fine here
        return userRepository.findBlockedUsers(userId);
        // Or if using the entity relationship directly:
        // User user = findUserById(userId);
        // return user.getBlockedUsers();
    }


    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public void updatePrivacySetting(Long userId, Boolean isPrivate) {
        // @PreAuthorize ensures only self or admin can update
        User user = findUserById(userId);
        user.setIsPrivate(isPrivate);
        userRepository.save(user);
        log.info("Privacy setting updated for user ID {} to {} by user '{}' or ADMIN.", userId, isPrivate, getCurrentUsername());
    }


    @Transactional(readOnly = true)
    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public Boolean getPrivacySetting(Long userId) {
        // @PreAuthorize ensures only self or admin can view directly
        User user = findUserById(userId);
        log.debug("Fetching privacy setting for user ID: {}", userId);
        return user.getIsPrivate();
    }

    @Transactional(readOnly = true)
    public boolean isProfileAccessible(Long profileUserId, Long requestingUserId) {
        // Handle case where requesting user is anonymous (not logged in)
        boolean isAnonymous = requestingUserId == null;

        User profileUser = findUserById(profileUserId);

        // Public profile? Accessible to everyone.
        if (profileUser.getIsPrivate() == null || !profileUser.getIsPrivate()) {
            log.trace("Profile {} is public, accessible.", profileUserId);
            return true;
        }

        // Private profile, but requester is anonymous? Not accessible.
        if (isAnonymous) {
            log.trace("Profile {} is private, anonymous access denied.", profileUserId);
            return false;
        }

        // Private profile, check if requester is the owner
        if (Objects.equals(profileUserId, requestingUserId)) {
            log.trace("Profile {} is private, owner access granted.", profileUserId);
            return true;
        }

        // Private profile, check if requester is a follower
        // Need to fetch the requesting user entity to check follower list
        User requestingUser = findUserById(requestingUserId);
        // Check if profileUser's list of followers contains requestingUser
        boolean isFollower = profileUser.getFollowers() != null && profileUser.getFollowers().contains(requestingUser);
        log.trace("Profile {} is private, checking follower status for user {}: {}", profileUserId, requestingUserId, isFollower);
        return isFollower;
    }

    @PreAuthorize("#userId == principal.id or hasRole('ADMIN')")
    public void updateProfilePicture(Long userId, MultipartFile file) {
        // @PreAuthorize handles auth check
        User user = findUserById(userId); // Verify user exists
        log.info("Updating profile picture for user ID: {}", userId);
        try {
            if (file.isEmpty()) throw new RuntimeException("File is empty");
            if (file.getSize() > 5 * 1024 * 1024) throw new RuntimeException("File size exceeds 5MB");
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/jpeg")) throw new RuntimeException("Only JPG images are allowed");

            String uploadDir = "uploads/profile-pictures/"; // Consider making this configurable
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) Files.createDirectories(dirPath);

            String fileName = user.getId() + ".jpg"; // Use user ID for consistency
            Path filePath = dirPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, REPLACE_EXISTING);
            log.info("Profile picture saved to {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save profile picture for user ID {}", userId, e);
            throw new RuntimeException("Failed to save profile picture", e);
        }
    }


    @PreAuthorize("#id == principal.id or hasRole('ADMIN')")
    public void deleteProfilePicture(Long id) {
        // @PreAuthorize handles auth check
        log.warn("Deleting profile picture for user ID: {}", id); // Log as warn due to deletion
        // Verify user exists first
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with ID: " + id);
        }
        Path imagePath = Paths.get("uploads/profile-pictures/" + id + ".jpg"); // Consistent path
        try {
            boolean deleted = Files.deleteIfExists(imagePath);
            if (deleted) {
                log.info("Profile picture deleted for user ID: {}", id);
            } else {
                log.warn("Profile picture file not found for deletion for user ID: {}", id);
            }
        } catch (IOException e) {
            log.error("Failed to delete profile picture for user ID {}", id, e);
            throw new RuntimeException("Failed to delete profile picture", e);
        }
    }


    // --- Helper Methods ---
    // Find user or throw specific exception
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id)); // Consider specific exception
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            // Depending on context, returning null or throwing might be appropriate
            // For methods requiring auth, throwing is safer.
            throw new AccessDeniedException("User is not authenticated.");
        }
        return (User) authentication.getPrincipal();
    }
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}