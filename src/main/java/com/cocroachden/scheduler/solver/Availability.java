package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import com.cocroachden.scheduler.domain.AvailabilityId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Builder
public record Availability(
        @PlanningId AvailabilityId id,
        Employee employee,
        LocalDate date,
        ShiftType shiftType,
        AvailabilityType type
) {
}
