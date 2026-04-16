package fr.ailegalcase.analysis;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_analysis_source_explanations")
@Getter
@Setter
public class SourceExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_analysis_id", nullable = false)
    private CaseAnalysis caseAnalysis;

    @Column(name = "source_key", nullable = false, length = 64)
    private String sourceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private SourceType sourceType;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(length = 300)
    private String sentence;

    @Column(name = "anchor_doc_id")
    private UUID anchorDocId;

    @Column(name = "anchor_question_id")
    private UUID anchorQuestionId;

    @Column(name = "anchor_f96_code", length = 64)
    private String anchorF96Code;

    @Column(name = "anchor_chat_message_id")
    private UUID anchorChatMessageId;

    @Column(name = "anchor_piece_index")
    private Integer anchorPieceIndex;

    @Column(name = "secondary_text", length = 500)
    private String secondaryText;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
    }

    public enum SourceType {
        DOCUMENT,
        QUESTION_AI,
        CHECKLIST_F96,
        CHAT,
        MISSING_PIECE,
        ANALYSIS_DETECTION,
        MULTI
    }
}
