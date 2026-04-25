package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MesuresProvisoiresRepository extends JpaRepository<MesuresProvisoiresAnalysis, UUID> {

    Optional<MesuresProvisoiresAnalysis> findByCaseFileId(UUID caseFileId);
}
