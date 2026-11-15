package com.ignitedev.ignitetestcamera.camera;

public enum Easing {
    LINEAR,
    SMOOTH,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT;

    public double apply(double a) {
        double t = a < 0.0 ? 0.0 : (a > 1.0 ? 1.0 : a);
        return switch (this) {
            case LINEAR -> t;
            case SMOOTH -> t * t * (3.0 - 2.0 * t);
            case EASE_IN -> t * t * t;
            case EASE_OUT -> 1.0 - Math.pow(1.0 - t, 3.0);
            case EASE_IN_OUT -> t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
        };
    }

    public static Easing byName(String name) {
        if (name == null) {
            return SMOOTH;
        }
        return switch (name.trim().toLowerCase()) {
            case "linear", "lin" -> LINEAR;
            case "in", "easein", "ease_in" -> EASE_IN;
            case "out", "easeout", "ease_out" -> EASE_OUT;
            case "inout", "easeinout", "ease_in_out" -> EASE_IN_OUT;
            default -> SMOOTH;
        };
    }
}
