package com.cocroachden.scheduler.solver.service;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.cocroachden.scheduler.domain.SolvingId;
import com.cocroachden.scheduler.solver.AvailabilityType;
import com.cocroachden.scheduler.solver.EmployeeSchedule;
import com.cocroachden.scheduler.solver.EmployeeShiftAssignmentInfo;
import com.cocroachden.scheduler.solver.command.startsolving.SolutionHasBeenFound;
import com.cocroachden.scheduler.solver.command.startsolving.SolvingHasStarted;
import com.cocroachden.scheduler.solver.command.startsolving.StartSolvingCommand;
import com.cocroachden.scheduler.solver.command.stopsolving.SolvingHasStopped;
import com.cocroachden.scheduler.solver.command.stopsolving.StopSolvingCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SolverService {

    private final SolverManager<EmployeeSchedule, SolvingId> solverManager;
    private final ApplicationEventPublisher publisher;
    private final Set<SolvingId> runningProblems = Collections.synchronizedSet(new HashSet<>());

    @EventListener
    @Async
    public void handle(StartSolvingCommand command) {
//        this.preAssignRequiredShifts(command.problem());
        solverManager.solveAndListen(
                command.id(),
                command.problem(),
                result -> publisher.publishEvent(new SolutionHasBeenFound(result))
        );
        runningProblems.add(command.id());
        publisher.publishEvent(new SolvingHasStarted(command.id()));
    }

    @EventListener
    public SolvingHasStopped handle(StopSolvingCommand command) {
        solverManager.terminateEarly(command.id());
        runningProblems.remove(command.id());
        return new SolvingHasStopped(command.id());
    }

    public SolverStatus getSolverStatus(SolvingId solvingId) {
        return solverManager.getSolverStatus(solvingId);
    }

    private void preAssignRequiredShifts(final EmployeeSchedule problem) {
        problem.getAvailabilities().stream()
               .filter(a -> a.type().equals(AvailabilityType.REQUIRED))
               .forEach(a -> {
                   problem.getShiftAssignments().stream()
                          .filter(sa -> sa.getEmployee() == null)
                          .filter(sa -> sa.getDate().equals(a.date()))
                          .filter(sa -> sa.getShiftType().equals(a.shiftType()))
                          .findAny()
                          .ifPresent(sa -> {
                              sa.setEmployee(a.employee());
                              a.employee().getShiftAssignments().add(sa);
                              a.employee().setAssignmentInfo(new EmployeeShiftAssignmentInfo(a.employee().getShiftAssignments()));
                          });
               });
    }
}
