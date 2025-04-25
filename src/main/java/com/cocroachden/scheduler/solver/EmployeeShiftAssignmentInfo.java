package com.cocroachden.scheduler.solver;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
@Getter
public class EmployeeShiftAssignmentInfo {

    private final Integer nightShifts;
    private final Integer dayShifts;
    private final Integer weekendShifts;

    public static EmployeeShiftAssignmentInfo calculate(List<ShiftAssignment> shiftAssignments) {
        var dayShifts = new AtomicInteger(0);
        var nightShifts = new AtomicInteger(0);
        var weekendShifts = new AtomicInteger(0);
        shiftAssignments.forEach(shiftAssignment -> {
            if (shiftAssignment.getShiftType().equals(ShiftType.NIGHT)) {
                nightShifts.incrementAndGet();
            }
            if (shiftAssignment.getShiftType().equals(ShiftType.DAY)) {
                dayShifts.incrementAndGet();
            }
            if (shiftAssignment.getDate().getDayOfWeek().getValue() > 5) {
                weekendShifts.incrementAndGet();
            }
        });
        return new EmployeeShiftAssignmentInfo(
                nightShifts.get(),
                dayShifts.get(),
                weekendShifts.get()
        );
    }

    public Integer getTotalCount() {
        return nightShifts + dayShifts;
    }

}
