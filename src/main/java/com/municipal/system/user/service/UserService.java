package com.municipal.system.user.service;

import com.municipal.system.department.entity.Department;
import com.municipal.system.department.repository.DepartmentRepository;
import com.municipal.system.exception.ConflictException;
import com.municipal.system.exception.ResourceNotFoundException;
import com.municipal.system.user.dto.CreateOfficerRequest;
import com.municipal.system.user.dto.UserResponse;
import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createOfficer(CreateOfficerRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered: " + request.email());
        }

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));

        User officer = User.builder()
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .role(Role.OFFICER)
                .department(department)
                .zoneId(request.zoneId())
                .available(Boolean.TRUE)
                .build();

        User saved = userRepository.save(officer);
        log.info("Created field officer id: {} for department: {} in zone: {}", saved.getId(), department.getCode(), request.zoneId());
        return UserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listOfficers(Long departmentId) {
        List<User> officers;
        if (departmentId != null) {
            officers = userRepository.findByDepartmentId(departmentId);
        } else {
            officers = userRepository.findByRole(Role.OFFICER);
        }
        return officers.stream().map(UserResponse::fromEntity).toList();
    }
}
