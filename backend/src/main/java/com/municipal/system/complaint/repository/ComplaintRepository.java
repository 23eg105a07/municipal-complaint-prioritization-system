package com.municipal.system.complaint.repository;

import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintCategory;
import com.municipal.system.complaint.entity.ComplaintStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    Page<Complaint> findByCitizenId(Long citizenId, Pageable pageable);

    Page<Complaint> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<Complaint> findByZoneId(Integer zoneId, Pageable pageable);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    List<Complaint> findByStatusNotIn(Collection<ComplaintStatus> statuses);

    List<Complaint> findByStatusNotInAndIsEscalatedFalse(Collection<ComplaintStatus> statuses);

    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN :closedStatuses ORDER BY c.priorityScore DESC, c.createdAt ASC, c.id ASC")
    Page<Complaint> findActivePriorityQueue(@Param("closedStatuses") Collection<ComplaintStatus> closedStatuses, Pageable pageable);

    @Modifying
    @Query("UPDATE Complaint c SET c.affectedCount = c.affectedCount + 1 WHERE c.id = :id")
    int incrementAffectedCount(@Param("id") Long id);

    long countByStatus(ComplaintStatus status);

    long countByDepartmentId(Long departmentId);

    long countByZoneId(Integer zoneId);

    long countByPriorityScoreGreaterThanEqual(BigDecimal score);

    long countByIsEscalatedTrue();

    @Query("SELECT c.zoneId, COUNT(c) FROM Complaint c GROUP BY c.zoneId")
    List<Object[]> countComplaintsGroupByZone();

    @Query("SELECT c.department.id, c.department.name, COUNT(c) FROM Complaint c GROUP BY c.department.id, c.department.name")
    List<Object[]> countComplaintsGroupByDepartment();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countComplaintsGroupByStatus();
}
