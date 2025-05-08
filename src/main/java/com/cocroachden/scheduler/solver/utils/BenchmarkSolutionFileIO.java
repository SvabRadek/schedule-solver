package com.cocroachden.scheduler.solver.utils;

import ai.timefold.solver.persistence.common.api.domain.solution.SolutionFileIO;
import com.cocroachden.scheduler.solver.EmployeeSchedule;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@AllArgsConstructor
public class BenchmarkSolutionFileIO implements SolutionFileIO<EmployeeSchedule> {

    private final ScheduleReader reader;
    private final ScheduleWriter writer;

    @Override
    public String getInputFileExtension() {
        return "xlsx";
    }

    @Override
    public String getOutputFileExtension() {
        return "xlsx";
    }

    @Override
    public EmployeeSchedule read(final File file) {
        return reader.read(file);
    }

    @Override
    public void write(final EmployeeSchedule employeeSchedule, final File file) {
        writer.write(employeeSchedule, file);
    }
}
