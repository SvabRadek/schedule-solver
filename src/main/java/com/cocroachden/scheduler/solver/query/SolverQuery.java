package com.cocroachden.scheduler.solver.query;

import ai.timefold.solver.core.api.solver.SolverStatus;
import com.cocroachden.scheduler.domain.SolvingId;
import com.cocroachden.scheduler.solver.service.SolverService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SolverQuery {

    private final SolverService solverService;

    public SolverStatus getSolverStatus(SolvingId id) {
        return solverService.getSolverStatus(id);
    }
}
