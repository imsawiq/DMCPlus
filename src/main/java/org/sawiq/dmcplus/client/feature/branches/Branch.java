package org.sawiq.dmcplus.client.feature.branches;

import net.minecraft.util.math.MathHelper;

public enum Branch {
    YELLOW("Жёлтая", Direction.NORTH, "North", 0xFFF2C94C),
    RED("Красная", Direction.EAST, "East", 0xFFFF6B6B),
    GREEN("Зелёная", Direction.SOUTH, "South", 0xFF58D68D),
    BLUE("Синяя", Direction.WEST, "West", 0xFF5DADE2);

    private final String displayName;
    private final Direction direction;
    private final String englishName;
    private final int color;

    Branch(String displayName, Direction direction, String englishName, int color) {
        this.displayName = displayName;
        this.direction = direction;
        this.englishName = englishName;
        this.color = color;
    }

    public String displayName() {
        return this.displayName;
    }

    public Direction direction() {
        return this.direction;
    }

    public String englishName() {
        return this.englishName;
    }

    public int color() {
        return this.color;
    }

    public int axisValue(double x, double z) {
        return (int) Math.round(Math.abs(this.direction.axisValue(x, z)));
    }

    public float normalizedDistance(double x, double z) {
        return (float) MathHelper.clamp(this.axisValue(x, z) / 3000.0D, 0.0D, 1.0D);
    }

    public enum Direction {
        NORTH("Z"),
        EAST("X"),
        SOUTH("Z"),
        WEST("X");

        private final String axisName;

        Direction(String axisName) {
            this.axisName = axisName;
        }

        public String axisName() {
            return this.axisName;
        }

        public double axisValue(double x, double z) {
            return switch (this) {
                case NORTH, SOUTH -> z;
                case EAST, WEST -> x;
            };
        }
    }
}
