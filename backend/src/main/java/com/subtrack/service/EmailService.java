package com.subtrack.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
}
