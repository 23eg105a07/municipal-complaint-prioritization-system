package com.municipal.system.complaint.repository;

import com.municipal.system.complaint.entity.ComplaintUpvote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintUpvoteRepository extends JpaRepository<ComplaintUpvote, Long> {

    boolean existsByComplaintIdAndCitizenId(Long complaintId, Long citizenId);

    Optional<ComplaintUpvote> findByComplaintIdAndCitizenId(Long complaintId, Long citizenId);

    long countByComplaintId(Long complaintId);
}
