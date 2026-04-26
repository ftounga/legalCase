package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DonationRepository extends JpaRepository<DonationAnalysis, UUID> {

    Optional<DonationAnalysis> findByCaseFileId(UUID caseFileId);
}
