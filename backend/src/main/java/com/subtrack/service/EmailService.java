package com.subtrack.service;

import java.util.List;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
    void sendPasswordResetEmail(String toEmail, String token);
    void sendRenewalDigestEmail(String toEmail, List<RenewalReminderEmailItem> renewals);
}
