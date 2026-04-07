package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PartageImmobilierRepository extends JpaRepository<PartageImmobilier, UUID> {
    Optional<PartageImmobilier> findByCaseFileId(UUID caseFileId);
}
