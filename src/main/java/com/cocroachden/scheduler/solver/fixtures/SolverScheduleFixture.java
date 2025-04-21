package com.cocroachden.scheduler.solver.fixtures;

import com.cocroachden.scheduler.domain.AvailabilityId;
import com.cocroachden.scheduler.domain.EmployeeId;
import com.cocroachden.scheduler.domain.ShiftAssignmentId;
import com.cocroachden.scheduler.solver.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class SolverScheduleFixture {

    private final String[] employeeNames = {
            "Jana", "Petra", "Milena", "Anna", "Alena", "Bohumila", "Dana", "Marketa", "Lucie", "Karla", "Kamila", "Stepanka", "Marie", "Vera"
    };

    public EmployeeSchedule generateEmployeeSchedule() {
        final var startDate = LocalDate.now();
        final var scheduleLength = 30;
        final var endDate = LocalDate.now().plusDays(scheduleLength);
        final var employees = this.generateEmployees();
        final var availabilities = new ArrayList<Availability>();
        final var shiftAssignments = new ArrayList<ShiftAssignment>();
        startDate.datesUntil(endDate.plusDays(1))
                 .forEach(date -> employees.forEach(e -> generateAvailability(e, date).ifPresent(availabilities::add)));
        startDate.datesUntil(endDate.plusDays(1))
                 .forEach(date -> {
                     for (int i = 0; i < 3; i++) {
                         shiftAssignments.add(
                                 new ShiftAssignment()
                                         .setId(new ShiftAssignmentId(UUID.randomUUID().toString()))
                                         .setDate(date)
                                         .setShiftType(ShiftType.DAY)
                         );
                     }
                     for (int i = 0; i < 2; i++) {
                         shiftAssignments.add(
                                 new ShiftAssignment()
                                         .setId(new ShiftAssignmentId(UUID.randomUUID().toString()))
                                         .setDate(date)
                                         .setShiftType(ShiftType.NIGHT)
                         );
                     }
                 });

        final var employeeSchedule = new EmployeeSchedule();
        employeeSchedule.setEmployees(employees);
        employeeSchedule.setAvailabilities(availabilities);
        employeeSchedule.setShiftAssignments(shiftAssignments);
        return employeeSchedule;
    }

    private List<Employee> generateEmployees() {
        return Arrays.stream(employeeNames)
                     .map(name -> new Employee(new EmployeeId(name), 15))
                     .toList();
    }

    private Optional<Availability> generateAvailability(
            Employee employee,
            LocalDate date
    ) {
        var shouldContinue = Math.random() < 0.2;
        if (!shouldContinue) {
            return Optional.empty();
        }
        var index = (int) Math.round(Math.random() * 3);
        if (index > 1) {
            //make index 2 and 3 (unavailable, required) most rare
            index = (int) Math.round((Math.random() * 3));
        }
        return Optional.of(new Availability(
                new AvailabilityId(employee.getEmployeeId().id() + date.toString()),
                employee,
                date,
                Math.random() < 0.5 ? ShiftType.NIGHT : ShiftType.DAY,
                AvailabilityType.values()[index]
        ));
    }

}
