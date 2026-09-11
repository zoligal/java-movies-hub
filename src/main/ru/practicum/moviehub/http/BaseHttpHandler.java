package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ru.practicum.moviehub.api.ErrorResponse;

public abstract class BaseHttpHandler {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_VALUE  = "application/json; charset=UTF-8";

    protected final Gson gson = new Gson();

    protected void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String jsonString = gson.toJson(body);
        byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(CONTENT_TYPE_HEADER, CONTENT_TYPE_VALUE);
        exchange.sendResponseHeaders(statusCode, jsonBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(jsonBytes);
        }
    }

    protected void sendStatus(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String error) throws IOException {
        sendJson(exchange, statusCode, new ErrorResponse(error));
    }

    protected void sendErrorWithDetails(HttpExchange exchange, int statusCode, String error, List<String> details) throws IOException {
        sendJson(exchange, statusCode, new ErrorResponse(error, details));
    }
}
