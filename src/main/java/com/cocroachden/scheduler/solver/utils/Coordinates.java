package com.cocroachden.scheduler.solver.utils;

public record Coordinates(Integer row, Integer column) {
    public static Coordinates of(Integer row, Integer column) {
        return new Coordinates(row, column);
    }
}
