package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;

public class EmployeeDifficultyWeightFactory implements SelectionSorterWeightFactory<EmployeeSchedule, Employee> {
    @Override
    public Comparable<Integer> createSorterWeight(final EmployeeSchedule schedule, final Employee selection) {
        return (int) schedule.getAvailabilities().stream()
                             .filter(a -> a.employee().equals(selection))
                             .count();
    }
}
