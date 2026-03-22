package fr.ailegalcase.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByCaseFileIdOrderByCreatedAtAsc(UUID caseFileId);

    @Query(value = """
            SELECT COUNT(*)
            FROM chat_messages cm
            JOIN case_files cf ON cf.id = cm.case_file_id
            WHERE cf.workspace_id = :workspaceId
              AND cm.created_at >= :startOfMonth
            """, nativeQuery = true)
    long countByWorkspaceIdSince(@Param("workspaceId") UUID workspaceId,
                                  @Param("startOfMonth") Instant startOfMonth);
}
