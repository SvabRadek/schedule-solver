package com.cocroachden.scheduler.solver.command.stopsolving;

import com.cocroachden.scheduler.domain.SolvingId;

public record SolvingHasStopped(SolvingId solvingId) {
}
