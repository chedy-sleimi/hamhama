package com.hamhama.service;

import com.hamhama.dto.UserProfile;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.repository.RecipeRepository;
import com.hamhama.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService  {
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;


    public UserService(UserRepository userRepository,RecipeRepository recipeRepository) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updatedUser.getUsername());
            user.setPassword(updatedUser.getPassword());
            user.setEmail(updatedUser.getEmail());
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    // Add the followUser method
    public void followUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId).orElseThrow(() -> new RuntimeException("Follower not found"));
        User following = userRepository.findById(followingId).orElseThrow(() -> new RuntimeException("Following user not found"));

        // Check if already following, if not, add to the following list
        if (!follower.getFollowing().contains(following)) {
            follower.getFollowing().add(following);
            userRepository.save(follower); // Save the updated follower
        }
    }

    // Add the unfollowUser method
    public void unfollowUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId).orElseThrow(() -> new RuntimeException("Follower not found"));
        User following = userRepository.findById(followingId).orElseThrow(() -> new RuntimeException("Following user not found"));

        // Check if following, if so, remove from the following list
        if (follower.getFollowing().contains(following)) {
            follower.getFollowing().remove(following);
            userRepository.save(follower); // Save the updated follower
        }
    }
    // Get followers of a user
    public List<User> getFollowers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Return the followers list from the User entity
        return user.getFollowers();
    }
    public void likeRecipe(Long userId, Long recipeId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (!user.getLikedRecipes().contains(recipe)) {
            user.getLikedRecipes().add(recipe);  // Add the recipe to the user's liked recipes list
            userRepository.save(user);           // Save the updated user
        }
    }
    public void unlikeRecipe(Long userId, Long recipeId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (user.getLikedRecipes().contains(recipe)) {
            user.getLikedRecipes().remove(recipe);  // Remove the recipe from the user's liked recipes list
            userRepository.save(user);               // Save the updated user
        }
    }
    public User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }
    public UserProfile getUserProfile(Long userId) {
        User user = findUserById(userId); // Get the user by ID

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Retrieve followers count and following count
        int followersCount = user.getFollowers().size();
        int followingCount = user.getFollowing().size();

        // Retrieve liked recipes
        List<Recipe> likedRecipes = user.getLikedRecipes();

        // Retrieve profile picture URL
        String profilePictureUrl = user.getProfilePictureUrl();

        // Retrieve privacy setting
        Boolean isPrivate = user.getIsPrivate();

        // Create and return the UserProfile
        return new UserProfile(
                user.getUsername(),
                user.getEmail(),
                followersCount,
                followingCount,
                likedRecipes,
                profilePictureUrl,
                isPrivate // Add the privacy setting
        );
    }

    // Add a method to update the profile picture URL
    public void updateProfilePicture(Long userId, String profilePictureUrl) {
        User user = findUserById(userId); // Find the user by ID
        user.setProfilePictureUrl(profilePictureUrl); // Update the profile picture URL
        userRepository.save(user); // Save the updated user
    }
    // Block a user
    public void blockUser(Long userId, Long blockedUserId) {
        if (userId.equals(blockedUserId)) {
            throw new RuntimeException("You cannot block yourself"); // Prevent self-blocking
        }

        User user = findUserById(userId); // Find the user who is blocking
        User blockedUser = findUserById(blockedUserId); // Find the user to be blocked

        if (user.getBlockedUsers().contains(blockedUser)) { // New validation
            throw new RuntimeException("User is already blocked"); // Prevent blocking someone who is already blocked
        }

        user.getBlockedUsers().add(blockedUser); // Add to the blocked list
        userRepository.save(user); // Save the updated user
    }
    // Unblock a user
    public void unblockUser(Long userId, Long blockedUserId) {
        User user = findUserById(userId); // Find the user who is unblocking
        User blockedUser = findUserById(blockedUserId); // Find the user to be unblocked

        // Check if the user is blocked
        if (!user.getBlockedUsers().contains(blockedUser)) {
            throw new RuntimeException("User is not blocked"); // Prevent unblocking someone who isn't blocked
        }

        user.getBlockedUsers().remove(blockedUser); // Remove from the blocked list
        userRepository.save(user); // Save the updated user
    }
    // Get blocked users
    public List<User> getBlockedUsers(Long userId) {
        return userRepository.findBlockedUsers(userId); // Fetch blocked users from the repository
    }
    // Update privacy setting
    public void updatePrivacySetting(Long userId, Boolean isPrivate) {
        User user = findUserById(userId); // Find the user
        user.setIsPrivate(isPrivate); // Update the privacy setting
        userRepository.save(user); // Save the updated user
    }

    // Fetch privacy setting
    public Boolean getPrivacySetting(Long userId) {
        User user = findUserById(userId); // Find the user
        return user.getIsPrivate(); // Return the privacy setting
    }

    // Check if a profile is accessible to a given user
    public boolean isProfileAccessible(Long profileUserId, Long requestingUserId) {
        User profileUser = findUserById(profileUserId); // Find the profile user
        User requestingUser = findUserById(requestingUserId); // Find the requesting user

        // If the profile is public, it's accessible to everyone
        if (!profileUser.getIsPrivate()) {
            return true;
        }

        // If the profile is private, only the user themselves and their followers can access it
        return profileUserId.equals(requestingUserId) || profileUser.getFollowers().contains(requestingUser);
    }
}