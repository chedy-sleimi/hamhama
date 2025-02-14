package com.hamhama.controller;

import com.hamhama.dto.UserProfile;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.service.UserService;
import org.springframework.web.bind.annotation.*;

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

}