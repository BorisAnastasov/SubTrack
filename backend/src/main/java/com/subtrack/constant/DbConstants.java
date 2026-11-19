package com.subtrack.constant;

public final class DbConstants {

    private DbConstants() {}

    // Role
    public static final int ROLE_NAME_MAX_LENGTH = 50;

    // User
    public static final int USERNAME_MAX_LENGTH = 30;
    public static final int PASSWORD_MAX_LENGTH = 20;
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int PASSWORD_HASH_MAX_LENGTH = 255;

    // Category
    public static final int CATEGORY_NAME_MAX_LENGTH = 100;
    public static final int COLOR_HEX_MAX_LENGTH = 20;

    // Subscription
    public static final int SUBSCRIPTION_NAME_MAX_LENGTH = 100;
    public static final int CURRENCY_CODE_LENGTH = 3;
    public static final int PAYMENT_METHOD_MAX_LENGTH = 50;
    public static final int DECIMAL_PRECISION = 10;
    public static final int DECIMAL_SCALE = 2;

    // Tax Obligation
    public static final int TAX_TITLE_MAX_LENGTH = 150;

    // Common Enums column size
    public static final int ENUM_COLUMN_MAX_LENGTH = 20;
}