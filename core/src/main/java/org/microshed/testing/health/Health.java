package org.microshed.testing.health;

import java.util.ArrayList;
import java.util.List;

public class Health {

    public Status status;
    public List<Check> checks = new ArrayList<>();

    public Check getCheck(String name) {
        return checks.stream()
                .filter(c -> c.name.equalsIgnoreCase(name))
                .findAny().orElse(null);
    }

    public static class Check {
        public String name;
        public Status status;
    }

    public enum Status {
        UP, DOWN;
    }
}
