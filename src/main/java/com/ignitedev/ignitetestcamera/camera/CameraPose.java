package com.ignitedev.ignitetestcamera.camera;

public record CameraPose(double x, double y, double z, float yaw, float pitch, float roll, float fov, boolean hasFov) {

    public static CameraPose of(double x, double y, double z, float yaw, float pitch, float roll) {
        return new CameraPose(x, y, z, yaw, pitch, roll, 0.0f, false);
    }

    public CameraPose withFov(float newFov) {
        return new CameraPose(x, y, z, yaw, pitch, roll, newFov, true);
    }
}
