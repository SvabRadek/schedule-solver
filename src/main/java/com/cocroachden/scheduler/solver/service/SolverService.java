package com.cocroachden.scheduler.solver.service;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import com.cocroachden.scheduler.domain.SolvingId;
import com.cocroachden.scheduler.solver.EmployeeSchedule;
import com.cocroachden.scheduler.solver.command.startsolving.SolutionHasBeenFound;
import com.cocroachden.scheduler.solver.command.startsolving.SolvingHasStarted;
import com.cocroachden.scheduler.solver.command.startsolving.StartSolvingCommand;
import com.cocroachden.scheduler.solver.command.stopsolving.SolvingHasStopped;
import com.cocroachden.scheduler.solver.command.stopsolving.StopSolvingCommand;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SolverService {

    private SolverManager<EmployeeSchedule, SolvingId> solverManager;
    private ApplicationEventPublisher publisher;

    @EventListener
    @Async
    public void handle(StartSolvingCommand command) {
        solverManager.solveAndListen(
                command.id(),
                command.problem(),
                result -> publisher.publishEvent(new SolutionHasBeenFound(result))
        );
        publisher.publishEvent(new SolvingHasStarted(command.id()));
    }

    @EventListener
    public SolvingHasStopped handle(StopSolvingCommand command) {
        solverManager.terminateEarly(command.id());
        return new SolvingHasStopped(command.id());
    }

    public SolverStatus getSolverStatus(SolvingId solvingId) {
        return solverManager.getSolverStatus(solvingId);
    }
}
