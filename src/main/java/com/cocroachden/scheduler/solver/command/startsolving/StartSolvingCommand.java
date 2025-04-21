package com.cocroachden.scheduler.solver.command.startsolving;

import com.cocroachden.scheduler.domain.Command;
import com.cocroachden.scheduler.domain.SolvingId;
import com.cocroachden.scheduler.solver.EmployeeSchedule;

public record StartSolvingCommand(
        SolvingId id,
        EmployeeSchedule problem
) implements Command {
}
