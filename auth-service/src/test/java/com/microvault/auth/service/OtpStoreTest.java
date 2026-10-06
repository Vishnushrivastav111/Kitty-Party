package com.microvault.auth.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpStoreTest {

    private final OtpStore store = new OtpStore();

    @Test
    void acceptsTheSavedCode() {
        store.save("asha@example.com", "123456");

        assertTrue(store.matches("asha@example.com", "123456"));
    }

    @Test
    void rejectsAWrongCode() {
        store.save("asha@example.com", "123456");

        assertFalse(store.matches("asha@example.com", "654321"));
    }

    @Test
    void rejectsAnEmailThatNeverRequestedACode() {
        assertFalse(store.matches("ghost@example.com", "123456"));
    }

    @Test
    void emailLookupIgnoresCaseAndSurroundingWhitespace() {
        store.save("  Asha@Example.COM ", "123456");

        assertTrue(store.matches("asha@example.com", "123456"));
        assertTrue(store.matches(" ASHA@example.com  ", "123456"));
    }

    @Test
    void aNullEmailIsTreatedAsAnEmptyKey() {
        store.save(null, "111111");

        assertTrue(store.matches(null, "111111"));
        assertTrue(store.matches("", "111111"));
    }

    @Test
    void aNewCodeReplacesThePreviousOne() {
        store.save("asha@example.com", "111111");
        store.save("asha@example.com", "222222");

        assertFalse(store.matches("asha@example.com", "111111"));
        assertTrue(store.matches("asha@example.com", "222222"));
    }

    @Test
    void clearForgetsTheCode() {
        store.save("asha@example.com", "123456");

        store.clear("asha@example.com");

        assertFalse(store.matches("asha@example.com", "123456"));
    }

    @Test
    void clearingAnUnknownEmailIsHarmless() {
        store.clear("ghost@example.com");

        assertFalse(store.matches("ghost@example.com", "123456"));
    }

    @Test
    void anExpiredCodeIsRejectedAndDiscarded() throws Exception {
        store.save("asha@example.com", "123456");
        expire("asha@example.com");

        assertFalse(store.matches("asha@example.com", "123456"));
        // it was removed, so even asking again cannot succeed
        assertFalse(store.matches("asha@example.com", "123456"));
    }

    @Test
    void theCodeIsLockedAfterFiveAttempts() {
        store.save("asha@example.com", "123456");

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertFalse(store.matches("asha@example.com", "000000"), "attempt " + attempt);
        }

        // the sixth attempt is refused even with the right code, and the code is gone
        assertFalse(store.matches("asha@example.com", "123456"));
        assertFalse(store.matches("asha@example.com", "123456"));
    }

    @Test
    void theRightCodeStillWorksOnTheFifthAttempt() {
        store.save("asha@example.com", "123456");

        for (int attempt = 1; attempt <= 4; attempt++) {
            assertFalse(store.matches("asha@example.com", "000000"));
        }

        assertTrue(store.matches("asha@example.com", "123456"));
    }

    @SuppressWarnings("unchecked")
    private void expire(String email) throws Exception {
        Field codesField = OtpStore.class.getDeclaredField("codes");
        codesField.setAccessible(true);
        Map<String, Object> codes = (Map<String, Object>) codesField.get(store);
        Object otp = codes.get(email);
        Field expiresAt = otp.getClass().getDeclaredField("expiresAt");
        expiresAt.setAccessible(true);
        expiresAt.set(otp, LocalDateTime.now().minusMinutes(1));
    }
}
