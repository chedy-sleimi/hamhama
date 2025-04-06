package com.hamhama.repository;

import com.hamhama.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository; // Add Repository annotation

import java.util.List;
import java.util.Optional;

@Repository // Add annotation
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // Consider if these custom queries are strictly needed if using JPA relationships directly
    @Query("SELECT u FROM User u JOIN u.followers f WHERE f.id = :userId") // Corrected query based on mapping
    List<User> findFollowers(Long userId);

    @Query("SELECT u.blockedUsers FROM User u WHERE u.id = :userId")
    List<User> findBlockedUsers(Long userId);

    // Add findByEmail if login via email is needed
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username); // Useful for registration check
    boolean existsByEmail(String email);      // Useful for registration check
}