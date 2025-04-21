package com.cocroachden.scheduler.domain;

import java.util.Comparator;

public record ShiftAssignmentId(String id) implements Comparable<ShiftAssignmentId> {
    @Override
    public int compareTo(final ShiftAssignmentId o) {
        return Comparator.comparing(ShiftAssignmentId::id).compare(this, o);
    }
}
