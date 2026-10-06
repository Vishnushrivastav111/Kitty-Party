package com.microvault.auth.serviceimpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl service(boolean enabled) {
        EmailServiceImpl service = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(service, "enabled", enabled);
        ReflectionTestUtils.setField(service, "from", "noreply@microvault.test");
        return service;
    }

    @Test
    void whenMailIsDisabledNothingIsSentAndFalseIsReturned() {
        assertFalse(service(false).sendOtp("asha@example.com", "123456"));

        verifyNoInteractions(mailSender);
    }

    @Test
    void whenMailIsEnabledTheCodeIsEmailedAndTrueIsReturned() {
        assertTrue(service(true).sendOtp("asha@example.com", "123456"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertArrayEquals(new String[] {"asha@example.com"}, message.getTo());
        assertEquals("noreply@microvault.test", message.getFrom());
        assertEquals("MicroVault password reset code", message.getSubject());
        assertTrue(message.getText().contains("123456"));
        assertTrue(message.getText().contains("expires in 10 minutes"));
    }

    @Test
    void aMailServerFailureIsPropagatedToTheCaller() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        EmailServiceImpl service = service(true);

        assertThrows(MailSendException.class, () -> service.sendOtp("asha@example.com", "123456"));
    }
}
