package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_question_answers")
@Getter
@Setter
public class AiQuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_question_id", nullable = false)
    private AiQuestion aiQuestion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answered_by_user_id", nullable = false)
    private User answeredByUser;

    @Column(name = "answer_text", nullable = false, length = Integer.MAX_VALUE)
    private String answerText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
    }
}
