package com.ignitedev.ignitetestcamera.camera;

/** One path keyframe. {@code t} is milliseconds from path start. */
public record Keyframe(double t, double x, double y, double z,
                       float yaw, float pitch, float roll,
                       float fov, boolean hasFov, Easing ease) {
}
