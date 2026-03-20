package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AiQuestionAnswerRepository extends JpaRepository<AiQuestionAnswer, UUID> {

    Optional<AiQuestionAnswer> findFirstByAiQuestionIdOrderByCreatedAtDesc(UUID aiQuestionId);

    void deleteByAiQuestionIdIn(Collection<UUID> aiQuestionIds);
}
