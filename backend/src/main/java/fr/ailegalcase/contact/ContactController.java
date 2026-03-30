package fr.ailegalcase.contact;

import fr.ailegalcase.workspace.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> contact(@Valid @RequestBody ContactRequest request) {
        emailService.sendContactToTeam(request);
        emailService.sendContactConfirmation(request);
        return ResponseEntity.ok(Map.of("status", "sent"));
    }
}
