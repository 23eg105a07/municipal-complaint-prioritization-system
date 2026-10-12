package com.municipal.system.complaint;

import com.municipal.system.complaint.dto.UpvoteResponse;
import com.municipal.system.complaint.entity.Complaint;
import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.complaint.entity.ComplaintUpvote;
import com.municipal.system.complaint.repository.ComplaintRepository;
import com.municipal.system.complaint.repository.ComplaintUpvoteRepository;
import com.municipal.system.complaint.service.UpvoteService;
import com.municipal.system.exception.BusinessRuleException;
import com.municipal.system.exception.ConflictException;
import com.municipal.system.prioritization.service.PriorityScoringService;
import com.municipal.system.user.entity.Role;
import com.municipal.system.user.entity.User;
import com.municipal.system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpvoteServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintUpvoteRepository upvoteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PriorityScoringService priorityScoringService;

    @InjectMocks
    private UpvoteService upvoteService;

    private User reporter;
    private User upvoter;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        reporter = User.builder().id(1L).email("citizen1@test.com").role(Role.CITIZEN).build();
        upvoter = User.builder().id(2L).email("citizen2@test.com").role(Role.CITIZEN).build();

        complaint = Complaint.builder()
                .id(100L)
                .title("Massive Pothole")
                .citizen(reporter)
                .status(ComplaintStatus.REPORTED)
                .severity(4)
                .affectedCount(1)
                .priorityScore(new BigDecimal("2.30"))
                .build();
    }

    @Test
    @DisplayName("Successfully upvote complaint increments count and recalculates score")
    void testSuccessfulUpvote() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(upvoteRepository.existsByComplaintIdAndCitizenId(100L, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(upvoter));
        when(priorityScoringService.calculateScore(any(Complaint.class))).thenReturn(new BigDecimal("2.60"));

        UpvoteResponse response = upvoteService.upvoteComplaint(100L, 2L);

        assertThat(response.upvoted()).isTrue();
        assertThat(response.affectedCount()).isEqualTo(2);
        assertThat(complaint.getPriorityScore()).isEqualByComparingTo("2.60");
        verify(upvoteRepository).save(any(ComplaintUpvote.class));
        verify(complaintRepository).incrementAffectedCount(100L);
    }

    @Test
    @DisplayName("Citizen cannot upvote their own complaint")
    void testCannotUpvoteOwnComplaint() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> upvoteService.upvoteComplaint(100L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot upvote your own complaint");
    }

    @Test
    @DisplayName("Cannot upvote closed or resolved complaint")
    void testCannotUpvoteClosedComplaint() {
        complaint.setStatus(ComplaintStatus.CLOSED);
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> upvoteService.upvoteComplaint(100L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot upvote a closed or resolved complaint");
    }

    @Test
    @DisplayName("Duplicate upvote triggers ConflictException")
    void testDuplicateUpvote() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(upvoteRepository.existsByComplaintIdAndCitizenId(100L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> upvoteService.upvoteComplaint(100L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already upvoted");
    }
}
