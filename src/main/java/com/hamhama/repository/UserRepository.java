package com.hamhama.repository;

import com.hamhama.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    @Query("SELECT u FROM User u JOIN u.following f WHERE f.id = :userId")
    List<User> findFollowers(Long userId);  // Query to get followers of a specific user

    @Query("SELECT u.blockedUsers FROM User u WHERE u.id = :userId")
    List<User> findBlockedUsers(Long userId); // Query to get blocked users for a specific user

}
