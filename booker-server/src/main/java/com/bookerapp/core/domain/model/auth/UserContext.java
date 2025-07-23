package com.bookerapp.core.domain.model.auth;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bookerapp.core.domain.model.auth.JwtClaims.*;

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
        this.email = claims.get(EMAIL, String.class);
        this.username = claims.get(PREFERRED_USERNAME, String.class);

        Map<String, Object> realmAccess = claims.get(REALM_ACCESS, Map.class);
        if (realmAccess != null && realmAccess.get(ROLES) != null) {
            this.roles = (List<String>) realmAccess.get(ROLES);
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

    public boolean hasRole(Role role) {
        return this.roles.contains(role.name());
    }
}
