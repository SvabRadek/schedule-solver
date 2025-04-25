package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import org.jspecify.annotations.NonNull;

public class EmployeeShiftAssignmentsListener implements VariableListener<EmployeeSchedule, Employee> {
    @Override
    public void beforeVariableChanged(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final Employee employee) {

    }

    @Override
    public void afterVariableChanged(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final Employee employee) {
        employee.setAssignmentInfo(EmployeeShiftAssignmentInfo.calculate(employee.getShiftAssignments()));
    }

    @Override
    public void beforeEntityAdded(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final Employee employee) {

    }

    @Override
    public void afterEntityAdded(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final Employee employee) {

    }

    @Override
    public void beforeEntityRemoved(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final Employee employee) {

    }

    @Override
    public void afterEntityRemoved(@NonNull final ScoreDirector<EmployeeSchedule> scoreDirector, @NonNull final Employee employee) {

    }
}
