package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MineursImmigrationRepository extends JpaRepository<MineursImmigrationAnalysis, UUID> {

    Optional<MineursImmigrationAnalysis> findByCaseFileId(UUID caseFileId);
}
