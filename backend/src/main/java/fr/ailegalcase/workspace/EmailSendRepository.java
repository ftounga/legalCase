package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface EmailSendRepository extends JpaRepository<EmailSend, UUID> {

    boolean existsByUserAndEmailType(User user, EmailSend.EmailType emailType);

    @Query("SELECT COUNT(e) > 0 FROM EmailSend e WHERE e.user = :user AND e.emailType = :type AND e.sentAt >= :since")
    boolean existsByUserAndEmailTypeAndSentAtAfter(@Param("user") User user,
                                                   @Param("type") EmailSend.EmailType type,
                                                   @Param("since") Instant since);
}
