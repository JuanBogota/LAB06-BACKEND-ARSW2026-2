package edu.eci.arsw.blueprints.persistence;

import edu.eci.arsw.blueprints.model.Blueprint;
import java.util.Set;
import edu.eci.arsw.blueprints.model.Point;
import java.util.List;

public interface BlueprintPersistence {

    void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException;

    Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException;

    Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException;

    Set<Blueprint> getAllBlueprints();

    void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException;

    void replacePoints(String author, String name, List<Point> points) throws BlueprintNotFoundException;

    void deleteBlueprint(String author, String name) throws BlueprintNotFoundException;
}
