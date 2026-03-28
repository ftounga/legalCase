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
}
