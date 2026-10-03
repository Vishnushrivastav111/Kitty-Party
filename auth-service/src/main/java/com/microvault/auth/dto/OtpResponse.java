package com.microvault.auth.dto;

public class OtpResponse {

    private boolean ok;
    private String message;
    private OtpData data;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OtpData getData() {
        return data;
    }

    public void setData(OtpData data) {
        this.data = data;
    }

    public static class OtpData {
        private String email;
        private String demoOtp;
        private int expiresInMinutes;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDemoOtp() {
            return demoOtp;
        }

        public void setDemoOtp(String demoOtp) {
            this.demoOtp = demoOtp;
        }

        public int getExpiresInMinutes() {
            return expiresInMinutes;
        }

        public void setExpiresInMinutes(int expiresInMinutes) {
            this.expiresInMinutes = expiresInMinutes;
        }
    }
}
