package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AiQuestionRepository extends JpaRepository<AiQuestion, UUID> {

    List<AiQuestion> findByCaseFileIdOrderByOrderIndex(UUID caseFileId);

    List<AiQuestion> findByCaseFileIdIn(Collection<UUID> caseFileIds);

    void deleteByCaseFileIdIn(Collection<UUID> caseFileIds);
}
