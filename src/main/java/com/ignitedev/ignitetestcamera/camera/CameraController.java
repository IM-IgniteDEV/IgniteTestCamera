package com.ignitedev.ignitetestcamera.camera;

/**
 * Holds the active camera state and samples an interpolated pose on demand.
 * Mutated from the control socket thread, read from the render thread; all shared
 * fields are volatile and swaps are atomic so no locking is needed.
 */
public final class CameraController {

    private volatile boolean active;
    private volatile CameraPose staticPose;
    private volatile Playback path;
    private volatile Float fovOverride;
    private volatile CameraPose lastApplied;
    private volatile boolean hudHideRequested;
    private volatile boolean nametagsHidden;

    private record Playback(Keyframe[] frames, long startNanos, double durationMs, boolean loop) {
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean on) {
        this.active = on;
        if (!on) {
            this.path = null;
            this.staticPose = null;
            this.lastApplied = null;
        }
    }

    public void goToPose(CameraPose pose) {
        this.path = null;
        this.staticPose = pose;
        this.active = true;
    }

    public void playPath(Keyframe[] frames, double durationMs, boolean loop) {
        if (frames == null || frames.length == 0) {
            return;
        }
        this.staticPose = null;
        this.path = new Playback(frames, System.nanoTime(), durationMs, loop);
        this.active = true;
    }

    public void setFovOverride(Float fov) {
        this.fovOverride = fov;
    }

    public Float fovOverride() {
        return fovOverride;
    }

    public Float effectiveFov() {
        Float override = this.fovOverride;
        if (override != null) {
            return override;
        }
        CameraPose applied = this.lastApplied;
        if (active && applied != null && applied.hasFov()) {
            return applied.fov();
        }
        return null;
    }

    public boolean isHudHideRequested() {
        return hudHideRequested;
    }

    public void setHudHideRequested(boolean value) {
        this.hudHideRequested = value;
    }

    public boolean isNametagsHidden() {
        return nametagsHidden;
    }

    public void setNametagsHidden(boolean value) {
        this.nametagsHidden = value;
    }

    public CameraPose lastApplied() {
        return lastApplied;
    }

    public void setLastApplied(CameraPose pose) {
        this.lastApplied = pose;
    }

    public boolean isPlaying() {
        Playback p = this.path;
        if (p == null) {
            return false;
        }
        if (p.loop()) {
            return true;
        }
        double elapsed = (System.nanoTime() - p.startNanos()) / 1_000_000.0;
        return elapsed < p.frames()[p.frames().length - 1].t();
    }

    /** Current pose for this frame, or null when nothing should override the vanilla camera. */
    public CameraPose sample() {
        CameraPose base;
        Playback p = this.path;
        if (p != null) {
            base = samplePlayback(p);
        } else {
            base = this.staticPose;
        }
        if (base == null) {
            return null;
        }
        Float override = this.fovOverride;
        return override != null ? base.withFov(override) : base;
    }

    private CameraPose samplePlayback(Playback p) {
        Keyframe[] f = p.frames();
        int n = f.length;
        double last = f[n - 1].t();
        double elapsed = (System.nanoTime() - p.startNanos()) / 1_000_000.0;
        if (p.loop() && last > 0.0) {
            elapsed %= last;
        }
        if (elapsed <= f[0].t()) {
            return poseOf(f[0]);
        }
        if (elapsed >= last) {
            return poseOf(f[n - 1]);
        }

        int i = 0;
        while (i < n - 1 && f[i + 1].t() < elapsed) {
            i++;
        }
        Keyframe a = f[i];
        Keyframe b = f[i + 1];
        double span = b.t() - a.t();
        double alpha = span <= 0.0 ? 0.0 : (elapsed - a.t()) / span;
        double e = a.ease().apply(alpha);

        Keyframe p0 = f[Math.max(0, i - 1)];
        Keyframe p3 = f[Math.min(n - 1, i + 2)];

        double x = Interpolations.catmullRom(p0.x(), a.x(), b.x(), p3.x(), e);
        double y = Interpolations.catmullRom(p0.y(), a.y(), b.y(), p3.y(), e);
        double z = Interpolations.catmullRom(p0.z(), a.z(), b.z(), p3.z(), e);
        float yaw = Interpolations.lerpAngle(a.yaw(), b.yaw(), e);
        float pitch = Interpolations.lerp(a.pitch(), b.pitch(), e);
        float roll = Interpolations.lerp(a.roll(), b.roll(), e);

        if (a.hasFov() && b.hasFov()) {
            return new CameraPose(x, y, z, yaw, pitch, roll, Interpolations.lerp(a.fov(), b.fov(), e), true);
        }
        if (a.hasFov()) {
            return new CameraPose(x, y, z, yaw, pitch, roll, a.fov(), true);
        }
        if (b.hasFov()) {
            return new CameraPose(x, y, z, yaw, pitch, roll, b.fov(), true);
        }
        return CameraPose.of(x, y, z, yaw, pitch, roll);
    }

    private static CameraPose poseOf(Keyframe k) {
        return new CameraPose(k.x(), k.y(), k.z(), k.yaw(), k.pitch(), k.roll(), k.fov(), k.hasFov());
    }
}
