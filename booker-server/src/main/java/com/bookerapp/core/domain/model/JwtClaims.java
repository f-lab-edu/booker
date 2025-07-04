package com.bookerapp.core.domain.model;

public final class JwtClaims {
    private JwtClaims() {}

    public static final String EMAIL = "email";
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String REALM_ACCESS = "realm_access";
    public static final String ROLES = "roles";
} 