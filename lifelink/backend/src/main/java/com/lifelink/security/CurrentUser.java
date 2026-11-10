package com.lifelink.security;

import com.lifelink.exception.ApiException;
import com.lifelink.model.User;
import com.lifelink.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the signed-in user for the current request. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw ApiException.unauthorized("Please sign in to continue");
        }
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> ApiException.unauthorized("Please sign in again"));
    }
}
