package com.cocroachden.scheduler.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.*;
import org.springframework.lang.NonNull;

import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class ScheduleConstraintProvider implements ConstraintProvider {

    public static final int MAX_SHIFTS_PER_WEEK = 4;
    public static final int MAX_CONSECUTIVE_SHIFTS = 3;

    @Override
    public @NonNull Constraint[] defineConstraints(@NonNull ConstraintFactory factory) {
        return new Constraint[]{
                onlyOneShiftPerDay(factory),
                allShiftsShouldBeAssigned(factory),
                noShiftWhenUnavailable(factory),
                penaltyWhenUndesirable(factory),
                rewardWhenDesirable(factory),
                noMoreThanXShiftInARow(factory),
                avoidSingleShifts(factory),
                noDayShiftsAfterNightShift(factory),
                preferFullWorkWeekends(factory),
                maxShiftCountPerWeek(factory),
                shiftWhenRequired(factory),
//                evenShiftDistribution(factory),
                penalizeDeviationFromIdealShiftCount(factory)
        };
    }

    Constraint onlyOneShiftPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(
                        ShiftAssignment.class,
                        Joiners.equal(ShiftAssignment::getEmployee),
                        Joiners.equal(ShiftAssignment::getDate)
                ).penalize(HardSoftScore.ONE_HARD)
                .asConstraint("One shift per day");
    }

    Constraint allShiftsShouldBeAssigned(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .filter(shiftAssignment -> shiftAssignment.getEmployee() == null)
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("All shifts should be assigned");
    }

    Constraint noShiftWhenUnavailable(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .join(
                                        Availability.class,
                                        Joiners.equal(ShiftAssignment::getDate, Availability::date),
                                        Joiners.equal(ShiftAssignment::getEmployee, Availability::employee),
                                        Joiners.equal(ShiftAssignment::getShiftType, Availability::shiftType)
                                ).filter((shiftAssignment, availability) -> availability.type().equals(AvailabilityType.UNAVAILABLE))
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("No shifts when unavailable");
    }

    Constraint shiftWhenRequired(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Availability.class)
                                .filter(availability -> availability.type().equals(AvailabilityType.REQUIRED))
                                .ifNotExists(
                                        ShiftAssignment.class,
                                        Joiners.equal(Availability::employee, ShiftAssignment::getEmployee),
                                        Joiners.equal(Availability::date, ShiftAssignment::getDate),
                                        Joiners.equal(Availability::shiftType, ShiftAssignment::getShiftType)
                                ).penalize(HardSoftScore.ONE_HARD, value -> 100)
                                .asConstraint("Shift when required");
    }

    Constraint penaltyWhenUndesirable(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .join(
                                        Availability.class,
                                        Joiners.equal(ShiftAssignment::getDate, Availability::date),
                                        Joiners.equal(ShiftAssignment::getEmployee, Availability::employee),
                                        Joiners.equal(ShiftAssignment::getShiftType, Availability::shiftType)
                                ).filter((shiftAssignment, availability) -> availability.type().equals(AvailabilityType.UNDESIRED))
                                .penalize(HardSoftScore.ONE_SOFT)
                                .asConstraint("Penalize undesirable shifts");
    }

    Constraint rewardWhenDesirable(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .join(
                                        Availability.class,
                                        Joiners.equal(ShiftAssignment::getDate, Availability::date),
                                        Joiners.equal(ShiftAssignment::getEmployee, Availability::employee),
                                        Joiners.equal(ShiftAssignment::getShiftType, Availability::shiftType)
                                ).filter((shiftAssignment, availability) -> availability.type().equals(AvailabilityType.DESIRED))
                                .reward(HardSoftScore.ONE_SOFT)
                                .asConstraint("Reward desirable shifts");
    }

    Constraint noMoreThanXShiftInARow(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .filter(shiftAssignment -> shiftAssignment.getEmployee() != null)
                                .filter(shiftAssignment -> shiftAssignment.getConsecutiveShiftAssignmentCount() > MAX_CONSECUTIVE_SHIFTS)
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("No more than " + MAX_CONSECUTIVE_SHIFTS + " shifts in a row");
    }

    Constraint avoidSingleShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .filter(shiftAssignment -> shiftAssignment.getEmployee() != null)
                                .filter(shiftAssignment -> shiftAssignment.getConsecutiveShiftAssignmentCount() == 1)
                                .penalize(HardSoftScore.ofSoft(5))
                                .asConstraint("Avoid single shifts");
    }

    Constraint noDayShiftsAfterNightShift(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(
                        ShiftAssignment.class,
                        Joiners.equal(ShiftAssignment::getEmployee),
                        Joiners.lessThan(ShiftAssignment::getDate)
                ).filter((first, second) -> first.getDate().plusDays(1).equals(second.getDate()))
                .filter((first, second) -> first.getShiftType() == ShiftType.NIGHT && second.getShiftType() == ShiftType.DAY)
                .penalize(HardSoftScore.ONE_HARD, (first, second) -> 50)
                .asConstraint("No day shifts after night shifts");
    }

    Constraint preferFullWorkWeekends(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(
                        ShiftAssignment.class,
                        Joiners.equal(ShiftAssignment::getEmployee),
                        Joiners.filtering((a, b) -> a.getDate().plusDays(1).equals(b.getDate())
                                && a.getDate().getDayOfWeek() == DayOfWeek.SATURDAY)

                ).reward(HardSoftScore.ofSoft(5))
                .asConstraint("Strongly prefer full work weekends");
    }

    Constraint maxShiftCountPerWeek(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(ShiftAssignment.class)
                                .groupBy(
                                        ShiftAssignment::getEmployee,
                                        shiftAssignment -> shiftAssignment.getDate().get(WeekFields.of(Locale.GERMANY).weekOfYear()),
                                        ConstraintCollectors.count()
                                ).filter((employee, week, count) -> count > MAX_SHIFTS_PER_WEEK)
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("Max shift count per week");
    }

//    Constraint evenShiftDistribution(ConstraintFactory constraintFactory) {
//        return constraintFactory.forEach(Employee.class)
//                .join(ShiftAssignment.class, Joiners.equal(e -> e, ShiftAssignment::getEmployee))
//                .groupBy((employee, shiftAssignment) -> employee, ConstraintCollectors.countBi())
//
//
//        return constraintFactory.forEach(ShiftAssignment.class)
//                                .filter(shiftAssignment -> shiftAssignment.getEmployee() != null)
//                                .groupBy(ShiftAssignment::getEmployee, ConstraintCollectors.count())
//                                .groupBy((employee, count) -> count, ConstraintCollectors.average((employee, count) -> count))
//                                .penalize(HardSoftScore.ONE_SOFT, (count, average) -> (int) Math.abs(count - average))
//                                .asConstraint("Equal distribution");
//    }

    Constraint penalizeDeviationFromIdealShiftCount(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Employee.class)
                                .penalize(
                                        HardSoftScore.ONE_SOFT,
                                        (employee) -> (int) Math.round(Math.pow(employee.getIdealShiftCount() - employee.getAssignmentCount(), 2))
                                ).asConstraint("Penalize deviation from ideal shift count");
    }

    //preferred colleague
}


