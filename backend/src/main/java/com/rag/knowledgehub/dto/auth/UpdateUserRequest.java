package com.rag.knowledgehub.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank
    private String nickname;

    @NotBlank
    private String role;

    private Boolean enabled;
}
