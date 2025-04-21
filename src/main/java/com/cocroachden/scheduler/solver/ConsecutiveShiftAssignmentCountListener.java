package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;

import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsecutiveShiftAssignmentCountListener implements VariableListener<EmployeeSchedule, ShiftAssignment> {
    @Override
    public void beforeVariableChanged(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final ShiftAssignment shiftAssignment) {

    }

    @Override
    public void afterVariableChanged(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final ShiftAssignment shiftAssignment) {
        this.updateConsecutiveAssignmentCount(scoreDirector, shiftAssignment);
    }

    @Override
    public void beforeEntityAdded(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final ShiftAssignment shiftAssignment) {

    }

    @Override
    public void afterEntityAdded(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final ShiftAssignment shiftAssignment) {
        this.updateConsecutiveAssignmentCount(scoreDirector, shiftAssignment);
    }

    @Override
    public void beforeEntityRemoved(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final ShiftAssignment shiftAssignment) {

    }

    @Override
    public void afterEntityRemoved(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final ShiftAssignment shiftAssignment) {
    }

    private void updateConsecutiveAssignmentCount(final ScoreDirector<EmployeeSchedule> scoreDirector, final ShiftAssignment shiftAssignment) {
        var consecutiveShitAssignmentCount = 1;
        var assignmentDate = shiftAssignment.getDate();
        var employeesShifts = scoreDirector.getWorkingSolution().getShiftAssignments().stream()
                                           .filter(sa -> sa.getEmployee() != null)
                                           .filter(sa -> sa.getEmployee().equals(shiftAssignment.getEmployee()))
                                           .collect(
                                                   Collectors.toMap(
                                                           ShiftAssignment::getDate,
                                                           Function.identity(),
                                                           (shiftAssignment1, shiftAssignment2) -> shiftAssignment1
                                                   ));
        int distance = 1;
        while (true) {
            var followingAssignment = employeesShifts.get(assignmentDate.plusDays(distance));
            if (followingAssignment == null) {
                break;
            }
            consecutiveShitAssignmentCount++;
            distance++;
        }
        distance = 1;
        while (true) {
            var previousAssignment = employeesShifts.get(assignmentDate.minusDays(distance));
            if (previousAssignment == null) {
                break;
            }
            consecutiveShitAssignmentCount++;
            distance++;
        }
        shiftAssignment.setConsecutiveShiftAssignmentCount(consecutiveShitAssignmentCount);
    }
}
