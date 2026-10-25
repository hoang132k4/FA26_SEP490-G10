package com.jlpp.modules.auth.service.impl;

import com.jlpp.modules.auth.model.CustomUserDetails;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Invalid email or password.");
        }

        String clean = identifier.trim();
        User user = userRepository.findByEmailIgnoreCase(clean)
                .or(() -> userRepository.findByUsername(clean))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));

        return new CustomUserDetails(user);
    }
}
