package fr.ailegalcase.analysis;

/**
 * Type d'événement de consommation IA.
 *
 * <p>F-15 / F-34 — codes d'origine (user-level) : pipeline IA principal + chat.
 * <p>F-257 — codes system-level ({@code SYSTEM_*}) ajoutés pour les appels Anthropic
 * exécutés hors contexte user direct (crons, blog SEO, super-admin, vision, conclusions,
 * détection de pièces, semantic diff, etc.). Le gate
 * {@code PlanLimitService.isMonthlyTokenBudgetExceeded} est <b>skip</b> pour les
 * codes system-level, mais l'enregistrement dans {@code usage_events} reste
 * <b>obligatoire</b> (avec {@code userId=null}) pour traçabilité globale du coût Anthropic.
 */
public enum JobType {
    // ── User-level (gate token + record) ─────────────────────────────────
    CHUNK_ANALYSIS,
    DOCUMENT_ANALYSIS,
    CASE_ANALYSIS,
    QUESTION_GENERATION,
    ENRICHED_ANALYSIS,
    CHAT_MESSAGE,

    // ── System-level (skip gate user, record obligatoire) ────────────────
    SYSTEM_REFERENTIAL_CHECK,
    SYSTEM_BLOG_GENERATION,
    SYSTEM_JP_BOOTSTRAP,
    SYSTEM_HELP_CHAT,
    SYSTEM_VISION_ENRICHMENT,
    SYSTEM_PIECE_DETECTION,
    SYSTEM_STYLE_LEARNING,
    SYSTEM_CASE_CONCLUSION,
    SYSTEM_SEMANTIC_DIFF,
    SYSTEM_JURISPRUDENCE_VERIFICATION,
    SYSTEM_CHAT_SUMMARY;

    /** F-257 — true pour les codes {@code SYSTEM_*} (gate user skip, record obligatoire). */
    public boolean isSystemLevel() {
        return name().startsWith("SYSTEM_");
    }

    /** F-257 — true pour les codes user-level historiques (gate token + record). */
    public boolean isUserLevel() {
        return !isSystemLevel();
    }
}
