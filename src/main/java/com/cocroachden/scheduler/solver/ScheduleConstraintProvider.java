package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import org.springframework.lang.NonNull;

import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class ScheduleConstraintProvider implements ConstraintProvider {

    public static final int MAX_SHIFTS_PER_WEEK = 5;
    public static final int MAX_CONSECUTIVE_SHIFTS = 3;

    @Override
    public @NonNull Constraint[] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[]{
                requireOnlyOneShiftPerDay(factory),
                requireNoShiftWhenUnavailable(factory),
                requireNoDayShiftsAfterNightShift(factory),
                requireShiftWhenRequired(factory),
                penalizeTooManyConsecutiveShifts(factory),
                penalizeTooManyShiftCountPerWeek(factory),
                penalizeAssignedWhenUndesirable(factory),
                penalizeSingleDayOff(factory),
                penalizeDeviationFromIdealShiftCount(factory),
                penalizeUnequalWeekendDistribution(factory),
                penalizeNightAndDayShiftDisbalance(factory),
                rewardAssignedWhenDesirable(factory),
                rewardFullWorkWeekends(factory),
        };
    }

    Constraint requireOnlyOneShiftPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(
                        ShiftAssignment.class,
                        Joiners.equal(ShiftAssignment::getEmployee),
                        Joiners.equal(ShiftAssignment::getDate)
                ).penalize(HardSoftScore.ofHard(100))
                .asConstraint("One shift per day");
    }

    Constraint requireNoShiftWhenUnavailable(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Availability.class)
                                .filter(availability -> availability.type().equals(AvailabilityType.UNAVAILABLE))
                                .ifExists(
                                        ShiftAssignment.class,
                                        Joiners.equal(Availability::date, ShiftAssignment::getDate),
                                        Joiners.equal(Availability::employee, ShiftAssignment::getEmployee),
                                        Joiners.equal(Availability::shiftType, ShiftAssignment::getShiftType)
                                ).penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("No shifts when unavailable");
    }

    Constraint requireShiftWhenRequired(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Availability.class)
                                .filter(availability -> availability.type().equals(AvailabilityType.REQUIRED))
                                .ifNotExists(
                                        ShiftAssignment.class,
                                        Joiners.equal(Availability::employee, ShiftAssignment::getEmployee),
                                        Joiners.equal(Availability::date, ShiftAssignment::getDate),
                                        Joiners.equal(Availability::shiftType, ShiftAssignment::getShiftType)
                                ).groupBy(Availability::employee, ConstraintCollectors.count())
                                .penalize(HardSoftScore.ONE_HARD, (employee, count) -> count)
                                .asConstraint("Shift when required");
    }

    Constraint penalizeAssignedWhenUndesirable(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Availability.class)
                                .filter(availability -> availability.type().equals(AvailabilityType.UNDESIRED))
                                .ifExists(
                                        ShiftAssignment.class,
                                        Joiners.equal(Availability::date, ShiftAssignment::getDate),
                                        Joiners.equal(Availability::employee, ShiftAssignment::getEmployee),
                                        Joiners.equal(Availability::shiftType, ShiftAssignment::getShiftType)
                                ).penalize(HardSoftScore.ONE_SOFT)
                                .asConstraint("Penalize undesirable shifts");
    }

    Constraint rewardAssignedWhenDesirable(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Availability.class)
                                .filter(availability -> availability.type().equals(AvailabilityType.DESIRED))
                                .ifExists(
                                        ShiftAssignment.class,
                                        Joiners.equal(Availability::date, ShiftAssignment::getDate),
                                        Joiners.equal(Availability::employee, ShiftAssignment::getEmployee),
                                        Joiners.equal(Availability::shiftType, ShiftAssignment::getShiftType)
                                ).reward(HardSoftScore.ONE_SOFT)
                                .asConstraint("Reward desirable shifts");
    }

    Constraint penalizeTooManyConsecutiveShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .filter(shiftAssignment -> shiftAssignment.getEmployee() != null)
                                .groupBy(
                                        ShiftAssignment::getEmployee,
                                        ConstraintCollectors.toConsecutiveSequences(assignment -> assignment.getDate().getDayOfYear())
                                ).flattenLast(SequenceChain::getConsecutiveSequences)
                                .filter((employee, shiftAssignmentIntegerSequence) -> shiftAssignmentIntegerSequence.getCount() > MAX_CONSECUTIVE_SHIFTS)
                                .penalize(HardSoftScore.ONE_HARD, (employee, shiftAssignmentIntegerSequence) -> shiftAssignmentIntegerSequence.getCount())
                                .asConstraint("Penalize too many consecutive shifts");
    }

    Constraint penalizeSingleDayOff(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .filter(shiftAssignment -> shiftAssignment.getEmployee() != null)
                                .groupBy(
                                        ShiftAssignment::getEmployee,
                                        ConstraintCollectors.toConsecutiveSequences(assignment -> assignment.getDate().getDayOfYear())
                                ).flattenLast(SequenceChain::getBreaks)
                                .filter((employee, shiftAssignmentIntegerBreak) -> shiftAssignmentIntegerBreak.getLength() == 2)
                                .penalize(HardSoftScore.ONE_SOFT)
                                .asConstraint("Penalize too short off time");
    }

    Constraint requireNoDayShiftsAfterNightShift(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(
                        ShiftAssignment.class,
                        Joiners.equal(ShiftAssignment::getEmployee),
                        Joiners.lessThan(ShiftAssignment::getDate)
                ).filter((first, second) -> first.getDate().plusDays(1).equals(second.getDate()))
                .filter((first, second) -> first.getShiftType() == ShiftType.NIGHT && second.getShiftType() == ShiftType.DAY)
                .penalize(HardSoftScore.ofHard(100))
                .asConstraint("No day shifts after night shifts");
    }

    Constraint rewardFullWorkWeekends(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(
                        ShiftAssignment.class,
                        Joiners.equal(ShiftAssignment::getEmployee),
                        Joiners.filtering((a, b) -> a.getDate().plusDays(1).equals(b.getDate())
                                && a.getDate().getDayOfWeek() == DayOfWeek.SATURDAY)

                ).reward(HardSoftScore.ofSoft(50))
                .asConstraint("Reward full work weekends");
    }

    Constraint penalizeTooManyShiftCountPerWeek(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .groupBy(
                                        ShiftAssignment::getEmployee,
                                        shiftAssignment -> shiftAssignment.getDate().get(WeekFields.of(Locale.GERMANY).weekOfYear()),
                                        ConstraintCollectors.count()
                                ).filter((employee, week, count) -> count > MAX_SHIFTS_PER_WEEK)
                                .penalize(HardSoftScore.ONE_HARD, (employee, week, count) -> count)
                                .asConstraint("Max shift count per week");
    }

    Constraint penalizeDeviationFromIdealShiftCount(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                                .penalize(
                                        HardSoftScore.ONE_SOFT,
                                        (employee) -> (int) Math.round(Math.pow(
                                                employee.getIdealShiftCount() - employee.getAssignmentInfo().getTotalCount(),
                                                2
                                        ))
                                ).asConstraint("Penalize deviation from ideal shift count");
    }

    Constraint penalizeNightAndDayShiftDisbalance(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                                .map(
                                        employee -> employee,
                                        employee -> {
                                            var days = employee.getAssignmentInfo().getDayShifts();
                                            var nights = employee.getAssignmentInfo().getNightShifts();
                                            return Math.abs(days - nights);
                                        }
                                ).filter((employee, difference) -> difference > 0)
                                .penalize(HardSoftScore.ONE_SOFT, (employee, difference) -> (int) Math.round(Math.pow(difference, 2)))
                                .asConstraint("Penalize unequal shift type distribution");
    }

    //TODO make sure this works as intended
    Constraint penalizeUnequalWeekendDistribution(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                                .groupBy(employee -> employee, ConstraintCollectors.average(e -> e.getAssignmentInfo().getWeekendShifts()))
                                .map(
                                        (employee, average) -> employee,
                                        (employee, average) -> Math.abs(Math.round(employee.getAssignmentInfo().getWeekendShifts() - average))
                                )
                                .filter((employee, deviation) -> deviation > 0)
                                .penalize(
                                        HardSoftScore.ONE_SOFT,
                                        (employee, deviation) -> (int) Math.round(Math.pow(deviation * 2, 2))
                                )
                                .asConstraint("Penalize unequal weekend distribution");
    }
}


