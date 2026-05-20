package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-207-04 : entité 1:1 par dossier portant la dernière analyse de
 * déclaration d'accident du travail à Fedris (Loi du 10 avril 1971
 * art. 62 ; AR 21/12/1971 art. 25). Outil BE-only — aucun équivalent
 * FR direct (la procédure d'instruction CPAM AT en France est gérée
 * par F-DT-33).
 *
 * <p>Snapshot JSON complet (inputs + résultat + formuleCalcul +
 * consequencesNonRespect) dans {@code result_data} pour restitution UI
 * sans recalcul et survie aux reload (mémoire
 * {@code feedback_decision_tools_all_fields_prefilled}).</p>
 */
@Entity
@Table(name = "at_fedris_declaration_analyses")
@Getter
@Setter
public class AtFedrisDeclarationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    /** Snapshot JSON sérialisé (inputs + outputs + formuleCalcul) — source de vérité pour le GET. */
    @Column(name = "result_data", nullable = false, columnDefinition = "TEXT")
    private String resultData = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
