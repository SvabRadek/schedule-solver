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

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

@ShellComponent
@AllArgsConstructor
public class SolverShell {

    private final ApplicationEventPublisher publisher;
    private final SolverScheduleFixture fixture;
    private final SolverQuery solverQuery;
    private final ScheduleParser converter;

    @ShellMethod("solve")
    public String solve() {
        var filename = "Rozvrh.xlsx";
        var folder = System.getProperty("user.dir");
        var file = new File(Path.of(folder + "/" + filename).toUri());
        if (!file.exists() || file.isDirectory()) {
            return "Expected file %s with problem definition not found in expected folder %s.".formatted(filename, folder);
        }
        var problem = converter.read(file);
        var id = UUID.randomUUID();
        publisher.publishEvent(
                new StartSolvingCommand(
                        new SolvingId(id.toString()),
                        problem
                )
        );
        return id.toString();
    }

    @ShellMethod("fixture")
    public String fixture() {
        var id = UUID.randomUUID();
        publisher.publishEvent(
                new StartSolvingCommand(
                        new SolvingId(id.toString()),
                        fixture.generateEmployeeSchedule()
                )
        );
        return id.toString();
    }

    @ShellMethod("status")
    public String status(String id) {
        return solverQuery.getSolverStatus(new SolvingId(id)).toString();
    }

    @ShellMethod("benchmark")
    public void benchmark() {
        var benchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource("plannerBenchmarkConfig.xml");
        var resource = ClassLoader.getSystemResource("example_problem.xlsx");
        var problem = converter.read(Path.of(resource.getPath()).toFile());
        var benchmark = benchmarkFactory.buildPlannerBenchmark(problem);
        benchmark.benchmarkAndShowReportInBrowser();
    }
}
