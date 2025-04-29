package com.cocroachden.scheduler.solver.policy;

import ai.timefold.solver.core.api.solver.SolverStatus;
import com.cocroachden.scheduler.domain.SolvingId;
import com.cocroachden.scheduler.solver.command.startsolving.SolvingHasStarted;
import com.cocroachden.scheduler.solver.command.stopsolving.SolvingHasStopped;
import com.cocroachden.scheduler.solver.command.stopsolving.StopSolvingCommand;
import com.cocroachden.scheduler.solver.query.SolverQuery;
import com.cocroachden.scheduler.system.time.service.SecondHasPassed;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@AllArgsConstructor
public class CheckForTerminatedSolutions {

    private final SolverQuery query;
    private final Set<SolvingId> activeRuns = new HashSet<>();

    @EventListener
    public void on(SolvingHasStarted event) {
        activeRuns.add(event.solvingId());
    }

    @EventListener
    public void on(SolvingHasStopped event) {
        activeRuns.remove(event.solvingId());
    }

    @EventListener
    public List<StopSolvingCommand> on(SecondHasPassed event) {
        return activeRuns.stream()
                         .map(solvingId -> {
                             var status = query.getSolverStatus(solvingId);
                             if (!status.equals(SolverStatus.NOT_SOLVING)) {
                                 return null;
                             }
                             return new StopSolvingCommand(solvingId);
                         }).filter(Objects::nonNull)
                         .toList();
    }
}
