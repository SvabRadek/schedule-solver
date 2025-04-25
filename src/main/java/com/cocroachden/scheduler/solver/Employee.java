package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import com.cocroachden.scheduler.domain.EmployeeId;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@PlanningEntity(difficultyWeightFactoryClass = EmployeeDifficultyWeightFactory.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class Employee {
    private EmployeeId employeeId;
    private Integer idealShiftCount;
    @InverseRelationShadowVariable(sourceVariableName = "employee")
    private final List<ShiftAssignment> shiftAssignments = new ArrayList<>();
    @Setter
    @ShadowVariable(
            sourceVariableName = "shiftAssignments",
            variableListenerClass = EmployeeShiftAssignmentsListener.class
    )
    private EmployeeShiftAssignmentInfo assignmentInfo = new EmployeeShiftAssignmentInfo(0, 0, 0);

    public Employee(final EmployeeId employeeId, final Integer idealShiftCount) {
        this.employeeId = employeeId;
        this.idealShiftCount = idealShiftCount;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Employee other) {
            return employeeId.equals(other.employeeId);
        }
        return false;
    }

    @Override
    public String toString() {
        return employeeId.id();
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId.id());
    }

}
