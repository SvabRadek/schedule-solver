package com.cocroachden.scheduler.solver;

import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import com.cocroachden.scheduler.domain.AvailabilityId;
import com.cocroachden.scheduler.domain.EmployeeId;
import com.cocroachden.scheduler.domain.ShiftAssignmentId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(
        properties = {
                "spring.shell.interactive.enabled=false"
        }
)
@Import(ScheduleConstraintProviderTest.ScheduleConstraintProviderTestConfiguration.class)
class ScheduleConstraintProviderTest {

    public static final Employee EXAMPLE_EMPLOYEE = new Employee(
            new EmployeeId("example_employee_1"),
            10
    );

    @Autowired
    private ConstraintVerifier<ScheduleConstraintProvider, EmployeeSchedule> constraintVerifier;

    @Test
    public void itCanHandleOneShiftPerDayConstraint() {
        var fistShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example1"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now())
                .setEmployee(EXAMPLE_EMPLOYEE);
        var secondShiftSameDay = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example2"))
                .setShiftType(ShiftType.NIGHT)
                .setDate(LocalDate.now())
                .setEmployee(EXAMPLE_EMPLOYEE);

        constraintVerifier.verifyThat(ScheduleConstraintProvider::onlyOneShiftPerDay)
                          .given(fistShift, secondShiftSameDay, EXAMPLE_EMPLOYEE)
                          .penalizesBy(1);
    }

    @Test
    public void itCanHandleNoNightShiftAfterDayShift() {
        var nightShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example1"))
                .setShiftType(ShiftType.NIGHT)
                .setDate(LocalDate.now())
                .setEmployee(EXAMPLE_EMPLOYEE);
        var followingDayShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example2"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now().plusDays(1))
                .setEmployee(EXAMPLE_EMPLOYEE);

        constraintVerifier.verifyThat(ScheduleConstraintProvider::noDayShiftsAfterNightShift)
                          .given(nightShift, followingDayShift, EXAMPLE_EMPLOYEE)
                          .penalizesBy(50);
    }

    @Test
    public void itCanHandleAvoidingSingleShifts() {
        var firstShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example1"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now())
                .setConsecutiveShiftAssignmentCount(2)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var secondShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example2"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now().plusDays(1))
                .setConsecutiveShiftAssignmentCount(2)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var thirdDetachedShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example3"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now().plusDays(3))
                .setConsecutiveShiftAssignmentCount(1)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var schedule = new EmployeeSchedule();
        schedule.setShiftAssignments(new ArrayList<>(List.of(firstShift, secondShift, thirdDetachedShift)));
        schedule.setEmployees(List.of(EXAMPLE_EMPLOYEE));
        schedule.setStartDate(LocalDate.now());
        schedule.setEndDate(LocalDate.now().plusDays(5));
        schedule.setAvailabilities(List.of());
        constraintVerifier.verifyThat(ScheduleConstraintProvider::avoidSingleShifts)
                          .givenSolution(schedule)
                          .penalizesBy(1);
    }

    @Test
    public void itCanHandleNoShiftWhenUnavailable() {
        var assignment = new ShiftAssignment()
                .setId(new ShiftAssignmentId("irrelevant"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now())
                .setConsecutiveShiftAssignmentCount(1)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var availability = Availability.builder()
                                       .id(new AvailabilityId("irrelevant"))
                                       .shiftType(ShiftType.DAY)
                                       .type(AvailabilityType.UNAVAILABLE)
                                       .date(LocalDate.now())
                                       .employee(EXAMPLE_EMPLOYEE)
                                       .build();
        constraintVerifier.verifyThat(ScheduleConstraintProvider::noShiftWhenUnavailable)
                          .given(assignment, availability, EXAMPLE_EMPLOYEE)
                          .penalizesBy(1);
    }

    @Test
    public void itCanHandleShiftWhenRequired() {
        var availability = Availability.builder()
                                       .id(new AvailabilityId("irrelevant"))
                                       .shiftType(ShiftType.DAY)
                                       .type(AvailabilityType.REQUIRED)
                                       .date(LocalDate.now())
                                       .employee(EXAMPLE_EMPLOYEE)
                                       .build();
        constraintVerifier.verifyThat(ScheduleConstraintProvider::shiftWhenRequired)
                          .given(availability, EXAMPLE_EMPLOYEE)
                          .penalizesBy(100);
    }

    @Test
    public void itCanHandleNoMoreThanXShiftsInARow() {
        var firstShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example1"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now())
                .setConsecutiveShiftAssignmentCount(4)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var secondShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example2"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now().plusDays(1))
                .setConsecutiveShiftAssignmentCount(4)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var thirdShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example3"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now().plusDays(2))
                .setConsecutiveShiftAssignmentCount(4)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var fourthShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example4"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now().plusDays(3))
                .setConsecutiveShiftAssignmentCount(4)
                .setEmployee(EXAMPLE_EMPLOYEE);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::noMoreThanXShiftInARow)
                          .given(firstShift, secondShift, thirdShift, fourthShift, EXAMPLE_EMPLOYEE)
                          .penalizesBy(4);
    }

    @Test
    public void itCanHandleMaxShiftCountPerWeek() {
        var monday = LocalDate.of(2025, 4, 21);
        var firstShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example1"))
                .setShiftType(ShiftType.DAY)
                .setDate(monday)
                .setConsecutiveShiftAssignmentCount(5)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var secondShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example2"))
                .setShiftType(ShiftType.DAY)
                .setDate(monday.plusDays(1))
                .setConsecutiveShiftAssignmentCount(5)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var thirdShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example3"))
                .setShiftType(ShiftType.DAY)
                .setDate(monday.plusDays(2))
                .setConsecutiveShiftAssignmentCount(5)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var fourthShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example4"))
                .setShiftType(ShiftType.DAY)
                .setDate(monday.plusDays(3))
                .setConsecutiveShiftAssignmentCount(5)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var fifthShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example5"))
                .setShiftType(ShiftType.DAY)
                .setDate(monday.plusDays(4))
                .setConsecutiveShiftAssignmentCount(5)
                .setEmployee(EXAMPLE_EMPLOYEE);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::maxShiftCountPerWeek)
                          .given(firstShift, secondShift, thirdShift, fourthShift, fifthShift, EXAMPLE_EMPLOYEE)
                          .penalizesBy(1);
    }

    @Test
    public void itCanHandleDeviationFromIdealShiftCount() {
        var firstShift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example1"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now())
                .setConsecutiveShiftAssignmentCount(1)
                .setEmployee(EXAMPLE_EMPLOYEE);
        EXAMPLE_EMPLOYEE.setAssignmentCount(1);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::penalizeDeviationFromIdealShiftCount)
                          .given(firstShift, EXAMPLE_EMPLOYEE)
                          .penalizesBy(81);
    }

    @Test
    public void itCanHandleDeviationFromIdealShiftCountWhenNoShiftsAreAssigned() {
        EXAMPLE_EMPLOYEE.setAssignmentCount(0);
        constraintVerifier.verifyThat(ScheduleConstraintProvider::penalizeDeviationFromIdealShiftCount)
                          .given(EXAMPLE_EMPLOYEE)
                          .penalizesBy(100);
    }

    @Test
    public void itCanHandleShiftWhenUndesired() {
        var shift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example1"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now())
                .setConsecutiveShiftAssignmentCount(1)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var availability = Availability.builder()
                                       .employee(EXAMPLE_EMPLOYEE)
                                       .id(new AvailabilityId("example2"))
                                       .shiftType(ShiftType.DAY)
                                       .type(AvailabilityType.UNDESIRED)
                                       .date(LocalDate.now())
                                       .build();
        constraintVerifier.verifyThat(ScheduleConstraintProvider::penaltyWhenUndesirable)
                          .given(shift, availability, EXAMPLE_EMPLOYEE)
                          .penalizesBy(1);
    }

    @Test
    public void itCanHandleShiftWhenDesirable() {
        var shift = new ShiftAssignment()
                .setId(new ShiftAssignmentId("example1"))
                .setShiftType(ShiftType.DAY)
                .setDate(LocalDate.now())
                .setConsecutiveShiftAssignmentCount(1)
                .setEmployee(EXAMPLE_EMPLOYEE);
        var availability = Availability.builder()
                                       .employee(EXAMPLE_EMPLOYEE)
                                       .id(new AvailabilityId("example2"))
                                       .shiftType(ShiftType.DAY)
                                       .type(AvailabilityType.DESIRED)
                                       .date(LocalDate.now())
                                       .build();
        constraintVerifier.verifyThat(ScheduleConstraintProvider::rewardWhenDesirable)
                          .given(shift, availability, EXAMPLE_EMPLOYEE)
                          .rewardsWith(1);
    }

//    @Test
//    public void itCanHandleShiftWhenDesirable() {
//        var shift = new ShiftAssignment()
//                .setId(new ShiftAssignmentId("example1"))
//                .setShiftType(ShiftType.DAY)
//                .setDate(LocalDate.now())
//                .setConsecutiveShiftAssignmentCount(1)
//                .setEmployee(EXAMPLE_EMPLOYEE);
//        var availability = Availability.builder()
//                                       .employee(EXAMPLE_EMPLOYEE)
//                                       .id(new AvailabilityId("example2"))
//                                       .shiftType(ShiftType.DAY)
//                                       .type(AvailabilityType.DESIRED)
//                                       .date(LocalDate.now())
//                                       .build();
//        constraintVerifier.verifyThat(ScheduleConstraintProvider::evenShiftDistribution)
//                          .given(shift, availability, EXAMPLE_EMPLOYEE)
//                          .rewardsWith(1);
//    }


    @TestConfiguration
    public static class ScheduleConstraintProviderTestConfiguration {
        @Bean
        public ConstraintVerifier<ScheduleConstraintProvider, EmployeeSchedule> constraintVerifier() {
            return ConstraintVerifier.build(
                    new ScheduleConstraintProvider(),
                    EmployeeSchedule.class,
                    ShiftAssignment.class,
                    Employee.class
            );
        }
    }

}