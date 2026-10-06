package com.microvault.auth.seed;

import com.microvault.auth.entity.User;
import com.microvault.auth.repository.UserRepository;
import com.microvault.auth.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    private AdminSeeder seeder() {
        return new AdminSeeder(userRepository, " Admin User ", " Admin@MicroVault.local ", " 9876543210 ", "Admin@123");
    }

    @Test
    void createsTheDefaultAdminWhenNoneExists() {
        when(userRepository.findByEmailIgnoreCase("Admin@MicroVault.local")).thenReturn(Optional.empty());

        seeder().run(new DefaultApplicationArguments());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User admin = captor.getValue();
        assertNotNull(admin.getId());
        assertEquals("Admin User", admin.getFullName());
        assertEquals("admin@microvault.local", admin.getEmail());
        assertEquals("9876543210", admin.getPhone());
        assertEquals("admin", admin.getRole());
        assertEquals("active", admin.getStatus());
        assertFalse(admin.isDeleted());
        assertNotNull(admin.getCreatedAt());
        assertEquals(admin.getCreatedAt(), admin.getUpdatedAt());
        assertTrue(PasswordUtil.matches("Admin@123", admin.getPasswordHash()));
    }

    @Test
    void doesNothingWhenTheAdminAlreadyExists() {
        when(userRepository.findByEmailIgnoreCase("Admin@MicroVault.local")).thenReturn(Optional.of(new User()));

        seeder().run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
    }

    @Test
    void propagatesARepositoryFailure() {
        when(userRepository.findByEmailIgnoreCase("Admin@MicroVault.local")).thenThrow(new IllegalStateException("db down"));
        AdminSeeder seeder = seeder();

        assertThrows(IllegalStateException.class, () -> seeder.run(new DefaultApplicationArguments()));
    }
}
