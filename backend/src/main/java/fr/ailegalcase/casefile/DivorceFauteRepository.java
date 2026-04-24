package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DivorceFauteRepository extends JpaRepository<DivorceFauteAnalysis, UUID> {

    Optional<DivorceFauteAnalysis> findByCaseFileId(UUID caseFileId);
}
