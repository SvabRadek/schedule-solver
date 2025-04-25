package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@PlanningSolution
@Setter
@Getter
@NoArgsConstructor
public class EmployeeSchedule {

    private LocalDate startDate;
    private LocalDate endDate;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Employee> employees;

    @ProblemFactCollectionProperty
    private List<Availability> availabilities;

    @PlanningEntityCollectionProperty
    private Set<ShiftAssignment> shiftAssignments = new LinkedHashSet<>();

    @PlanningScore
    private HardSoftScore score;

    public void printResults() {
        System.out.println("Score: " + score.toString());
        var headerFormat = new StringBuilder("%30s ");
        var headerElements = new ArrayList<>();
        headerElements.add("");
        var dates = shiftAssignments.stream()
                                    .map(ShiftAssignment::getDate)
                                    .distinct()
                                    .sorted()
                                    .toList();
        dates.forEach(date -> {
            headerFormat.append("%7s" + "|");
            headerElements.add(date.format(DateTimeFormatter.ofPattern("dd/MM")) + " " + date.getDayOfWeek().getValue());
        });

        System.out.format(headerFormat + "%n", headerElements.toArray());

        employees.forEach(employee -> {
            var lineFormat = new StringBuilder("%-30s:");
            var lineElements = new ArrayList<>();
            lineElements.add(employee.getEmployeeId().id() + " - " + employee.getShiftAssignments().size());
            dates.forEach(date -> {
                var assignment = shiftAssignments.stream()
                                                 .filter(sa -> sa.getEmployee() != null)
                                                 .filter(sa -> sa.getEmployee().equals(employee) && sa.getDate().equals(date))
                                                 .findAny()
                                                 .map(sa -> sa.getShiftType() == ShiftType.DAY ? "D" : "N")
                                                 .orElse("");
                var availabilitySymbol = "";
                var availabilityForDay = availabilities.stream()
                                                       .filter(a -> a.employee().equals(employee) && a.date().equals(date))
                                                       .toList();
                if (availabilityForDay.size() == 2) {
                    availabilitySymbol = "/V";
                } else if (availabilityForDay.size() == 1) {
                    availabilitySymbol = "/" + availabilityForDay.get(0).shiftType().name().charAt(0);
                }
                lineFormat.append("%7s" + "|");
                lineElements.add(assignment + availabilitySymbol);
            });
            System.out.format(lineFormat + "%n", lineElements.toArray());
        });
    }

    @Override
    public String toString() {
        var assignments = shiftAssignments.stream()
                                          .filter(shiftAssignment -> shiftAssignment.getEmployee() != null)
                                          .sorted(Comparator.comparing(ShiftAssignment::getDate))
                                          .map(ShiftAssignment::toString)
                                          .toList();

        var assignmentsAsString = String.join(System.lineSeparator(), assignments);
        return """
                Score: %s
                Assignments:
                %s
                """.formatted(score.toString(), assignmentsAsString);
    }
}
