package com.microvault.auth.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashUsesTheDocumentedPbkdf2Format() {
        String[] parts = PasswordUtil.hash("Str0ng@Pass").split("\\$");

        assertEquals(4, parts.length);
        assertEquals("pbkdf2_sha256", parts[0]);
        assertEquals("120000", parts[1]);
        assertEquals(16, Base64.getDecoder().decode(parts[2]).length);
        assertEquals(32, Base64.getDecoder().decode(parts[3]).length);
    }

    @Test
    void hashingTheSamePasswordTwiceGivesDifferentHashes() {
        assertNotEquals(PasswordUtil.hash("Str0ng@Pass"), PasswordUtil.hash("Str0ng@Pass"));
    }

    @Test
    void matchesAcceptsTheOriginalPassword() {
        String hash = PasswordUtil.hash("Str0ng@Pass");

        assertTrue(PasswordUtil.matches("Str0ng@Pass", hash));
    }

    @Test
    void matchesRejectsAnotherPassword() {
        String hash = PasswordUtil.hash("Str0ng@Pass");

        assertFalse(PasswordUtil.matches("str0ng@pass", hash));
        assertFalse(PasswordUtil.matches("", hash));
    }

    @Test
    void matchesRejectsNullInputs() {
        String hash = PasswordUtil.hash("Str0ng@Pass");

        assertFalse(PasswordUtil.matches(null, hash));
        assertFalse(PasswordUtil.matches("Str0ng@Pass", null));
    }

    @Test
    void matchesRejectsAStoredValueWithTheWrongShape() {
        assertFalse(PasswordUtil.matches("x", "plain-text"));
        assertFalse(PasswordUtil.matches("x", "pbkdf2_sha256$120000$onlythree"));
        assertFalse(PasswordUtil.matches("x", "pbkdf2_sha256$120000$a$b$c"));
    }

    @Test
    void matchesRejectsAnotherAlgorithmPrefix() {
        String hash = PasswordUtil.hash("Str0ng@Pass").replaceFirst("pbkdf2_sha256", "md5");

        assertFalse(PasswordUtil.matches("Str0ng@Pass", hash));
    }

    @Test
    void matchesRejectsCorruptedParts() {
        String valid = PasswordUtil.hash("Str0ng@Pass");
        String[] parts = valid.split("\\$");

        assertFalse(PasswordUtil.matches("Str0ng@Pass", "pbkdf2_sha256$abc$" + parts[2] + "$" + parts[3]));
        assertFalse(PasswordUtil.matches("Str0ng@Pass", "pbkdf2_sha256$120000$***$" + parts[3]));
        assertFalse(PasswordUtil.matches("Str0ng@Pass", "pbkdf2_sha256$120000$" + parts[2] + "$***"));
        assertFalse(PasswordUtil.matches("Str0ng@Pass", "pbkdf2_sha256$0$" + parts[2] + "$" + parts[3]));
    }

    @Test
    void matchesRejectsAHashOfTheWrongLength() {
        String valid = PasswordUtil.hash("Str0ng@Pass");
        String[] parts = valid.split("\\$");
        String shortHash = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});

        assertFalse(PasswordUtil.matches("Str0ng@Pass", "pbkdf2_sha256$120000$" + parts[2] + "$" + shortHash));
    }

    @Test
    void matchesHonoursTheIterationCountStoredInTheHash() {
        String valid = PasswordUtil.hash("Str0ng@Pass");
        String[] parts = valid.split("\\$");

        // same salt and hash but a different iteration count cannot verify
        assertFalse(PasswordUtil.matches("Str0ng@Pass", "pbkdf2_sha256$1000$" + parts[2] + "$" + parts[3]));
    }

    @Test
    void utilityClassCannotBeInstantiatedByCallers() throws Exception {
        Constructor<PasswordUtil> constructor = PasswordUtil.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        try {
            assertEquals(PasswordUtil.class, constructor.newInstance().getClass());
        } catch (InvocationTargetException exception) {
            throw new AssertionError("private constructor must not throw", exception);
        }
        assertThrows(NullPointerException.class, () -> PasswordUtil.hash(null));
    }
}
