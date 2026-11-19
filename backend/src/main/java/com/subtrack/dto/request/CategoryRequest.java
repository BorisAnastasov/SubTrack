package com.subtrack.dto.request;

import com.subtrack.constant.DbConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank
        @Size(max = DbConstants.CATEGORY_NAME_MAX_LENGTH)
        String name,

        @Pattern(regexp = "^#(?:[0-9a-fA-F]{3}){1,2}$")
        @Size(max = DbConstants.COLOR_HEX_MAX_LENGTH)
        String color
) {
}
