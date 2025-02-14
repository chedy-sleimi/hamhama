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

        // Create and return the UserProfile
        return new UserProfile(user.getUsername(), user.getEmail(), followersCount, followingCount, likedRecipes);
    }
}