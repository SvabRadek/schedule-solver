package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import com.cocroachden.scheduler.domain.AvailabilityId;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record Availability(
        @PlanningId AvailabilityId id,
        Employee employee,
        LocalDate date,
        ShiftType shiftType,
        AvailabilityType type
) {
    public static Availability of(
            Employee employee,
            LocalDate date,
            ShiftType shiftType,
            AvailabilityType type
    ) {
        return new Availability(
                new AvailabilityId(employee.getEmployeeId().id() + date.toString() + shiftType.getSymbol()),
                employee,
                date,
                shiftType,
                type
        );
    }

    public String getSymbol() {
        return type.getSymbol() + shiftType.getSymbol();
    }

}
