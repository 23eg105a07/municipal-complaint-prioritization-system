package com.municipal.system.assignment.repository;

import com.municipal.system.assignment.entity.Assignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Optional<Assignment> findByComplaintIdAndActiveTrue(Long complaintId);

    List<Assignment> findByComplaintIdOrderByAssignedAtDesc(Long complaintId);

    List<Assignment> findByOfficerIdAndActiveTrue(Long officerId);

    List<Assignment> findByOfficerIdOrderByAssignedAtDesc(Long officerId);

    boolean existsByComplaintIdAndOfficerIdAndActiveTrue(Long complaintId, Long officerId);
}
