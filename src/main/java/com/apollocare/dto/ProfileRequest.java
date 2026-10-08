package com.apollocare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProfileRequest(
        @NotBlank(message = "Name is required") String name,
        @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Phone number is not valid") String phone) {
}
