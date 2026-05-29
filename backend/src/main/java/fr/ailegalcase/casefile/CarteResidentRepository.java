package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CarteResidentRepository extends JpaRepository<CarteResidentAnalysis, UUID> {

    Optional<CarteResidentAnalysis> findByCaseFileId(UUID caseFileId);
}
