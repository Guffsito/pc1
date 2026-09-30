package pe.utec.dbp.labreserve.laboratory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.utec.dbp.labreserve.laboratory.dto.LaboratoryRequestDTO;
import pe.utec.dbp.labreserve.laboratory.dto.LaboratoryResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/laboratories")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    public LaboratoryController(LaboratoryService laboratoryService) {
        this.laboratoryService = laboratoryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LaboratoryResponseDTO> create(@Valid @RequestBody LaboratoryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratoryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<LaboratoryResponseDTO>> list() {
        return ResponseEntity.ok(laboratoryService.listAll());
    }
}
