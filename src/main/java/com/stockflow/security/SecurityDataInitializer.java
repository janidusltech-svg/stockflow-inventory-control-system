package com.stockflow.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(name = "stockflow.security.seed-demo-users", havingValue = "true", matchIfMissing = true)
public class SecurityDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapPassword;

    public SecurityDataInitializer(UserAccountRepository userAccountRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${stockflow.security.bootstrap-password:StockFlow@2026}")
                                   String bootstrapPassword) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SeedAccount> accounts = List.of(
                new SeedAccount("operations", "Operations Manager", "operations@stockflow.local",
                        UserRole.OPERATIONS_MANAGER),
                new SeedAccount("store", "Store Supervisor", "store@stockflow.local",
                        UserRole.STORE_SUPERVISOR),
                new SeedAccount("finance", "Finance Executive", "finance@stockflow.local",
                        UserRole.FINANCE_EXECUTIVE),
                new SeedAccount("service", "Customer Service Coordinator", "service@stockflow.local",
                        UserRole.CUSTOMER_SERVICE_COORDINATOR),
                new SeedAccount("itadmin", "IT and Systems Officer", "itadmin@stockflow.local",
                        UserRole.IT_SYSTEMS_OFFICER));

        int created = 0;
        for (SeedAccount seed : accounts) {
            if (userAccountRepository.existsByUsernameIgnoreCase(seed.username())) {
                continue;
            }
            userAccountRepository.save(new UserAccount(
                    seed.username(),
                    passwordEncoder.encode(bootstrapPassword),
                    seed.fullName(),
                    seed.email(),
                    seed.role(),
                    true));
            created++;
        }

        if (created > 0) {
            LOGGER.info("Created {} StockFlow role-based demonstration accounts", created);
        }
    }

    private record SeedAccount(String username, String fullName, String email, UserRole role) {
    }
}
