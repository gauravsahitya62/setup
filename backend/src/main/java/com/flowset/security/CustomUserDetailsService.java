package com.flowset.security;

import com.flowset.users.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository
                .findActiveByEmail(username)
                .map(user -> new UserPrincipal(
                        user.getId(),
                        user.getEmail(),
                        user.getPasswordHash(),
                        user.getRole(),
                        user.getStatus(),
                        user.isEmailVerified()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
