package fr.ailegalcase.analysis;

/**
 * F-179 SF-179-01 — statut d'une vérification de référence jurisprudentielle.
 *
 * <ul>
 *   <li>{@link #VERIFIED} — l'arrêt existe et la position juridique qu'on lui
 *       prête est fidèle à son contenu réel.</li>
 *   <li>{@link #SUSPECT} — l'arrêt existe mais la position alléguée par le
 *       document est incohérente avec son contenu réel (cas le plus précieux :
 *       détection de mauvaise foi adverse).</li>
 *   <li>{@link #NOT_FOUND} — référence introuvable, y compris après le fallback
 *       web search (probable hallucination adverse).</li>
 *   <li>{@link #UNCERTAIN} — la vérification automatique n'a pas pu conclure
 *       (knowledge gap Claude probable + web search en échec ou non concluant) ;
 *       l'avocat doit vérifier manuellement. Statut de repli de sécurité : toute
 *       valeur inconnue retournée par le modèle y est ramenée plutôt que vers un
 *       faux {@code NOT_FOUND} qui discréditerait l'outil.</li>
 * </ul>
 */
public enum JurisprudenceCheckStatus {

    VERIFIED,
    SUSPECT,
    NOT_FOUND,
    UNCERTAIN;

    /**
     * Convertit une valeur libre (typiquement renvoyée par le modèle) en statut.
     * Toute valeur nulle, vide ou hors enum est ramenée à {@link #UNCERTAIN} —
     * jamais à {@code NOT_FOUND} (invariant anti-gadget F-179).
     */
    public static JurisprudenceCheckStatus fromModelValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNCERTAIN;
        }
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNCERTAIN;
        }
    }
}
