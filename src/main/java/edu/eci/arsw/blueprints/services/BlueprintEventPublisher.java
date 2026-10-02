package edu.eci.arsw.blueprints.services;

import edu.eci.arsw.blueprints.model.Point;

public interface BlueprintEventPublisher {
    void publishPoint(String author, String name, Point point);
}