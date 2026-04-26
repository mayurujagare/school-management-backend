// src/main/java/com/schoolmanagement/module/auth/dto/AuthResponse.java
package com.schoolmanagement.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String       accessToken;
    private String       refreshToken;
    private String       tokenType;
    private UserInfo     user;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfo {
        private UUID         id;
        private UUID         schoolId;
        private String       firstName;
        private String       lastName;
        private String       email;
        private String       mobile;
        private List<String> roles;
        private boolean      mustChangePassword;  // true on first staff login
    }
}