package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TribunalFamilleBeMesuresProvisoiresRepository
        extends JpaRepository<TribunalFamilleBeMesuresProvisoiresAnalysis, UUID> {

    Optional<TribunalFamilleBeMesuresProvisoiresAnalysis> findByCaseFileId(UUID caseFileId);
}
