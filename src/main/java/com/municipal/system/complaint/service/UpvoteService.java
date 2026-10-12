package com.municipal.system.complaint.service;

import com.municipal.system.complaint.dto.UpvoteResponse;
import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintUpvote;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.complaint.repository.ComplaintUpvoteRepository;
import com.municipal.system.exception.BusinessRuleException;
import com.municipal.system.exception.ConflictException;
import com.municipal.system.exception.ResourceNotFoundException;
import com.municipal.system.prioritization.service.PriorityScoringService;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpvoteService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintUpvoteRepository upvoteRepository;
    private final UserRepository userRepository;
    private final PriorityScoringService priorityScoringService;

    @Transactional
    @CacheEvict(value = "priorityQueue", allEntries = true)
    public UpvoteResponse upvoteComplaint(Long complaintId, Long citizenId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        if (complaint.getStatus().isTerminal()) {
            throw new BusinessRuleException("Cannot upvote a closed or resolved complaint");
        }

        if (complaint.getCitizen().getId().equals(citizenId)) {
            throw new BusinessRuleException("You cannot upvote your own complaint");
        }

        if (upvoteRepository.existsByComplaintIdAndCitizenId(complaintId, citizenId)) {
            throw new ConflictException("You have already upvoted this complaint");
        }

        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", citizenId));

        ComplaintUpvote upvote = ComplaintUpvote.builder()
                .complaint(complaint)
                .citizen(citizen)
                .build();
        upvoteRepository.save(upvote);

        complaintRepository.incrementAffectedCount(complaintId);
        int newAffectedCount = complaint.getAffectedCount() + 1;
        complaint.setAffectedCount(newAffectedCount);

        // Recalculate priority score with the new affected count
        BigDecimal newScore = priorityScoringService.calculateScore(complaint);
        complaint.setPriorityScore(newScore);
        complaintRepository.save(complaint);

        log.info("Complaint {} upvoted by citizen {}. New affected count: {}, new priority score: {}",
                complaintId, citizenId, newAffectedCount, newScore);

        return new UpvoteResponse(complaintId, newAffectedCount, true, "Complaint successfully upvoted");
    }
}
