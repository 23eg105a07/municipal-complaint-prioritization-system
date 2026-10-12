package com.municipal.system.user.repository;

import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = 'OFFICER' AND u.department.id = :departmentId AND u.zoneId = :zoneId AND u.available = true")
    List<User> findAvailableOfficersInZone(@Param("departmentId") Long departmentId, @Param("zoneId") Integer zoneId);

    List<User> findByDepartmentId(Long departmentId);
}
