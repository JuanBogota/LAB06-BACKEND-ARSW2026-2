package edu.eci.arsw.blueprints.persistence;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.entity.BlueprintEntity;
import edu.eci.arsw.blueprints.persistence.entity.PointEmbeddable;
import edu.eci.arsw.blueprints.persistence.repository.BlueprintJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Primary // Resuelve la ambigüedad con InMemoryBlueprintPersistence: esta pasa a ser la implementación activa
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BlueprintJpaRepository repo;

    @Autowired
    public PostgresBlueprintPersistence(BlueprintJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (repo.existsByAuthorAndName(bp.getAuthor(), bp.getName())) {
            throw new BlueprintPersistenceException(
                    "Blueprint already exists: " + bp.getAuthor() + ":" + bp.getName());
        }
        repo.save(toEntity(bp));
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        BlueprintEntity entity = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        return toDomain(entity);
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        List<BlueprintEntity> entities = repo.findByAuthor(author);
        if (entities.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints for author: " + author);
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        return new HashSet<>(repo.findAll().stream().map(this::toDomain).collect(Collectors.toSet()));
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        BlueprintEntity entity = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        entity.getPoints().add(new PointEmbeddable(x, y));
        repo.save(entity);
    }

    @Override
    @Transactional
    public void replacePoints(String author, String name, List<Point> points) throws BlueprintNotFoundException {
        BlueprintEntity entity = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        entity.getPoints().clear();
        points.forEach(p -> entity.getPoints().add(new PointEmbeddable(p.x(), p.y())));
    }

    @Override
    @Transactional
    public void deleteBlueprint(String author, String name) throws BlueprintNotFoundException {
        BlueprintEntity entity = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        repo.delete(entity);
    }

    private BlueprintEntity toEntity(Blueprint bp) {
        List<PointEmbeddable> points = bp.getPoints().stream()
                .map(p -> new PointEmbeddable(p.x(), p.y()))
                .collect(Collectors.toList());
        return new BlueprintEntity(bp.getAuthor(), bp.getName(), points);
    }

    private Blueprint toDomain(BlueprintEntity entity) {
        List<Point> points = entity.getPoints().stream()
                .map(p -> new Point(p.getX(), p.getY()))
                .collect(Collectors.toList());
        return new Blueprint(entity.getAuthor(), entity.getName(), points);
    }
}