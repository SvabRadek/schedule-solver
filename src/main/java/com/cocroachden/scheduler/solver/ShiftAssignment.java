package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.cocroachden.scheduler.domain.ShiftAssignmentId;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.Objects;

@PlanningEntity
@Accessors(chain = true)
@Setter
@Getter
public class ShiftAssignment {
    //https://docs.timefold.ai/timefold-solver/latest/design-patterns/design-patterns
    @PlanningId
    private ShiftAssignmentId id;
    @PlanningVariable
    private Employee employee;
    private LocalDate date;
    private ShiftType shiftType;

    @ShadowVariable(
            sourceVariableName = "employee",
            variableListenerClass = ConsecutiveShiftAssignmentCountListener.class
    )
    private Integer consecutiveShiftAssignmentCount;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!( o instanceof final ShiftAssignment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return date.toString() + "-" + shiftType.name() + "->" + employee.toString();
    }
}
