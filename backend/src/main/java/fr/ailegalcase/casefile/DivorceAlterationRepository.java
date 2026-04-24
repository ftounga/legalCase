package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DivorceAlterationRepository extends JpaRepository<DivorceAlterationAnalysis, UUID> {

    Optional<DivorceAlterationAnalysis> findByCaseFileId(UUID caseFileId);
}
