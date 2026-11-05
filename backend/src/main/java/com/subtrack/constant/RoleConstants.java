package com.subtrack.constant;

import lombok.Getter;

@Getter
public enum RoleConstants {
    ROLE_USER("ROLE_USER"),
    ROLE_ADMIN("ROLE_ADMIN"),
    ROLE_PREMIUM("ROLE_PREMIUM");

    private final String roleName;

    RoleConstants(String roleName) {
        this.roleName = roleName;
    }

    public static final class Strings {
        private Strings() {}
        public static final String ROLE_USER = "ROLE_USER";
        public static final String ROLE_ADMIN = "ROLE_ADMIN";
        public static final String ROLE_PREMIUM = "ROLE_PREMIUM";
    }
}