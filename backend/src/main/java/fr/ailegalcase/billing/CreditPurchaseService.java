package fr.ailegalcase.billing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreditPurchaseService {

    private final CreditPurchaseRepository creditPurchaseRepository;

    public CreditPurchaseService(CreditPurchaseRepository creditPurchaseRepository) {
        this.creditPurchaseRepository = creditPurchaseRepository;
    }

    public long getTotalTokensBought(UUID workspaceId) {
        return creditPurchaseRepository.sumTokensBoughtByWorkspaceId(workspaceId);
    }

    /** SF-122-04 : somme des pages OCR achetées sur toute la vie du workspace. */
    public long getTotalOcrPagesBought(UUID workspaceId) {
        return creditPurchaseRepository.sumOcrPagesBoughtByWorkspaceId(workspaceId);
    }

    @Transactional
    public void record(UUID workspaceId, long tokensBought, int amountCents, String stripeSessionId) {
        if (creditPurchaseRepository.findByStripeSessionId(stripeSessionId).isPresent()) return;
        CreditPurchase cp = new CreditPurchase();
        cp.setWorkspaceId(workspaceId);
        cp.setTokensBought(tokensBought);
        cp.setAmountCents(amountCents);
        cp.setStripeSessionId(stripeSessionId);
        creditPurchaseRepository.save(cp);
    }

    /** SF-122-04 : persiste un achat de pack OCR. Idempotent par stripeSessionId. */
    @Transactional
    public void recordOcrPack(UUID workspaceId, int ocrPagesBought, int amountCents, String stripeSessionId) {
        if (creditPurchaseRepository.findByStripeSessionId(stripeSessionId).isPresent()) return;
        CreditPurchase cp = new CreditPurchase();
        cp.setWorkspaceId(workspaceId);
        cp.setTokensBought(0);
        cp.setOcrPagesBought(ocrPagesBought);
        cp.setAmountCents(amountCents);
        cp.setStripeSessionId(stripeSessionId);
        creditPurchaseRepository.save(cp);
    }
}
