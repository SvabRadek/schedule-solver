package com.cocroachden.scheduler.domain;

import java.util.Comparator;

public record AvailabilityId(String id) implements Comparable<AvailabilityId> {
    @Override
    public int compareTo(final AvailabilityId o) {
        return Comparator.comparing(AvailabilityId::id).compare(this, o);
    }
}
