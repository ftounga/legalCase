package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AiQuestionAnswerRepository extends JpaRepository<AiQuestionAnswer, UUID> {

    Optional<AiQuestionAnswer> findFirstByAiQuestionIdOrderByCreatedAtDesc(UUID aiQuestionId);

    boolean existsByAiQuestion_CaseFile_IdAndCreatedAtAfter(UUID caseFileId, java.time.Instant after);

    void deleteByAiQuestionIdIn(Collection<UUID> aiQuestionIds);
}
