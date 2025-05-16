package com.cocroachden.scheduler.solver.shell;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import com.cocroachden.scheduler.domain.EmployeeId;
import com.cocroachden.scheduler.domain.SolvingId;
import com.cocroachden.scheduler.domain.Vocabulary;
import com.cocroachden.scheduler.solver.Employee;
import com.cocroachden.scheduler.solver.EmployeeSchedule;
import com.cocroachden.scheduler.solver.command.startsolving.StartSolvingCommand;
import com.cocroachden.scheduler.solver.fixtures.SolverScheduleFixture;
import com.cocroachden.scheduler.solver.query.SolverQuery;
import com.cocroachden.scheduler.solver.utils.ScheduleReader;
import com.cocroachden.scheduler.solver.utils.ScheduleWriter;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.IntStream;

@ShellComponent
@AllArgsConstructor
public class SolverShell {

    private final ApplicationEventPublisher publisher;
    private final SolverScheduleFixture fixture;
    private final SolverQuery solverQuery;
    private final ScheduleReader reader;
    private final ScheduleWriter scheduleWriter;
    private final Vocabulary vocabulary;

    @ShellMethod("solve")
    public String solve() {
        var filename = vocabulary.translateFromEn("Assignment") + ".xlsx";
        var folder = System.getProperty("user.dir");
        var file = new File(Path.of(folder + "/" + filename).toUri());
        if (!file.exists() || file.isDirectory()) {
            return "Expected file %s with problem definition not found in expected folder %s.".formatted(filename, folder);
        }
        var problem = reader.read(file);
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

    @ShellMethod("generate")
    public void generate() {
        var scanner = new Scanner(System.in);
        final var pathname = System.getProperty("user.dir") + "/" + vocabulary.translateFromEn("Assignment") + ".xlsx";
        var file = new File(pathname);
        if (file.exists() && file.isFile()) {
            if (!this.shouldOverwriteFile(pathname, scanner)) {
                return;
            }
        }
        System.out.println(vocabulary.translateFromEn("Please enter start date in format D.M.YY"));
        var startDate = scanner.next();
        System.out.println(vocabulary.translateFromEn("Please enter end date in format D.M.YY"));
        var endDate = scanner.next();
        System.out.println(vocabulary.translateFromEn("Please enter number of employees"));
        var numOfEmployees = scanner.nextInt();
        var formatter = DateTimeFormatter.ofPattern("d.M.yy");
        var parsedStartDate = LocalDate.parse(startDate, formatter);
        var parsedEndDate = LocalDate.parse(endDate, formatter);

        var schedule = new EmployeeSchedule();
        schedule.setStartDate(parsedStartDate);
        schedule.setEndDate(parsedEndDate);

        IntStream.range(0, numOfEmployees)
                .forEach(i -> schedule.getEmployees().add(new Employee(new EmployeeId(vocabulary.translateFromEn("Employee") + i), 10)));
        scheduleWriter.write(schedule, file);
        System.out.println(vocabulary.translateFromEn("Done!"));
    }

    private boolean shouldOverwriteFile(final String pathname, final Scanner scanner) {
        System.out.printf(vocabulary.translateFromEn("File %s already exists. Do you want to overwrite it? Yes/No"), pathname);
        var overwrite = vocabulary.translateToEn(scanner.next());
        if (overwrite.equalsIgnoreCase("yes")) {
            return true;
        } else if (overwrite.equalsIgnoreCase("no")) {
            System.out.println(vocabulary.translateFromEn("Terminating generation."));
            return false;
        } else {
            System.out.println(vocabulary.translateFromEn("Please enter yes or no"));
            return this.shouldOverwriteFile(pathname, scanner);
        }
    }

    @ShellMethod("status")
    public String status(String id) {
        return solverQuery.getSolverStatus(new SolvingId(id)).toString();
    }

    @ShellMethod("benchmark")
    public void benchmark() {
        var benchmarkFactory = PlannerBenchmarkFactory.createFromXmlResource("plannerBenchmarkConfig.xml");
        var resource = ClassLoader.getSystemResource("example_problem.xlsx");
        var problem = reader.read(Path.of(resource.getPath()).toFile());
        var benchmark = benchmarkFactory.buildPlannerBenchmark(problem);
        benchmark.benchmarkAndShowReportInBrowser();
    }
}
