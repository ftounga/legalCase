package fr.ailegalcase.billing;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_purchases")
public class CreditPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "tokens_bought", nullable = false)
    private long tokensBought;

    /** SF-122-04 : pages OCR achetées (un des deux champs est 0 selon le type de pack). */
    @Column(name = "ocr_pages_bought", nullable = false)
    private int ocrPagesBought;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(name = "stripe_session_id", nullable = false, unique = true)
    private String stripeSessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public long getTokensBought() { return tokensBought; }
    public void setTokensBought(long tokensBought) { this.tokensBought = tokensBought; }
    public int getOcrPagesBought() { return ocrPagesBought; }
    public void setOcrPagesBought(int ocrPagesBought) { this.ocrPagesBought = ocrPagesBought; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }
    public Instant getCreatedAt() { return createdAt; }
}
