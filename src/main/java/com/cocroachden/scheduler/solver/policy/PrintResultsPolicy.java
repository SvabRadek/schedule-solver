package com.cocroachden.scheduler.solver.policy;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import com.cocroachden.scheduler.solver.EmployeeSchedule;
import com.cocroachden.scheduler.solver.command.startsolving.SolutionHasBeenFound;
import com.cocroachden.scheduler.solver.utils.ScheduleWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrintResultsPolicy {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduleWriter writer;
    private final SolutionManager<EmployeeSchedule, HardSoftScore> solutionManager;
    private ScheduledFuture<?> scheduledTask;

    @EventListener
    public void on(SolutionHasBeenFound event) {
        log.info("Solution has been found");
        event.schedule().printResults();
        System.out.println(solutionManager.analyze(event.schedule()).summarize());
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }
        scheduledTask = executor.schedule(() -> {
            var path = Path.of(System.getProperty("user.dir") + "/Vysledek.xlsx");
            log.info("Writing latest solution to file '{}'", path);
            try {
                writer.write(event.schedule(), path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 5, TimeUnit.SECONDS);

    }
}
