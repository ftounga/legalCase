package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TribunalTravailFicheRepository extends JpaRepository<TribunalTravailFiche, UUID> {

    Optional<TribunalTravailFiche> findByCaseFileId(UUID caseFileId);
}
