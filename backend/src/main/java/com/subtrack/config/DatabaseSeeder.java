package com.subtrack.config;

import com.subtrack.model.*;
import com.subtrack.model.enums.BillingCycle;
import com.subtrack.model.enums.SubscriptionStatus;
import com.subtrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TaxObligationRepository taxObligationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting database seeding process...");

        Role userRole = seedRole("ROLE_USER");
        Role adminRole = seedRole("ROLE_ADMIN");

        User demoUser = seedUser("demo@subtrack.com", "password123", false, Set.of(userRole));
        seedUser("admin@subtrack.com", "admin123", true, Set.of(userRole, adminRole));

        Category streamingCat = seedCategory("Streaming", "#E50914", null);
        Category softwareCat = seedCategory("Software & Tools", "#007ACC", null);
        seedCategory("Utilities", "#F5A623", null);
        seedCategory("Taxes & State", "#4A90E2", null);

        seedSubscriptions(demoUser, streamingCat, softwareCat);

        seedTaxObligations(demoUser);

        log.info("Database seeding completed successfully!");
    }

    private Role seedRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role(null, roleName);
                    log.info("Seeding role: {}", roleName);
                    return roleRepository.save(role);
                });
    }

    private User seedUser(String email, String rawPassword, boolean isPremium, Set<Role> roles) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (!user.isVerified()) {
                        user.setVerified(true);
                        return userRepository.save(user);
                    }
                    return user;
                })
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(email);
                    user.setPasswordHash(passwordEncoder.encode(rawPassword));
                    user.setPremium(isPremium);
                    user.setVerified(true);
                    user.setRoles(new HashSet<>(roles));
                    log.info("Seeding user: {}", email);
                    return userRepository.save(user);
                });
    }

    private Category seedCategory(String name, String color, User user) {
        List<Category> existing = categoryRepository.findByUserIdOrUserIsNull(user != null ? user.getId() : null);
        return existing.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> {
                    Category category = new Category(null, name, color, user);
                    log.info("Seeding category: {}", name);
                    return categoryRepository.save(category);
                });
    }

    private void seedSubscriptions(User user, Category streaming, Category software) {
        if (subscriptionRepository.findByUserId(user.getId()).isEmpty()) {
            Subscription netflix = new Subscription(
                    null,
                    user,
                    streaming,
                    "Netflix Standard",
                    new BigDecimal("19.99"),
                    "BGN",
                    BillingCycle.MONTHLY,
                    LocalDate.now().plusDays(12),
                    "Credit Card",
                    SubscriptionStatus.ACTIVE,
                    "Family account"
            );

            Subscription jetbrains = new Subscription(
                    null,
                    user,
                    software,
                    "IntelliJ IDEA Ultimate",
                    new BigDecimal("29.00"),
                    "EUR",
                    BillingCycle.MONTHLY,
                    LocalDate.now().plusDays(5),
                    "Debit Card",
                    SubscriptionStatus.ACTIVE,
                    "Developer pack"
            );

            subscriptionRepository.saveAll(List.of(netflix, jetbrains));
            log.info("Seeded initial subscriptions for {}", user.getEmail());
        }
    }

    private void seedTaxObligations(User user) {
        if (taxObligationRepository.findByUserId(user.getId()).isEmpty()) {
            TaxObligation vignette = new TaxObligation(
                    null,
                    user,
                    "Годишна винетка МПС",
                    new BigDecimal("87.00"),
                    LocalDate.now().plusMonths(2),
                    false,
                    14
            );

            taxObligationRepository.save(vignette);
            log.info("Seeded initial tax obligations for {}", user.getEmail());
        }
    }
}