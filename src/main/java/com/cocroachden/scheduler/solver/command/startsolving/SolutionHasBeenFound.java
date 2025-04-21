package com.cocroachden.scheduler.solver.command.startsolving;

import com.cocroachden.scheduler.domain.Event;
import com.cocroachden.scheduler.solver.EmployeeSchedule;

public record SolutionHasBeenFound(EmployeeSchedule schedule) implements Event {
}
