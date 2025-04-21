package com.cocroachden.scheduler.solver.policy;

import com.cocroachden.scheduler.solver.command.startsolving.SolutionHasBeenFound;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PrintResultsPolicy {

    @EventListener
    public void on(SolutionHasBeenFound event) {
        log.info("Solution has been found");
        event.schedule().printResults();
    }
}
