package com.cocroachden.scheduler.solver.command.startsolving;

import com.cocroachden.scheduler.domain.Event;
import com.cocroachden.scheduler.domain.SolvingId;

public record SolvingHasStarted(SolvingId solvingId) implements Event {
}
