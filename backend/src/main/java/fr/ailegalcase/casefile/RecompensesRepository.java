package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecompensesRepository extends JpaRepository<RecompensesAnalysis, UUID> {

    Optional<RecompensesAnalysis> findByCaseFileId(UUID caseFileId);
}
