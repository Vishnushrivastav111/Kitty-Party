package com.microvault.auth.seed;

import com.microvault.auth.entity.User;
import com.microvault.auth.repository.UserRepository;
import com.microvault.auth.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String password;

    public AdminSeeder(UserRepository userRepository,
                       @Value("${microvault.seed.admin.full-name}") String fullName,
                       @Value("${microvault.seed.admin.email}") String email,
                       @Value("${microvault.seed.admin.phone}") String phone,
                       @Value("${microvault.seed.admin.password}") String password) {
        this.userRepository = userRepository;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmailIgnoreCase(email.trim()).isPresent()) {
            log.info("Default admin already exists for {}", email);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setFullName(fullName.trim());
        admin.setEmail(email.trim().toLowerCase());
        admin.setPhone(phone.trim());
        admin.setPasswordHash(PasswordUtil.hash(password));
        admin.setRole("admin");
        admin.setStatus("active");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin.setDeleted(false);
        userRepository.save(admin);
        log.info("Created default admin {}", admin.getEmail());
    }
}
