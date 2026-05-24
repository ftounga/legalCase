package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MiseAPiedDisciplinaireRepository
        extends JpaRepository<MiseAPiedDisciplinaireAnalysis, UUID> {

    Optional<MiseAPiedDisciplinaireAnalysis> findByCaseFileId(UUID caseFileId);
}
