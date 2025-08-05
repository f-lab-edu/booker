package com.bookerapp.core.domain.model.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.util.List;
import com.bookerapp.core.domain.model.auth.Role;

public class UserResponse {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Request {
        private String userId;
        private String username;
        private String email;
        private List<Role> roles;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Response {
        private final String userId;
        private final String username;
        private final String email;
        private final List<Role> roles;
        private final boolean authenticated;
    }
}
