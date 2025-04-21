package com.cocroachden.scheduler.domain;

public record EmployeeId(String id) {
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof EmployeeId other) {
            return this.id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
