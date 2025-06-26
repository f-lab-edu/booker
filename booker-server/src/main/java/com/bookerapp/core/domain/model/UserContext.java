package com.bookerapp.core.domain.model;

import java.util.List;

public class UserContext {
    private final String userId;
    private final String username;
    private final String email;
    private final List<String> roles;

    public UserContext(String userId, String username, String email, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
} 