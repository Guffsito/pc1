package pe.utec.dbp.labreserve.laboratory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utec.dbp.labreserve.auth.UserRepository;
import pe.utec.dbp.labreserve.laboratory.dto.LaboratoryRequestDTO;
import pe.utec.dbp.labreserve.laboratory.dto.LaboratoryResponseDTO;
import pe.utec.dbp.labreserve.model.LabStatus;
import pe.utec.dbp.labreserve.model.Laboratory;
import pe.utec.dbp.labreserve.model.Role;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.shared.BadRequestException;
import pe.utec.dbp.labreserve.shared.ConflictException;
import pe.utec.dbp.labreserve.shared.NotFoundException;

import java.util.List;

@Service
public class LaboratoryService {

    private final LaboratoryRepository laboratoryRepository;
    private final UserRepository userRepository;

    public LaboratoryService(LaboratoryRepository laboratoryRepository, UserRepository userRepository) {
        this.laboratoryRepository = laboratoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LaboratoryResponseDTO create(LaboratoryRequestDTO request) {
        if (laboratoryRepository.existsByName(request.name())) {
            throw new ConflictException("Ya existe un laboratorio llamado " + request.name());
        }

        UserAccount manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> NotFoundException.of("El usuario", request.managerId()));

        if (manager.getRole() == Role.ROLE_STUDENT) {
            throw new BadRequestException("Un estudiante no puede ser encargado de laboratorio");
        }

        Laboratory laboratory = new Laboratory(
                request.name(), request.location(), manager, LabStatus.ACTIVE);

        return LaboratoryResponseDTO.from(laboratoryRepository.save(laboratory));
    }

    @Transactional(readOnly = true)
    public List<LaboratoryResponseDTO> listAll() {
        return laboratoryRepository.findAll().stream()
                .map(LaboratoryResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Laboratory getById(Long id) {
        return laboratoryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("El laboratorio", id));
    }
}
