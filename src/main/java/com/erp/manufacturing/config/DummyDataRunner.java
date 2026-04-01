package com.erp.manufacturing.config;

import com.erp.manufacturing.entity.User;
import com.erp.manufacturing.enums.UserRole;
import com.erp.manufacturing.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DummyDataRunner {

    private static final Logger log = LoggerFactory.getLogger(DummyDataRunner.class);

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User gm = new User();
                gm.setUsername("gm");
                gm.setPassword(passwordEncoder.encode("password"));
                gm.setRole(UserRole.GENERAL_MANAGER);
                gm.setName("General Manager");
                userRepository.save(gm);

                User designer = new User();
                designer.setUsername("designer");
                designer.setPassword(passwordEncoder.encode("password"));
                designer.setRole(UserRole.DESIGNER);
                designer.setName("Designer");
                userRepository.save(designer);

                User purchase = new User();
                purchase.setUsername("purchase");
                purchase.setPassword(passwordEncoder.encode("password"));
                purchase.setRole(UserRole.PURCHASE_MANAGER);
                purchase.setName("Purchase Manager");
                userRepository.save(purchase);

                User production = new User();
                production.setUsername("production");
                production.setPassword(passwordEncoder.encode("password"));
                production.setRole(UserRole.PRODUCTION_MANAGER);
                production.setName("Production Manager");
                userRepository.save(production);

                User accounts = new User();
                accounts.setUsername("accounts");
                accounts.setPassword(passwordEncoder.encode("password"));
                accounts.setRole(UserRole.ACCOUNTS);
                accounts.setName("Accounts");
                userRepository.save(accounts);

                log.info("Default users created successfully.");
            }
        };
    }
}
