package fr.ailegalcase.blog.cost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * F-120 SF-120-03 — garde-fou principal coût IA blog.
 *
 * <p>Source de vérité unique : table {@code blog_usage_events}. Avant tout appel
 * Anthropic ou OpenAI déclenché par le pipeline blog, l'orchestrateur appelle
 * {@link #canSpend(IaProvider, BigDecimal)} pour vérifier que le cap mensuel
 * n'est pas dépassé. Après chaque appel, {@link #record} persiste l'événement.
 *
 * <p>Caps configurables via {@code application.yml} :
 * <pre>
 * blog.ia.budget.anthropic-eur=30
 * blog.ia.budget.openai-eur=20
 * </pre>
 *
 * <p>La table est dédiée au blog (découplée de {@code usage_events} qui est
 * dossier-orienté avec {@code case_file_id NOT NULL}). Les coûts blog ne sont
 * donc jamais comptabilisés dans les quotas plan utilisateur F-16.
 */
@Service
public class IaCostTracker {

    private static final Logger log = LoggerFactory.getLogger(IaCostTracker.class);

    private final BlogUsageEventRepository repository;
    private final Map<IaProvider, BigDecimal> capsByProvider;

    public IaCostTracker(BlogUsageEventRepository repository,
                         @Value("${blog.ia.budget.anthropic-eur:30}") BigDecimal anthropicCap,
                         @Value("${blog.ia.budget.openai-eur:20}") BigDecimal openaiCap) {
        this.repository = repository;
        this.capsByProvider = new EnumMap<>(IaProvider.class);
        this.capsByProvider.put(IaProvider.ANTHROPIC, anthropicCap);
        this.capsByProvider.put(IaProvider.OPENAI, openaiCap);
    }

    /**
     * Vérifie si une dépense estimée peut être engagée sans dépasser le cap mensuel.
     *
     * @param provider          fournisseur cible
     * @param estimatedCostEur  coût estimé de l'appel à venir
     * @return {@code true} si {@code monthlySpent + estimatedCost <= cap}
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean canSpend(IaProvider provider, BigDecimal estimatedCostEur) {
        BigDecimal cap = capsByProvider.get(provider);
        if (cap == null) {
            throw new IllegalStateException("No cap configured for provider " + provider);
        }
        BigDecimal spent = monthlySpent(provider);
        BigDecimal projected = spent.add(estimatedCostEur);
        boolean ok = projected.compareTo(cap) <= 0;
        if (!ok) {
            log.warn("Budget cap reached for provider {}: spent={}€, estimated={}€, cap={}€",
                    provider, spent, estimatedCostEur, cap);
        }
        return ok;
    }

    /**
     * Enregistre un événement de coût IA. Idéalement appelé dans la même transaction
     * que la persistance du résultat métier (article créé, image stockée, etc.) pour
     * garantir l'atomicité check/write.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(IaProvider provider,
                       String model,
                       BlogUsageEventType eventType,
                       UUID blogArticleId,
                       int tokensInput,
                       int tokensOutput,
                       BigDecimal costEur) {
        BlogUsageEvent event = new BlogUsageEvent();
        event.setProvider(provider);
        event.setModel(model);
        event.setEventType(eventType);
        event.setBlogArticleId(blogArticleId);
        event.setTokensInput(tokensInput);
        event.setTokensOutput(tokensOutput);
        event.setCostEur(costEur);
        repository.save(event);
        log.info("Blog usage event recorded: provider={}, model={}, type={}, articleId={}, " +
                        "in={}, out={}, cost={}€",
                provider, model, eventType, blogArticleId, tokensInput, tokensOutput, costEur);
    }

    /**
     * Coût cumulé du mois civil en cours pour un provider donné.
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public BigDecimal monthlySpent(IaProvider provider) {
        Instant since = startOfCurrentMonth();
        return repository.sumCostByProviderSince(provider, since);
    }

    private static Instant startOfCurrentMonth() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
