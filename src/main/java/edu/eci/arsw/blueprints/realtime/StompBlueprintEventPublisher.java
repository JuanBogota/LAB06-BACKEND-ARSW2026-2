package edu.eci.arsw.blueprints.realtime;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.services.BlueprintEventPublisher;

@Component
public class StompBlueprintEventPublisher implements BlueprintEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(StompBlueprintEventPublisher.class);

    private final SimpMessagingTemplate template;

    public StompBlueprintEventPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    @Override
    public void publishPoint(String author, String name, Point point) {
        String destination = "/topic/blueprints." + author + "." + name;
        template.convertAndSend(destination, new BlueprintUpdate(author, name, List.of(point)));
        log.info("Punto {} publicado en {}", point, destination);
    }

    public record BlueprintUpdate(String author, String name, List<Point> points) { }
}