package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler implements HttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        String requestPath = exchange.getRequestURI().getPath();
        String queryString = exchange.getRequestURI().getQuery();

        ParseYearResult yearResult = parseYearParam(queryString);

        try {
            switch (requestMethod) {
                case "GET":
                    handleGet(exchange, requestPath, yearResult);
                    break;
                case "POST":
                    handlePost(exchange, requestPath);
                    break;
                case "DELETE":
                    handleDelete(exchange, requestPath);
                    break;
                default:
                    sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception exception) {
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleGet(HttpExchange exchange, String path, ParseYearResult yearResult) throws IOException {
        if ("/movies".equals(path)) {
            if (yearResult.wasSpecified) {
                if (yearResult.value == null) {
                    sendError(exchange, 400, "Параметр year должен быть числом");
                    return;
                }
                List<Movie> movies = store.findByYear(yearResult.value);
                sendJson(exchange, 200, movies);
                return;
            }
            List<Movie> movies = store.getAll();
            sendJson(exchange, 200, movies);
            return;
        }

        if (path.startsWith("/movies/")) {
            String idPart = path.substring("/movies/".length());
            if (!isNumeric(idPart)) {
                sendError(exchange, 400, "Некорректный ID");
                return;
            }
            int movieId = Integer.parseInt(idPart);
            Movie movie = store.getById(movieId);
            if (movie == null) {
                sendError(exchange, 404, "Фильм не найден");
            } else {
                sendJson(exchange, 200, movie);
            }
            return;
        }

        sendStatus(exchange, 404);
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (!"/movies".equals(path)) {
            sendStatus(exchange, 404);
            return;
        }

        String contentTypeHeader = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentTypeHeader == null || !contentTypeHeader.contains("application/json")) {
            sendError(exchange, 415, "Unsupported Media Type");
            return;
        }

        String requestBody;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            StringBuilder bufferBuilder = new StringBuilder();
            char[] buffer = new char[1024];
            int readCount;
            while ((readCount = reader.read(buffer)) != -1) {
                bufferBuilder.append(buffer, 0, readCount);
            }
            requestBody = bufferBuilder.toString();
        }

        if (requestBody == null || requestBody.isBlank()) {
            sendErrorWithDetails(exchange, 422, "Ошибка валидации", List.of("Тело запроса не может быть пустым"));
            return;
        }

        Movie movie;
        Gson gson = new Gson();
        try {
            movie = gson.fromJson(requestBody, Movie.class);
        } catch (JsonSyntaxException syntaxException) {
            sendErrorWithDetails(exchange, 422, "Ошибка валидации", List.of("Некорректный JSON"));
            return;
        }

        if (movie == null) {
            sendErrorWithDetails(exchange, 422, "Ошибка валидации",
                    List.of("Тело запроса не должно быть null"));
            return;
        }

        movie.setId(null);

        List<String> validationErrors = new ArrayList<>();

        String title = movie.getTitle();
        if (title == null || title.isBlank()) {
            validationErrors.add("название не должно быть пустым");
        } else if (title.length() > 100) {
            validationErrors.add("длина названия не должна превышать 100 символов");
        }

        int currentYear = Year.now().getValue();
        int movieYear = movie.getYear();
        if (movieYear < 1888 || movieYear > currentYear + 1) {
            validationErrors.add(String.format("год должен быть между 1888 и %d", currentYear + 1));
        }

        if (!validationErrors.isEmpty()) {
            sendErrorWithDetails(exchange, 422, "Ошибка валидации", validationErrors);
            return;
        }

        Movie savedMovie = store.save(movie);
        sendJson(exchange, 201, savedMovie);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (!path.startsWith("/movies/")) {
            sendStatus(exchange, 404);
            return;
        }
        String idPart = path.substring("/movies/".length());
        if (!isNumeric(idPart)) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }
        int movieId = Integer.parseInt(idPart);
        if (store.deleteById(movieId)) {
            sendStatus(exchange, 204);
        } else {
            sendError(exchange, 404, "Фильм не найден");
        }
    }

    private static class ParseYearResult {
        final Integer value;
        final boolean wasSpecified;

        private ParseYearResult(Integer value, boolean wasSpecified) {
            this.value = value;
            this.wasSpecified = wasSpecified;
        }
    }

    private ParseYearResult parseYearParam(String queryString) {
        if (queryString == null) {
            return new ParseYearResult(null, false);
        }

        String[] queryParts = queryString.split("&");
        for (String part : queryParts) {
            if (part.startsWith("year=")) {
                String value = part.substring("year=".length());
                try {
                    int year = Integer.parseInt(value);
                    return new ParseYearResult(year, true);
                } catch (NumberFormatException numberFormatException) {
                    return new ParseYearResult(null, true);
                }
            }
        }
        return new ParseYearResult(null, false);
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            int number = Integer.parseInt(str);
            return number > 0;
        } catch (NumberFormatException numberFormatException) {
            return false;
        }
    }
}
