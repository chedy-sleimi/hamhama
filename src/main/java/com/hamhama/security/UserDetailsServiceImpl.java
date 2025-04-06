package com.hamhama.security;

import com.hamhama.model.User;
import com.hamhama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

@Service
@RequiredArgsConstructor // Lombok for constructor injection
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // Good practice for read operations
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try finding by username first
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // If you want to allow login by email as well, uncomment and adapt this:
        // User user = userRepository.findByUsername(usernameOrEmail)
        //      .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
        //            .orElseThrow(() -> new UsernameNotFoundException("User Not Found with: " + usernameOrEmail)));


        // The User entity itself implements UserDetails, so we can return it directly.
        // Spring Security will use the getAuthorities(), getPassword(), etc. methods from the User class.
        return user;
    }
}