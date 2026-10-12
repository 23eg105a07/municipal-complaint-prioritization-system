package com.municipal.system.department.repository;

import com.municipal.system.department.entity.Department;
import com.municipal.system.department.entity.DepartmentCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCode(DepartmentCode code);

    boolean existsByCode(DepartmentCode code);
}
