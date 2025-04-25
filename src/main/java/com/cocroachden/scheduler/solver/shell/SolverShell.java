package com.cocroachden.scheduler.solver.shell;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import com.cocroachden.scheduler.domain.SolvingId;
import com.cocroachden.scheduler.solver.command.startsolving.StartSolvingCommand;
import com.cocroachden.scheduler.solver.fixtures.SolverScheduleFixture;
import com.cocroachden.scheduler.solver.query.SolverQuery;
import com.cocroachden.scheduler.solver.utils.ScheduleParser;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@ShellComponent
@AllArgsConstructor
public class SolverShell {

    private final ApplicationEventPublisher publisher;
    private final SolverScheduleFixture fixture;
    private final SolverQuery solverQuery;
    private final ScheduleParser converter;

    @ShellMethod("Start")
    public String start() {
        var id = UUID.randomUUID();
        publisher.publishEvent(
                new StartSolvingCommand(
                        new SolvingId(id.toString()),
                        fixture.generateEmployeeSchedule()
                )
        );
        return id.toString();
    }

    @ShellMethod("Solve")
    public String solve() throws IOException {
        var filename = "Rozvrh.xlsx";
        var problem = converter.read(Path.of(System.getProperty("user.dir") + "/" + filename).toFile());
        var id = UUID.randomUUID();
        publisher.publishEvent(
                new StartSolvingCommand(
                        new SolvingId(id.toString()),
                        problem
                )
        );
        return id.toString();
    }

    @ShellMethod("Status")
    public String status(String id) {
        return solverQuery.getSolverStatus(new SolvingId(id)).toString();
    }

    @ShellMethod("Benchmark")
    public void benchmark() {
        var benchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource("plannerBenchmarkConfig.xml");
        var resource = ClassLoader.getSystemResource("example_problem.xlsx");
        var problem = converter.read(Path.of(resource.getPath()).toFile());
        var benchmark = benchmarkFactory.buildPlannerBenchmark(problem);
        benchmark.benchmarkAndShowReportInBrowser();
    }
}
