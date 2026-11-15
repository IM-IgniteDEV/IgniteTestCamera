package com.ignitedev.ignitetestcamera.camera;

public final class Interpolations {

    private Interpolations() {
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static float lerp(float a, float b, double t) {
        return (float) (a + (b - a) * t);
    }

    /** Shortest-path angle interpolation in degrees. */
    public static float lerpAngle(float a, float b, double t) {
        float delta = ((b - a) % 360.0f + 540.0f) % 360.0f - 180.0f;
        return a + delta * (float) t;
    }

    /** Uniform Catmull-Rom spline through p1 and p2, tangents from p0 and p3. */
    public static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1)
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }
}
