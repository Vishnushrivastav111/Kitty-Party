package com.microvault.auth.seed;

import com.microvault.auth.entity.User;
import com.microvault.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void createsTheDefaultAdminWhenMissing() {
        when(userRepository.findByEmailIgnoreCase("admin@microvault.local")).thenReturn(Optional.empty());
        AdminSeeder seeder = seeder();

        seeder.run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User admin = saved.getValue();
        assertEquals("MicroVault Admin", admin.getFullName());
        assertEquals("admin@microvault.local", admin.getEmail());
        assertEquals("admin", admin.getRole());
        assertEquals("active", admin.getStatus());
        assertFalse(admin.isDeleted());
    }

    @Test
    void skipsWhenTheAdminAlreadyExists() {
        when(userRepository.findByEmailIgnoreCase("admin@microvault.local")).thenReturn(Optional.of(new User()));
        AdminSeeder seeder = seeder();

        seeder.run(null);

        verify(userRepository, never()).save(any());
    }

    private AdminSeeder seeder() {
        return new AdminSeeder(userRepository, "MicroVault Admin", "admin@microvault.local", "9876543210", "Admin@123");
    }
}
