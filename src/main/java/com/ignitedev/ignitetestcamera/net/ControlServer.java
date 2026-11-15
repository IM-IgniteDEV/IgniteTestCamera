package com.ignitedev.ignitetestcamera.net;

import com.ignitedev.ignitetestcamera.IgniteTestCamera;
import com.ignitedev.ignitetestcamera.camera.CameraController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Localhost-only line protocol: one JSON object per line in, one JSON object per line out.
 * Runs off the render thread; mutating commands are handed to the client thread by the handler.
 */
public final class ControlServer {

    private final int port;
    private final CommandHandler handler;

    private volatile boolean running;
    private ServerSocket serverSocket;

    public ControlServer(int port, CameraController controller) {
        this.port = port;
        this.handler = new CommandHandler(controller);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        } catch (IOException e) {
            IgniteTestCamera.LOGGER.error("Failed to bind control channel on 127.0.0.1:{}", port, e);
            return;
        }
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "itcam-control");
        acceptThread.setDaemon(true);
        acceptThread.start();
        IgniteTestCamera.LOGGER.info("Camera control channel listening on 127.0.0.1:{}", port);
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Thread connection = new Thread(() -> handle(socket), "itcam-conn");
                connection.setDaemon(true);
                connection.start();
            } catch (IOException e) {
                if (running) {
                    IgniteTestCamera.LOGGER.debug("Accept loop error", e);
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                out.write(handler.handleLine(trimmed));
                out.write('\n');
                out.flush();
            }
        } catch (IOException e) {
            // client disconnected
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // shutting down
        }
    }
}
