package com.municipal.system.complaint.entity;

import com.municipal.system.department.entity.DepartmentCode;
import lombok.Getter;

/**
 * Civic complaint categories.
 * Each category maps deterministically to a municipal DepartmentCode.
 */
@Getter
public enum ComplaintCategory {
    // Roads & Infrastructure
    ROADS(DepartmentCode.ROADS),
    POTHOLE(DepartmentCode.ROADS),
    ROAD_DAMAGE(DepartmentCode.ROADS),
    TRAFFIC_SIGNAL(DepartmentCode.ROADS),
    SIDEWALK_DAMAGE(DepartmentCode.ROADS),

    // Water & Sanitation
    WATER(DepartmentCode.WATER),
    WATER_LEAKAGE(DepartmentCode.WATER),
    SEWAGE_OVERFLOW(DepartmentCode.WATER),
    DRAINAGE_BLOCK(DepartmentCode.WATER),
    CONTAMINATED_WATER(DepartmentCode.WATER),

    // Electricity & Public Lighting
    ELECTRICAL(DepartmentCode.ELECTRICAL),
    STREETLIGHT_OUT(DepartmentCode.ELECTRICAL),
    POWER_OUTAGE(DepartmentCode.ELECTRICAL),
    EXPOSED_WIRING(DepartmentCode.ELECTRICAL),
    TRANSFORMER_ISSUE(DepartmentCode.ELECTRICAL);

    private final DepartmentCode departmentCode;

    ComplaintCategory(DepartmentCode departmentCode) {
        this.departmentCode = departmentCode;
    }
}
