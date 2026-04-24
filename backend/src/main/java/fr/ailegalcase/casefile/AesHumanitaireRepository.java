package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AesHumanitaireRepository extends JpaRepository<AesHumanitaireAnalysis, UUID> {

    Optional<AesHumanitaireAnalysis> findByCaseFileId(UUID caseFileId);
}
