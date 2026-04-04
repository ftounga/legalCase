package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImmigrationPieceCheckRepository extends JpaRepository<ImmigrationPieceCheck, UUID> {

    List<ImmigrationPieceCheck> findByCaseFileIdAndTitreTypeAndCountry(UUID caseFileId, String titreType, String country);

    Optional<ImmigrationPieceCheck> findByCaseFileIdAndTitreTypeAndCountryAndLabel(
            UUID caseFileId, String titreType, String country, String label);
}
