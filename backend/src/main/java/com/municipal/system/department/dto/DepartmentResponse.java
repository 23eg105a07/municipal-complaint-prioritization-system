package com.municipal.system.department.dto;

import com.municipal.system.department.entity.Department;
import com.municipal.system.department.entity.DepartmentCode;

public record DepartmentResponse(
        Long id,
        DepartmentCode code,
        String name,
        String description,
        Long headOfficerId,
        String headOfficerName
) {
    public static DepartmentResponse fromEntity(Department dept) {
        Long headId = dept.getHeadOfficer() != null ? dept.getHeadOfficer().getId() : null;
        String headName = dept.getHeadOfficer() != null ? dept.getHeadOfficer().getFullName() : null;
        return new DepartmentResponse(
                dept.getId(),
                dept.getCode(),
                dept.getName(),
                dept.getDescription(),
                headId,
                headName
        );
    }
}
