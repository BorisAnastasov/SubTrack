package com.subtrack.config;

import com.subtrack.constant.RoleConstants;
import com.subtrack.model.Role;
import com.subtrack.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            for (RoleConstants roleEnum : RoleConstants.values()) {
                if (roleRepository.findByName(roleEnum.name()).isEmpty()) {
                    roleRepository.save(new Role(null, roleEnum.name()));
                }
            }
        };
    }
}