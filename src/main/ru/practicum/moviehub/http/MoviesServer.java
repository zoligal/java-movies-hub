package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer httpServer;
    private final MoviesStore moviesStore;

    public MoviesServer(MoviesStore moviesStore, int port) throws IOException {
        this.moviesStore = moviesStore;
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(null);

        int boundPort = httpServer.getAddress().getPort();
        System.out.printf("[SERVER] Starting on port: %d%n", boundPort);

        httpServer.createContext("/movies", new MoviesHandler(moviesStore));
    }

    public void start() {
        httpServer.start();
        System.out.println("[SERVER] Started.");
    }

    public void stop() {
        httpServer.stop(0);
        System.out.println("[SERVER] Stopped.");
    }

    public int getPort() {
        return httpServer.getAddress().getPort();
    }
}
