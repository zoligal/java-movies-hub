package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;

public class MovieHubApp {

    public static void main(String[] args) {
        try {
            final MoviesServer server = new MoviesServer(new MoviesStore(), 8080);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            server.start();
            System.out.printf("[APP] MovieHub запущен на порту %d%n", 8080);
        } catch (IOException exception) {
            System.err.printf("[APP] Не удалось запустить сервер: %s%n", exception.getMessage());
            exception.printStackTrace();
        }
    }
}
