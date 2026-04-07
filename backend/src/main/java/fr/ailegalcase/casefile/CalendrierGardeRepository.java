package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CalendrierGardeRepository extends JpaRepository<CalendrierGarde, UUID> {
    Optional<CalendrierGarde> findByCaseFileId(UUID caseFileId);
}
