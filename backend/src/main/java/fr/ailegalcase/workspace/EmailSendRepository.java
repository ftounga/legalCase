package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailSendRepository extends JpaRepository<EmailSend, UUID> {

    boolean existsByUserAndEmailType(User user, EmailSend.EmailType emailType);
}
