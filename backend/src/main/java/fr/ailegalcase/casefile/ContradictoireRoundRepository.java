package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContradictoireRoundRepository extends JpaRepository<ContradictoireRound, UUID> {

    List<ContradictoireRound> findByCaseFileIdOrderByRoundNumberAsc(UUID caseFileId);

    Optional<ContradictoireRound> findFirstByCaseFileIdOrderByRoundNumberDesc(UUID caseFileId);
}
