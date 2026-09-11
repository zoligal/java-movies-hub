package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MoviesStore {
    private int nextId = 1;
    private final Map<Integer, Movie> moviesById = new ConcurrentHashMap<>();

    public List<Movie> getAll() {
        return new ArrayList<>(moviesById.values());
    }

    public Movie getById(int id) {
        return moviesById.get(id);
    }

    public Movie save(Movie movie) {
        if (movie.getId() == null) {
            movie.setId(nextId++);
        }
        moviesById.put(movie.getId(), movie);
        return movie;
    }

    public boolean deleteById(int id) {
        return moviesById.remove(id) != null;
    }

    public List<Movie> findByYear(int year) {
        List<Movie> result = new ArrayList<>();
        for (Movie movie : moviesById.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }
        return result;
    }
}
