package com.ignitedev.ignitetestcamera.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ignitedev.ignitetestcamera.camera.CameraController;
import com.ignitedev.ignitetestcamera.camera.CameraPose;
import com.ignitedev.ignitetestcamera.camera.Easing;
import com.ignitedev.ignitetestcamera.camera.Keyframe;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Parses one JSON command line and applies it. State that only the controller owns is written
 * directly (volatile). Anything touching live client state is dispatched to the client thread.
 */
public final class CommandHandler {

    private final CameraController controller;

    public CommandHandler(CameraController controller) {
        this.controller = controller;
    }

    public String handleLine(String line) {
        try {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String cmd = optString(obj, "cmd", "");
            return switch (cmd) {
                case "ping" -> ok().toString();
                case "freecam" -> freecam(obj);
                case "pose" -> pose(obj);
                case "path" -> path(obj);
                case "fov" -> fov(obj);
                case "hud" -> hud(obj);
                case "status" -> status();
                default -> err("unknown cmd: " + cmd);
            };
        } catch (Exception e) {
            return err("bad request: " + e.getMessage());
        }
    }

    private String freecam(JsonObject obj) {
        boolean on = optBool(obj, "on", true);
        controller.setActive(on);
        JsonObject res = ok();
        res.addProperty("active", on);
        return res.toString();
    }

    private String pose(JsonObject obj) {
        double x = reqDouble(obj, "x");
        double y = reqDouble(obj, "y");
        double z = reqDouble(obj, "z");
        float yaw = optFloat(obj, "yaw", 0.0f);
        float pitch = optFloat(obj, "pitch", 0.0f);
        float roll = optFloat(obj, "roll", 0.0f);
        CameraPose target;
        if (obj.has("fov")) {
            target = new CameraPose(x, y, z, yaw, pitch, roll, obj.get("fov").getAsFloat(), true);
        } else {
            target = CameraPose.of(x, y, z, yaw, pitch, roll);
        }
        controller.goToPose(target);
        return ok().toString();
    }

    private String path(JsonObject obj) {
        if (!obj.has("keyframes") || !obj.get("keyframes").isJsonArray()) {
            return err("path requires a keyframes array");
        }
        JsonArray array = obj.getAsJsonArray("keyframes");
        int n = array.size();
        if (n < 2) {
            return err("path requires at least 2 keyframes");
        }
        double durationMs = optDouble(obj, "ms", 0.0);
        boolean loop = optBool(obj, "loop", false);

        Keyframe[] frames = new Keyframe[n];
        double maxT = 0.0;
        boolean anyT = false;
        for (int i = 0; i < n; i++) {
            JsonObject kf = array.get(i).getAsJsonObject();
            if (kf.has("t")) {
                anyT = true;
                maxT = Math.max(maxT, kf.get("t").getAsDouble());
            }
        }
        if (durationMs <= 0.0) {
            durationMs = anyT ? maxT : 0.0;
        }
        if (durationMs <= 0.0) {
            return err("path requires ms or keyframe t values");
        }

        for (int i = 0; i < n; i++) {
            JsonObject kf = array.get(i).getAsJsonObject();
            double t = kf.has("t") ? kf.get("t").getAsDouble() : durationMs * i / (n - 1);
            double x = reqDouble(kf, "x");
            double y = reqDouble(kf, "y");
            double z = reqDouble(kf, "z");
            float yaw = optFloat(kf, "yaw", 0.0f);
            float pitch = optFloat(kf, "pitch", 0.0f);
            float roll = optFloat(kf, "roll", 0.0f);
            boolean hasFov = kf.has("fov");
            float fovValue = hasFov ? kf.get("fov").getAsFloat() : 0.0f;
            Easing ease = Easing.byName(optString(kf, "ease", null), Easing.LINEAR);
            frames[i] = new Keyframe(t, x, y, z, yaw, pitch, roll, fovValue, hasFov, ease);
        }
        Arrays.sort(frames, Comparator.comparingDouble(Keyframe::t));

        controller.playPath(frames, durationMs, loop, Easing.byName(optString(obj, "ease", null)));
        JsonObject res = ok();
        res.addProperty("ms", durationMs);
        res.addProperty("keyframes", n);
        return res.toString();
    }

    private String fov(JsonObject obj) {
        float value = optFloat(obj, "value", 0.0f);
        if (value <= 0.0f) {
            controller.setFovOverride(null);
            JsonObject res = ok();
            res.addProperty("cleared", true);
            return res.toString();
        }
        controller.setFovOverride(value);
        JsonObject res = ok();
        res.addProperty("fov", value);
        return res.toString();
    }

    private String hud(JsonObject obj) {
        boolean hide = optBool(obj, "hide", true);
        boolean nametags = obj.has("nametags") ? obj.get("nametags").getAsBoolean() : hide;
        controller.setHudHideRequested(hide);
        controller.setNametagsHidden(nametags);
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.options != null) {
                client.options.hudHidden = hide;
            }
        });
        JsonObject res = ok();
        res.addProperty("hudHidden", hide);
        res.addProperty("nametagsHidden", nametags);
        return res.toString();
    }

    private String status() {
        JsonObject res = ok();
        res.addProperty("active", controller.isActive());
        res.addProperty("playing", controller.isPlaying());
        res.addProperty("hudHidden", controller.isHudHideRequested());
        res.addProperty("nametagsHidden", controller.isNametagsHidden());
        Float override = controller.fovOverride();
        if (override != null) {
            res.addProperty("fovOverride", override);
        }
        CameraPose applied = controller.lastApplied();
        if (applied != null) {
            JsonObject pose = new JsonObject();
            pose.addProperty("x", applied.x());
            pose.addProperty("y", applied.y());
            pose.addProperty("z", applied.z());
            pose.addProperty("yaw", applied.yaw());
            pose.addProperty("pitch", applied.pitch());
            pose.addProperty("roll", applied.roll());
            if (applied.hasFov()) {
                pose.addProperty("fov", applied.fov());
            }
            res.add("pose", pose);
        }
        return res.toString();
    }

    private static JsonObject ok() {
        JsonObject res = new JsonObject();
        res.addProperty("ok", true);
        return res;
    }

    private static String err(String message) {
        JsonObject res = new JsonObject();
        res.addProperty("ok", false);
        res.addProperty("error", message);
        return res.toString();
    }

    private static String optString(JsonObject obj, String key, String fallback) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : fallback;
    }

    private static boolean optBool(JsonObject obj, String key, boolean fallback) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsBoolean() : fallback;
    }

    private static double optDouble(JsonObject obj, String key, double fallback) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsDouble() : fallback;
    }

    private static float optFloat(JsonObject obj, String key, float fallback) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsFloat() : fallback;
    }

    private static double reqDouble(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) {
            throw new IllegalArgumentException("missing number: " + key);
        }
        return el.getAsDouble();
    }
}
