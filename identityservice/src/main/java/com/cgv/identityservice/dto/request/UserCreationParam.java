package com.cgv.identityservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class UserCreationParam {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private boolean emailVerified;
    private List<Credential> credentials;

    @Data
    @Builder
    public static class Credential {
        private String type;
        private String value;
        private boolean temporary;
    }
}
