package com.microvault.auth.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpStore {

    private static final int MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, Otp> codes = new ConcurrentHashMap<>();

    public void save(String email, String code) {
        Otp otp = new Otp();
        otp.code = code;
        otp.expiresAt = LocalDateTime.now().plusMinutes(MINUTES);
        otp.attempts = 0;
        codes.put(normalize(email), otp);
    }

    public boolean matches(String email, String code) {
        Otp otp = codes.get(normalize(email));
        if (otp == null) {
            return false;
        }
        if (otp.expiresAt.isBefore(LocalDateTime.now())) {
            codes.remove(normalize(email));
            return false;
        }
        otp.attempts = otp.attempts + 1;
        if (otp.attempts > MAX_ATTEMPTS) {
            codes.remove(normalize(email));
            return false;
        }
        return otp.code.equals(code);
    }

    public void clear(String email) {
        codes.remove(normalize(email));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class Otp {
        private String code;
        private LocalDateTime expiresAt;
        private int attempts;
    }
}
