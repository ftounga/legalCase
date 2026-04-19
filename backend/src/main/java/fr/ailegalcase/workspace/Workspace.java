package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    private String billingEmail;

    @Column(nullable = false, length = 50)
    private String legalDomain;

    @Column(nullable = false, length = 10)
    private String country = "FRANCE";

    @Column(nullable = false, length = 50)
    private String planCode;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    // SF-122-01 : compteurs OCR pour Textract. Enforcement en SF-122-02.
    @Column(name = "ocr_pages_used_current_month", nullable = false)
    private int ocrPagesUsedCurrentMonth = 0;

    @Column(name = "ocr_pages_used_current_day", nullable = false)
    private int ocrPagesUsedCurrentDay = 0;

    // SF-122-04 : compteur cumulatif (never reset) pour calculer le reliquat des packs achetés.
    @Column(name = "ocr_pages_used_all_time", nullable = false)
    private int ocrPagesUsedAllTime = 0;

    @Column(name = "ocr_usage_last_reset_date")
    private LocalDate ocrUsageLastResetDate;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
    }
}
