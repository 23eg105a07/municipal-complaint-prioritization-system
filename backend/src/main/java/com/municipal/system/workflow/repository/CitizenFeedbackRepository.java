package com.municipal.system.workflow.repository;

import com.municipal.system.workflow.entity.CitizenFeedback;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CitizenFeedbackRepository extends JpaRepository<CitizenFeedback, Long> {

    Optional<CitizenFeedback> findByComplaintId(Long complaintId);

    boolean existsByComplaintId(Long complaintId);

    @Query("SELECT AVG(f.rating) FROM CitizenFeedback f")
    Double findAverageRating();

    @Query("SELECT AVG(f.rating) FROM CitizenFeedback f WHERE f.complaint.department.id = :departmentId")
    Double findAverageRatingByDepartmentId(@Param("departmentId") Long departmentId);
}
