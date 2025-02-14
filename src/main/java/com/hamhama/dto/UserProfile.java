package com.hamhama.dto;

import com.hamhama.model.Recipe;

import java.util.List;

public class UserProfile {

    private String username;
    private String email;
    private int followersCount;
    private int followingCount;
    private List<Recipe> likedRecipes;

    public UserProfile(String username, String email, int followersCount, int followingCount, List<Recipe> likedRecipes) {
        this.username = username;
        this.email = email;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.likedRecipes = likedRecipes;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public List<Recipe> getLikedRecipes() {
        return likedRecipes;
    }

    public void setLikedRecipes(List<Recipe> likedRecipes) {
        this.likedRecipes = likedRecipes;
    }
}
