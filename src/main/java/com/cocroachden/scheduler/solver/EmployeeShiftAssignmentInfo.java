package com.cocroachden.scheduler.solver;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class EmployeeShiftAssignmentInfo {

    private Integer nightShifts;
    private Integer dayShifts;
    private Integer weekendShifts;

    public EmployeeShiftAssignmentInfo(List<ShiftAssignment> shiftAssignments) {
        calculate(shiftAssignments);
    }

    public void calculate(List<ShiftAssignment> shiftAssignments) {
        var dayShifts = 0;
        var nightShifts = 0;
        var weekendShifts = 0;
        for (ShiftAssignment shiftAssignment : shiftAssignments) {
            if (shiftAssignment.getShiftType().equals(ShiftType.NIGHT)) {
                nightShifts++;
            }
            if (shiftAssignment.getShiftType().equals(ShiftType.DAY)) {
                dayShifts++;
            }
            if (shiftAssignment.getDate().getDayOfWeek().getValue() > 5) {
                weekendShifts++;
            }
        }
        this.nightShifts = nightShifts;
        this.dayShifts = dayShifts;
        this.weekendShifts = weekendShifts;
    }

    public Integer getTotalCount() {
        return nightShifts + dayShifts;
    }

}
