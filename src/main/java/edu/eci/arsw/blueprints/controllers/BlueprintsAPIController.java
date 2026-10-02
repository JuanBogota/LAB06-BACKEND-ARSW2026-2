package edu.eci.arsw.blueprints.controllers;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import edu.eci.arsw.blueprints.model.ApiResponse;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/blueprints")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    // GET /blueprints
    @Operation(summary = "Obtener todos los blueprints")
    @GetMapping
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(services.getAllBlueprints()));
    }

    // GET /blueprints/{author}
    @Operation(summary = "Obtener blueprints de un autor")
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(@PathVariable String author) {
        return ResponseEntity.ok(ApiResponse.ok(services.getBlueprintsByAuthor(author)));
    }

    // GET /blueprints/{author}/{bpname}
    @Operation(summary = "Obtener un blueprint específico")
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Blueprint>> byAuthorAndName(
            @PathVariable String author, @PathVariable String bpname)
            throws BlueprintNotFoundException {
        return ResponseEntity.ok(ApiResponse.ok(services.getBlueprint(author, bpname)));
    }

    // POST /blueprints
    @Operation(summary = "Crear un nuevo blueprint")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Blueprint creado")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos")
    @PostMapping
    public ResponseEntity<ApiResponse<Blueprint>> add(@Valid @RequestBody NewBlueprintRequest req)
            throws BlueprintPersistenceException {
        Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
        services.addNewBlueprint(bp);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bp));
    }

    // PUT /blueprints/{author}/{bpname}/points
    @Operation(summary = "Agregar un punto a un blueprint")
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<Blueprint>> addPoint(
            @PathVariable String author, @PathVariable String bpname, @RequestBody Point p)
            throws BlueprintNotFoundException {
        services.addPoint(author, bpname, p.x(), p.y());
        Blueprint updated = services.getBlueprint(author, bpname);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.accepted(updated));
    }

    // PUT /blueprints/{author}/{bpname}
    @Operation(summary = "Reemplazar todos los puntos de un blueprint")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Blueprint actualizado")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Blueprint no existe")
    @PutMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Blueprint>> replacePoints(
            @PathVariable String author, @PathVariable String bpname,
            @Valid @RequestBody ReplacePointsRequest req)
            throws BlueprintNotFoundException {
        services.replacePoints(author, bpname, req.points());
        Blueprint updated = services.getBlueprint(author, bpname);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.accepted(updated));
    }

    // DELETE /blueprints/{author}/{bpname}
    @Operation(summary = "Eliminar un blueprint")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Blueprint eliminado")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Blueprint no existe")
    @DeleteMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String author, @PathVariable String bpname)
            throws BlueprintNotFoundException {
        services.deleteBlueprint(author, bpname);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record NewBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            @Valid java.util.List<Point> points
    ) { }

    public record ReplacePointsRequest(@NotNull @Valid List<Point> points) { }
}
