package com.municipal.system.workflow;

import com.municipal.system.complaint.entity.ComplaintStatus;
import com.municipal.system.exception.BusinessRuleException;
import com.municipal.system.workflow.service.WorkflowTransitionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowTransitionPolicyTest {

    private WorkflowTransitionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new WorkflowTransitionPolicy();
    }

    @Test
    @DisplayName("Valid status transitions succeed")
    void testValidTransitions() {
        assertThat(policy.isValidTransition(ComplaintStatus.REPORTED, ComplaintStatus.VERIFIED)).isTrue();
        assertThat(policy.isValidTransition(ComplaintStatus.REPORTED, ComplaintStatus.ASSIGNED)).isTrue();
        assertThat(policy.isValidTransition(ComplaintStatus.REPORTED, ComplaintStatus.REJECTED)).isTrue();

        assertThat(policy.isValidTransition(ComplaintStatus.VERIFIED, ComplaintStatus.ASSIGNED)).isTrue();
        assertThat(policy.isValidTransition(ComplaintStatus.VERIFIED, ComplaintStatus.IN_PROGRESS)).isTrue();

        assertThat(policy.isValidTransition(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS)).isTrue();
        assertThat(policy.isValidTransition(ComplaintStatus.ASSIGNED, ComplaintStatus.RESOLVED)).isTrue();

        assertThat(policy.isValidTransition(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED)).isTrue();

        assertThat(policy.isValidTransition(ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED)).isTrue();
        assertThat(policy.isValidTransition(ComplaintStatus.RESOLVED, ComplaintStatus.IN_PROGRESS)).isTrue();

        assertThatCode(() -> policy.validateTransition(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Invalid status transitions throw BusinessRuleException")
    void testInvalidTransitions() {
        assertThat(policy.isValidTransition(ComplaintStatus.REPORTED, ComplaintStatus.CLOSED)).isFalse();
        assertThat(policy.isValidTransition(ComplaintStatus.CLOSED, ComplaintStatus.IN_PROGRESS)).isFalse();
        assertThat(policy.isValidTransition(ComplaintStatus.REPORTED, ComplaintStatus.REPORTED)).isFalse();

        assertThatThrownBy(() -> policy.validateTransition(ComplaintStatus.REPORTED, ComplaintStatus.CLOSED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid status transition");

        assertThatThrownBy(() -> policy.validateTransition(ComplaintStatus.CLOSED, ComplaintStatus.REPORTED))
                .isInstanceOf(BusinessRuleException.class);
    }
}
