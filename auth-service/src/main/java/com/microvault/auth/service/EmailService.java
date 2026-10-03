package com.microvault.auth.service;

public interface EmailService {

    /**
     * Sends the code when mail is enabled.
     * Returns false when mail is turned off so the caller can show a local demo code.
     */
    boolean sendOtp(String email, String otp);
}
