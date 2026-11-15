package com.ignitedev.ignitetestcamera;

import com.ignitedev.ignitetestcamera.camera.CameraController;
import com.ignitedev.ignitetestcamera.net.ControlServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class IgniteTestCamera implements ClientModInitializer {

    public static final String MOD_ID = "ignitetestcamera";
    public static final Logger LOGGER = LoggerFactory.getLogger("IgniteTestCamera");

    private static final CameraController CONTROLLER = new CameraController();
    private static final int DEFAULT_PORT = 8767;

    private static ControlServer server;

    public static CameraController controller() {
        return CONTROLLER;
    }

    @Override
    public void onInitializeClient() {
        int port = resolvePort();
        server = new ControlServer(port, CONTROLLER);
        server.start();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (server != null) {
                server.stop();
            }
        });
    }

    private static int resolvePort() {
        try {
            Path cfg = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");
            if (Files.exists(cfg)) {
                Properties props = new Properties();
                try (var in = Files.newInputStream(cfg)) {
                    props.load(in);
                }
                String value = props.getProperty("port");
                if (value != null && !value.isBlank()) {
                    return Integer.parseInt(value.trim());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read port from config, falling back", e);
        }
        String sys = System.getProperty(MOD_ID + ".port");
        if (sys != null) {
            try {
                return Integer.parseInt(sys.trim());
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return DEFAULT_PORT;
    }
}
