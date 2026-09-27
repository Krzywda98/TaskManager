package com.example.taskmanager.services;

import com.example.taskmanager.models.User;
import com.example.taskmanager.models.enums.Role;
import com.example.taskmanager.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CurrentUser {
    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Account no longer exists"));
    }

    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    public void requireSelfOrAdmin(User actor, Long userId) {
        if (!isAdmin(actor) && !Objects.equals(actor.getId(), userId)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public void requireAdmin(User actor) {
        if (!isAdmin(actor)) {
            throw new AccessDeniedException("Administrator access required");
        }
    }
}
