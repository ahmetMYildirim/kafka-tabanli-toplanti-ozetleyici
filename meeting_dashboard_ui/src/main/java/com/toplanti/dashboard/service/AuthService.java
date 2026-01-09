package com.toplanti.dashboard.service;

import com.toplanti.dashboard.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static AuthService instance;

    private User currentUser;
    private String authToken;
    private boolean authenticated = false;

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public CompletableFuture<Boolean> login(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Login attempt for user: {}", username);

            
            if ("admin".equals(username) && "admin123".equals(password)) {
                currentUser = User.builder()
                        .id("1")
                        .username("admin")
                        .email("admin@company.com")
                        .fullName("Admin User")
                        .role(User.Role.CEO)
                        .department("Executive")
                        .active(true)
                        .build();
                authToken = "demo-token-admin";
                authenticated = true;
                log.info("Login successful for admin");
                return true;
            }

            if ("manager".equals(username) && "manager123".equals(password)) {
                currentUser = User.builder()
                        .id("2")
                        .username("manager")
                        .email("manager@company.com")
                        .fullName("Manager User")
                        .role(User.Role.MANAGER)
                        .department("Engineering")
                        .managerId("1")
                        .active(true)
                        .build();
                authToken = "demo-token-manager";
                authenticated = true;
                log.info("Login successful for manager");
                return true;
            }

            if ("user".equals(username) && "user123".equals(password)) {
                currentUser = User.builder()
                        .id("3")
                        .username("user")
                        .email("user@company.com")
                        .fullName("Regular User")
                        .role(User.Role.EMPLOYEE)
                        .department("Engineering")
                        .managerId("2")
                        .active(true)
                        .build();
                authToken = "demo-token-user";
                authenticated = true;
                log.info("Login successful for user");
                return true;
            }

            log.warn("Login failed for user: {}", username);
            return false;
        });
    }

    public CompletableFuture<Boolean> register(String username, String password, String email, String fullName, String department) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Register attempt for user: {}", username);

            currentUser = User.builder()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .role(User.Role.EMPLOYEE)
                    .department(department)
                    .active(true)
                    .build();
            authToken = "demo-token-" + username;
            authenticated = true;

            log.info("Registration successful for: {}", username);
            return true;
        });
    }

    public void logout() {
        log.info("Logging out user: {}", currentUser != null ? currentUser.getUsername() : "unknown");
        currentUser = null;
        authToken = null;
        authenticated = false;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean canAssignTaskTo(User targetUser) {
        if (currentUser == null || targetUser == null) return false;
        return currentUser.getRole().canAssignTaskTo(targetUser.getRole());
    }

    public boolean canViewTasksOf(User targetUser) {
        if (currentUser == null || targetUser == null) return false;
        return currentUser.getRole().canViewTasksOf(targetUser.getRole());
    }

    public boolean hasPermission(String permission) {
        if (currentUser == null) return false;

        return switch (permission) {
            case "CREATE_TASK" -> currentUser.getRole().getHierarchyLevel() <= 5;
            case "DELETE_TASK" -> currentUser.getRole().getHierarchyLevel() <= 3;
            case "VIEW_ALL_TASKS" -> currentUser.getRole().getHierarchyLevel() <= 2;
            case "MANAGE_USERS" -> currentUser.getRole().getHierarchyLevel() <= 2;
            case "EXPORT_DATA" -> currentUser.getRole().getHierarchyLevel() <= 4;
            default -> false;
        };
    }
}

