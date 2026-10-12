package com.municipal.system.user.dto;

import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        String phone,
        Long departmentId,
        String departmentName,
        Integer zoneId,
        Boolean available,
        Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        Long deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        String deptName = user.getDepartment() != null ? user.getDepartment().getName() : null;
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getPhone(),
                deptId,
                deptName,
                user.getZoneId(),
                user.getAvailable(),
                user.getCreatedAt()
        );
    }
}
