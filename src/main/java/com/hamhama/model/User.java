package com.hamhama.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Table(name = "users")
public class User implements UserDetails { // Implement UserDetails for security

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;

    private String email;

    private Boolean isAdmin = false;

    private Boolean isPrivate = false; // Default to public profile

    @Enumerated(EnumType.STRING) // Store Role as String (USER or ADMIN)
    @ElementCollection(fetch = FetchType.EAGER) // Correct annotation for collection of enum values
    private Set<Role> roles = new HashSet<>(); // Roles stored as Set


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Recipe> recipes = new ArrayList<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_follows",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    private List<User> following = new ArrayList<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "following")
    private List<User> followers = new ArrayList<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_likes",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "recipe_id")
    )
    private List<Recipe> likedRecipes = new ArrayList<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "blocked_users", // New table to track blocked users
            joinColumns = @JoinColumn(name = "user_id"), // The user who is blocking
            inverseJoinColumns = @JoinColumn(name = "blocked_user_id") // The user being blocked
    )
    private List<User> blockedUsers = new ArrayList<>(); // List of users blocked by this user




    // ✅ Add Getter & Setter for Roles
    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    // ✅ Implement UserDetails Methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())); // Convert role to Spring format
        }
        return authorities;
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // ✅ Standard Getters & Setters

    public List<User> getBlockedUsers() {
        return blockedUsers;
    }

    public void setBlockedUsers(List<User> blockedUsers) {
        this.blockedUsers = blockedUsers;
    }

    // Add getter and setter for isPrivate
    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<User> getFollowing() {
        return following;
    }

    public void setFollowing(List<User> following) {
        this.following = following;
    }

    public List<User> getFollowers() {
        return followers;
    }

    public void setFollowers(List<User> followers) {
        this.followers = followers;
    }

    public List<Recipe> getLikedRecipes() {
        return likedRecipes;
    }

    public void setLikedRecipes(List<Recipe> likedRecipes) {
        this.likedRecipes = likedRecipes;
    }
}
