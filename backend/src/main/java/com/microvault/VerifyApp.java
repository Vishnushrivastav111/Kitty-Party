package com.microvault;

import com.microvault.config.DatabaseConfig;
import com.microvault.daoimpl.UserDAOImpl;
import com.microvault.dto.UserDTO;
import com.microvault.model.User;
import com.microvault.service.UserService;
import com.microvault.serviceimpl.UserServiceImpl;
import com.microvault.util.DBConnection;
import com.microvault.util.PasswordUtil;

/**
 * Small Sprint 1 smoke check. Run from the backend folder:
 *
 *   mvn -q -DskipTests compile
 *   java -cp "target/classes;target/dependency/*" com.microvault.VerifyApp
 */
public class VerifyApp {

    public static void main(String[] args) {
        System.out.println("MicroVault Sprint 1 verification");
        System.out.println("--------------------------------");

        boolean passwordOk = verifyPasswordHashing();
        boolean databaseOk = verifyDatabase();
        boolean crudOk = databaseOk && verifyUserCrud();

        System.out.println("--------------------------------");
        System.out.println("Password hashing : " + status(passwordOk));
        System.out.println("Database connect : " + status(databaseOk));
        System.out.println("User CRUD        : " + status(crudOk));

        if (!passwordOk || !databaseOk || !crudOk) {
            System.exit(1);
        }
    }

    private static boolean verifyPasswordHashing() {
        String hash = PasswordUtil.hash("User@1234");
        boolean matches = PasswordUtil.matches("User@1234", hash);
        boolean rejectsWrong = !PasswordUtil.matches("Wrong@1234", hash);
        System.out.println("Password hash sample: " + hash.substring(0, 24) + "...");
        return matches && rejectsWrong && hash.startsWith("pbkdf2_sha256$");
    }

    private static boolean verifyDatabase() {
        System.out.println("JDBC URL: " + DatabaseConfig.getUrl());
        System.out.println("Username: " + DatabaseConfig.getUsername());
        try {
            boolean available = DBConnection.isAvailable();
            System.out.println("PostgreSQL reachable: " + available);
            return available;
        } catch (Exception exception) {
            System.out.println("PostgreSQL error: " + exception.getMessage());
            return false;
        }
    }

    private static boolean verifyUserCrud() {
        UserService userService = new UserServiceImpl(new UserDAOImpl());
        String email = "verify." + System.currentTimeMillis() + "@microvault.test";

        try {
            User user = new User();
            user.setFullName("Verify User");
            user.setEmail(email);
            user.setPhone("9876501234");
            user.setRole("user");
            user.setStatus("active");

            UserDTO created = userService.createUser(user, "User@1234");
            UserDTO found = userService.getUserById(created.getId());
            UserDTO loggedIn = userService.login(email, "User@1234");
            boolean deleted = userService.softDeleteUser(created.getId());

            System.out.println("Created user id: " + created.getId());
            System.out.println("Found user    : " + found.getFullName());
            System.out.println("Login email   : " + loggedIn.getEmail());
            System.out.println("Soft deleted  : " + deleted);
            return created.getId() != null && deleted;
        } catch (Exception exception) {
            System.out.println("CRUD error: " + exception.getMessage());
            return false;
        }
    }

    private static String status(boolean ok) {
        return ok ? "OK" : "FAILED";
    }
}
