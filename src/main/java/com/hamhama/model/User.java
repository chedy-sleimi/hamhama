package com.hamhama.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors; // Import Collectors

@Entity
@Table(name = "users")
public class User implements UserDetails { // Implement UserDetails for security

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50) // Added length constraint
    private String username;

    @Column(nullable = false) // Password should not be null
    private String password;

    @Column(unique = true, nullable = false, length = 100) // Added length constraint
    private String email;

    // Consider removing isAdmin if using Roles consistently
    // private Boolean isAdmin = false;

    private Boolean isPrivate = false; // Default to public profile

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id")) // Customize table name
    @Column(name = "role") // Customize column name
    private Set<Role> roles = new HashSet<>(); // Roles stored as Set


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true) // Added orphanRemoval
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


    // --- UserDetails Implementation ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Map roles to Spring Security's GrantedAuthority format (e.g., "ROLE_USER", "ROLE_ADMIN")
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Add logic if accounts can expire
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Add logic if accounts can be locked
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Add logic if credentials can expire
    }

    @Override
    public boolean isEnabled() {
        return true; // Add logic if accounts can be disabled
    }

    // --- Standard Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public List<Recipe> getRecipes() { return recipes; }
    public void setRecipes(List<Recipe> recipes) { this.recipes = recipes; }

    public List<User> getFollowing() { return following; }
    public void setFollowing(List<User> following) { this.following = following; }

    public List<User> getFollowers() { return followers; }
    public void setFollowers(List<User> followers) { this.followers = followers; }

    public List<Recipe> getLikedRecipes() { return likedRecipes; }
    public void setLikedRecipes(List<Recipe> likedRecipes) { this.likedRecipes = likedRecipes; }

    public List<User> getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(List<User> blockedUsers) { this.blockedUsers = blockedUsers; }

    // --- equals, hashCode, toString (Recommended) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) || Objects.equals(username, user.username); // ID preferred, username fallback
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username); // Use immutable or unique fields
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", isPrivate=" + isPrivate +
                '}';
    }
}