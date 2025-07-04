package com.bookerapp.core.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class UserResponse {
    private final String userId;
    private final String username;
    private final String email;
    private final Set<Role> roles;
    private final boolean authenticated;
} 