package edu.eci.arsw.blueprints.services;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import edu.eci.arsw.blueprints.model.Point;

@Service
public class DrawingService {

    // Evita nombres con '.', '*' o '#', que romperían la convención de tópicos
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final BlueprintEventPublisher publisher;

    public DrawingService(BlueprintEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void draw(String author, String name, Point point) {
        if (point == null || !isSafe(author) || !isSafe(name)) {
            throw new IllegalArgumentException(
                "author y name deben ser alfanuméricos (_ y - permitidos) y point es obligatorio");
        }
        publisher.publishPoint(author, name, point);
    }

    private boolean isSafe(String value) {
        return value != null && SAFE_NAME.matcher(value).matches();
    }
}