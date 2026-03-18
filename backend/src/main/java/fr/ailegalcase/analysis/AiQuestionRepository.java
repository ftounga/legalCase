package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiQuestionRepository extends JpaRepository<AiQuestion, UUID> {

    List<AiQuestion> findByCaseFileIdOrderByOrderIndex(UUID caseFileId);
}
