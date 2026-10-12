package com.municipal.system.config;

import com.municipal.system.department.entity.Department;
import com.municipal.system.department.entity.DepartmentCode;
import com.municipal.system.department.repository.DepartmentRepository;
import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds departments and default accounts when running under the "local" profile.
 */
@Slf4j
@Component
@Profile("local")
@Order(1)
@RequiredArgsConstructor
public class LocalDepartmentDataInitializer implements ApplicationRunner {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (departmentRepository.count() == 0) {
            log.info("Seeding initial municipal departments for local profile...");
            departmentRepository.save(Department.builder()
                    .code(DepartmentCode.ROADS)
                    .name("Roads & Infrastructure")
                    .description("City roads, potholes, pavements, bridges, and traffic signage")
                    .build());

            departmentRepository.save(Department.builder()
                    .code(DepartmentCode.WATER)
                    .name("Water Supply & Sanitation")
                    .description("Municipal water pipelines, leaks, sewage, and drainage systems")
                    .build());

            departmentRepository.save(Department.builder()
                    .code(DepartmentCode.ELECTRICAL)
                    .name("Electricity & Public Lighting")
                    .description("Streetlights, power line faults, and municipal electrical grid")
                    .build());
            log.info("Default departments seeded successfully.");
        }

        if (!userRepository.existsByEmail("citizen@municipal.gov")) {
            log.info("Seeding default Citizen user (citizen@municipal.gov)...");
            userRepository.save(User.builder()
                    .email("citizen@municipal.gov")
                    .fullName("Alex Citizen")
                    .passwordHash(passwordEncoder.encode("UserPassword123!"))
                    .role(Role.CITIZEN)
                    .phone("+1-555-0155")
                    .build());
            log.info("Default citizen user seeded successfully.");
        }
    }
}
