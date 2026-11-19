package com.subtrack.constant;

public final class MessageConstants {


    private MessageConstants() {
    }

    public static final String VALIDATION_EMAIL_REQUIRED = "Email is required";
    public static final String VALIDATION_EMAIL_INVALID = "Invalid email format";
    public static final String VALIDATION_EMAIL_TOO_LONG
            = "Email must not exceed " + DbConstants.EMAIL_MAX_LENGTH + " characters";
    public static final String VALIDATION_PASSWORD_REQUIRED = "Password is required";
    public static final String VALIDATION_PASSWORD_LENGTH
            = "Password must be between " + DbConstants.PASSWORD_MIN_LENGTH + " and "
            + DbConstants.PASSWORD_MAX_LENGTH + " characters";
    public static final String VALIDATION_REFRESH_TOKEN_REQUIRED = "Refresh token is required";
    public static final String VALIDATION_NAME_REQUIRED = "Name is required";
    public static final String VALIDATION_NAME_TOO_LONG
            = "Name must not exceed " + DbConstants.SUBSCRIPTION_NAME_MAX_LENGTH + " characters";
    public static final String VALIDATION_PRICE_REQUIRED = "Price is required";
    public static final String VALIDATION_PRICE_POSITIVE = "Price must be greater than zero";
    public static final String VALIDATION_CURRENCY_REQUIRED = "Currency is required";
    public static final String VALIDATION_CURRENCY_FORMAT = "Currency must be a three-letter uppercase code";
    public static final String VALIDATION_BILLING_CYCLE_REQUIRED = "Billing cycle is required";
    public static final String VALIDATION_START_DATE_REQUIRED = "Start date is required";

    public static final String ERROR_EMAIL_ALREADY_TAKEN = "Email is already taken!";
    public static final String ERROR_EMAIL_NOT_VERIFIED = "Verify your email before signing in";
    public static final String ERROR_VERIFICATION_TOKEN_INVALID = "Verification link is invalid or already used";
    public static final String ERROR_VERIFICATION_TOKEN_EXPIRED = "Verification link has expired";
    public static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password";
    public static final String ERROR_REFRESH_TOKEN_EXPIRED
            = "Refresh token was expired. Please make a new signin request";
    public static final String ERROR_REFRESH_TOKEN_NOT_FOUND = "Refresh token is not in database!";
    public static final String ERROR_RESET_PASSWORD_TOKEN_INVALID = "Reset password link is invalid or already used";
    public static final String ERROR_USER_NOT_FOUND = "User not found with id: ";
    public static final String ERROR_USER_EMAIL_NOT_FOUND = "User not found with email: ";
    public static final String ERROR_ROLE_NOT_FOUND = "Default user role not found in database";
    public static final String ERROR_UNAUTHORIZED_RESOURCE = "You do not have permission to access this resource";
    public static final String VALIDATION_RESET_TOKEN_REQUIRED = "Reset token is required";


    public static final String ERROR_SUBSCRIPTION_NOT_FOUND = "Subscription not found with id: ";
    public static final String ERROR_CATEGORY_NOT_FOUND = "Category not found with id: ";
    public static final String ERROR_TAX_OBLIGATION_NOT_FOUND = "Tax obligation not found with id: ";

    public static final String SUCCESS_LOGOUT = "Logged out successfully";
}