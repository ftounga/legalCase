package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransfertEntrepriseL12241Repository
        extends JpaRepository<TransfertEntrepriseL12241Analysis, UUID> {

    Optional<TransfertEntrepriseL12241Analysis> findByCaseFileId(UUID caseFileId);
}
