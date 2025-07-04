package com.bookerapp.core.domain.model;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class UserContext {
    private final String userId;
    private final String username;
    private final String email;
    private final List<String> roles;

    @SuppressWarnings("unchecked")
    public UserContext(Claims claims) {
        this.userId = claims.getSubject();
        this.email = claims.get("email", String.class);
        this.username = claims.get("preferred_username", String.class);
        
        Map<String, Object> realmAccess = claims.get("realm_access", Map.class);
        if (realmAccess != null && realmAccess.get("roles") != null) {
            this.roles = (List<String>) realmAccess.get("roles");
        } else {
            this.roles = Collections.emptyList();
        }
    }

    public List<Role> getRolesAsRole() {
        return roles.stream()
                .map(String::toUpperCase)
                .map(Role::valueOf)
                .collect(Collectors.toList());
    }
} 