package com.municipal.system.workflow.repository;

import com.municipal.system.workflow.entity.ComplaintStatusLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintStatusLogRepository extends JpaRepository<ComplaintStatusLog, Long> {

    List<ComplaintStatusLog> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);

    List<ComplaintStatusLog> findByComplaintIdOrderByCreatedAtDesc(Long complaintId);
}
