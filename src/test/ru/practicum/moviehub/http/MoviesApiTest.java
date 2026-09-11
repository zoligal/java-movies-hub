package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private MoviesServer server;
    private HttpClient httpClient;

    private static final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() throws Exception {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testEmptyList() throws Exception {
        HttpResponse<String> response = sendGet("/movies");

        assertEquals(200, response.statusCode(), "Статус должен быть 200");
        assertContentType(response);

        String body = response.body().trim().replaceAll("\\s", "");

        assertTrue(
                body.startsWith("[") && body.endsWith("]"),
                "Тело ответа должно быть JSON-массивом"
        );
        assertEquals("[]", body, "При пустом списке должен возвращаться пустой массив");
    }

    @Test
    void testListAfterAdd() throws Exception {
        sendPost("/movies", "{\"title\":\"Inception\",\"year\":2010}");
        sendPost("/movies", "{\"title\":\"Matrix\",\"year\":1999}");

        HttpResponse<String> response = sendGet("/movies");

        assertEquals(200, response.statusCode(), "Статус должен быть 200");
        assertContentType(response);

        assertTrue(response.body().contains("Inception"), "В ответе должен быть фильм Inception");
        assertTrue(response.body().contains("Matrix"), "В ответе должен быть фильм Matrix");
    }

    @Test
    void testPostSuccess() throws Exception {
        HttpResponse<String> response = sendPost("/movies", "{\"title\":\"Inception\",\"year\":2010}");

        assertEquals(201, response.statusCode(), "Статус должен быть 201");
        assertContentType(response);

        assertTrue(response.body().contains("\"id\""), "В ответе должен присутствовать id");
        assertTrue(response.body().contains("Inception"), "В ответе должен быть заголовок фильма");
    }

    @Test
    void testPostEmptyTitle() throws Exception {
        HttpResponse<String> response = sendPost("/movies", "{\"title\":\"\",\"year\":2010}");

        assertEquals(422, response.statusCode(), "Статус должен быть 422");
        assertTrue(response.body().contains("error"), "В теле ответа должна быть ошибка");
        assertTrue(response.body().contains("details"), "В теле ответа должны быть детали ошибки");
    }

    @Test
    void testPostLongTitle() throws Exception {
        String longTitle = "x".repeat(101);
        String jsonPayload = String.format("{\"title\":\"%s\",\"year\":2010}", longTitle);

        HttpResponse<String> response = sendPost("/movies", jsonPayload);

        assertEquals(422, response.statusCode(), "Статус должен быть 422");
        assertTrue(response.body().contains("details"), "В теле ответа должны быть детали ошибки");
    }

    @Test
    void testPostYearTooSmall() throws Exception {
        HttpResponse<String> response = sendPost("/movies", "{\"title\":\"Test\",\"year\":1887}");

        assertEquals(422, response.statusCode(), "Статус должен быть 422");
    }

    @Test
    void testPostYearTooLarge() throws Exception {
        int invalidYear = java.time.Year.now().getValue() + 2;
        String jsonPayload = String.format("{\"title\":\"Test\",\"year\":%d}", invalidYear);

        HttpResponse<String> response = sendPost("/movies", jsonPayload);

        assertEquals(422, response.statusCode(), "Статус должен быть 422");
    }

    @Test
    void testPostWrongContentType() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "text/plain")
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString("bla"))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(415, response.statusCode(), "Статус должен быть 415");
    }

    @Test
    void testPostInvalidJson() throws Exception {
        HttpResponse<String> response = sendPost("/movies", "{not valid json}");

        assertEquals(422, response.statusCode(), "Статус должен быть 422");
        assertTrue(response.body().contains("error"), "В теле ответа должна быть ошибка");
    }

    @Test
    void testGetByIdExists() throws Exception {
        HttpResponse<String> createdResponse = sendPost("/movies", "{\"title\":\"Inception\",\"year\":2010}");
        int movieId = extractId(createdResponse.body());

        HttpResponse<String> response = sendGet(String.format("/movies/%d", movieId));

        assertEquals(200, response.statusCode(), "Статус должен быть 200");
        assertContentType(response);
        assertTrue(response.body().contains("Inception"), "В ответе должен быть фильм Inception");
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        HttpResponse<String> response = sendGet("/movies/999");

        assertEquals(404, response.statusCode(), "Статус должен быть 404");
        assertTrue(response.body().contains("error"), "В теле ответа должна быть ошибка");
    }

    @Test
    void testGetByIdInvalidId() throws Exception {
        HttpResponse<String> response = sendGet("/movies/abc");

        assertEquals(400, response.statusCode(), "Статус должен быть 400");
        assertTrue(response.body().contains("error"), "В теле ответа должна быть ошибка");
    }

    @Test
    void testDeleteExists() throws Exception {
        HttpResponse<String> createdResponse = sendPost("/movies", "{\"title\":\"Inception\",\"year\":2010}");
        int movieId = extractId(createdResponse.body());

        HttpResponse<String> response = sendDelete("/movies/" + movieId);

        assertEquals(204, response.statusCode(), "Статус должен быть 204");
    }

    @Test
    void testDeleteNotFound() throws Exception {
        HttpResponse<String> response = sendDelete("/movies/999");

        assertEquals(404, response.statusCode(), "Статус должен быть 404");
    }

    @Test
    void testDeleteInvalidId() throws Exception {
        HttpResponse<String> response = sendDelete("/movies/abc");

        assertEquals(400, response.statusCode(), "Статус должен быть 400");
    }

    @Test
    void testFilterByYearMatch() throws Exception {
        sendPost("/movies", "{\"title\":\"Inception\",\"year\":2010}");
        sendPost("/movies", "{\"title\":\"Matrix\",\"year\":1999}");

        HttpResponse<String> response = sendGet("/movies?year=2010");

        assertEquals(200, response.statusCode(), "Статус должен быть 200");
        assertTrue(response.body().contains("Inception"), "Должен быть возвращён фильм Inception");
        assertFalse(response.body().contains("Matrix"), "Фильм Matrix не должен быть в ответе");
    }

    @Test
    void testFilterByYearNoMatch() throws Exception {
        sendPost("/movies", "{\"title\":\"Inception\",\"year\":2010}");

        HttpResponse<String> response = sendGet("/movies?year=1900");

        assertEquals(200, response.statusCode(), "Статус должен быть 200");
        String body = response.body().trim().replaceAll("\\s", "");
        assertEquals("[]", body, "Должен возвращаться пустой массив");
    }

    @Test
    void testFilterByYearInvalid() throws Exception {
        HttpResponse<String> response = sendGet("/movies?year=abc");

        assertEquals(400, response.statusCode(), "Статус должен быть 400");
        assertTrue(response.body().contains("error"), "В теле ответа должна быть ошибка");
    }

    @Test
    void testUnsupportedMethod() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .timeout(Duration.ofSeconds(2))
                .PUT(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(405, response.statusCode(), "Статус должен быть 405");
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s%s", BASE_URL, path)))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendPost(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s%s", BASE_URL, path)))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendDelete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s%s", BASE_URL, path)))
                .timeout(Duration.ofSeconds(2))
                .DELETE()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertContentType(HttpResponse<String> response) {
        String expectedContentType = "application/json; charset=UTF-8";
        String actualContentType = response.headers().firstValue("Content-Type").orElse("");

        assertEquals(
                expectedContentType,
                actualContentType,
                String.format(
                        "Content-Type должен быть '%s', но получен '%s'",
                        expectedContentType,
                        actualContentType
                )
        );
    }

    private int extractId(String body) {
        String idKey = "\"id\":";
        int keyIndex = body.indexOf(idKey);
        int startIndex = keyIndex + idKey.length();
        int endIndex = startIndex;

        while (endIndex < body.length() && Character.isDigit(body.charAt(endIndex))) {
            endIndex++;
        }

        return Integer.parseInt(body.substring(startIndex, endIndex));
    }
}
