package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MesuresEloignementRepository extends JpaRepository<MesuresEloignementAnalysis, UUID> {

    Optional<MesuresEloignementAnalysis> findByCaseFileId(UUID caseFileId);
}
