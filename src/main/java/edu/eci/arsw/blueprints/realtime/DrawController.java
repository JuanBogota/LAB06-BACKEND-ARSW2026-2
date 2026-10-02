package edu.eci.arsw.blueprints.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.services.DrawingService;

@Controller
public class DrawController {

    private static final Logger log = LoggerFactory.getLogger(DrawController.class);

    private final DrawingService drawing;

    public DrawController(DrawingService drawing) {
        this.drawing = drawing;
    }

    @MessageMapping("/draw")
    public void onDraw(DrawMessage msg) {
        drawing.draw(msg.author(), msg.name(), msg.point());
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    public void onInvalid(IllegalArgumentException e) {
        log.warn("Mensaje /draw rechazado: {}", e.getMessage());
    }

    public record DrawMessage(String author, String name, Point point) { }
}