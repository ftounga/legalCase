package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;

import static fr.ailegalcase.casefile.CasePhaseType.*;

/**
 * F-283 / SF-283-03 — catalogue autoritatif des phases pertinentes par
 * (domaine légal × pays). Chaque combinaison renvoie une <b>liste ordonnée</b>
 * de {@link CasePhaseSuggestion} (type + libellé par défaut éditable).
 *
 * <p>L'enum {@link CasePhaseType} reste la valeur stockée (rétro-compatible) ;
 * le catalogue ne fait que <i>suggérer</i> un sous-ensemble ordonné + un libellé.
 * L'avocat garde la liberté de renommer le libellé et de choisir tout type.
 *
 * <p><b>Fallback</b> : toute combinaison non couverte (domaine ou pays inconnu)
 * retombe sur la liste générique civile FR travail (les 8 phases historiques) —
 * garantit qu'aucun dossier ne se retrouve sans suggestions et ne casse rien.
 *
 * <p>Pays : {@code "FRANCE"} / {@code "BELGIQUE"} (cf. {@code workspace.country}).
 * Domaine : {@code DROIT_DU_TRAVAIL} / {@code DROIT_FAMILLE} / {@code DROIT_IMMIGRATION}.
 */
public final class CasePhaseSuggestionCatalog {

    private CasePhaseSuggestionCatalog() {}

    private static CasePhaseSuggestion s(CasePhaseType type, String defaultLabel) {
        return new CasePhaseSuggestion(type, defaultLabel);
    }

    // ── Catalogues par (domaine, pays) ───────────────────────────────────────

    private static final List<CasePhaseSuggestion> TRAVAIL_FRANCE = List.of(
            s(SAISINE, "Saisine du Conseil de prud'hommes"),
            s(CONCILIATION, "Audience de conciliation (BCO)"),
            s(MISE_EN_ETAT, "Mise en état"),
            s(FOND, "Audience de jugement (bureau de jugement)"),
            s(JUGEMENT, "Jugement"),
            s(APPEL, "Appel (Cour d'appel — chambre sociale)"),
            s(CASSATION, "Pourvoi en cassation"),
            s(EXECUTION, "Exécution"));

    private static final List<CasePhaseSuggestion> TRAVAIL_BELGIQUE = List.of(
            s(INTRODUCTION, "Introduction (citation / requête)"),
            s(MISE_EN_ETAT, "Mise en état (art. 747 C. jud.)"),
            s(FOND, "Audience de plaidoiries (Tribunal du travail)"),
            s(JUGEMENT, "Jugement"),
            s(APPEL, "Appel (Cour du travail)"),
            s(CASSATION, "Pourvoi en cassation"),
            s(EXECUTION, "Exécution"));

    private static final List<CasePhaseSuggestion> IMMIGRATION_FRANCE = List.of(
            s(RECOURS_PREALABLE, "Recours gracieux / hiérarchique"),
            s(TRIBUNAL_ADMINISTRATIF, "Recours contentieux (Tribunal administratif)"),
            s(CNDA, "Cour nationale du droit d'asile (asile)"),
            s(COUR_ADMINISTRATIVE_APPEL, "Appel (Cour administrative d'appel)"),
            s(CONSEIL_ETAT, "Cassation (Conseil d'État)"),
            s(EXECUTION, "Exécution"));

    private static final List<CasePhaseSuggestion> IMMIGRATION_BELGIQUE = List.of(
            s(RECOURS_PREALABLE, "Recours (Office des étrangers)"),
            s(CCE, "Conseil du Contentieux des Étrangers"),
            s(CONSEIL_ETAT, "Conseil d'État (cassation administrative)"),
            s(EXECUTION, "Exécution"));

    private static final List<CasePhaseSuggestion> FAMILLE_FRANCE = List.of(
            s(SAISINE, "Requête / assignation"),
            s(CONCILIATION, "Audience d'orientation / tentative de conciliation"),
            s(MISE_EN_ETAT, "Mise en état"),
            s(FOND, "Audience de plaidoiries"),
            s(JUGEMENT, "Jugement"),
            s(APPEL, "Appel (Cour d'appel)"),
            s(CASSATION, "Pourvoi en cassation"),
            s(EXECUTION, "Exécution"));

    private static final List<CasePhaseSuggestion> FAMILLE_BELGIQUE = List.of(
            s(INTRODUCTION, "Introduction"),
            s(CONCILIATION, "Chambre de règlement amiable"),
            s(MISE_EN_ETAT, "Mise en état"),
            s(FOND, "Audience de plaidoiries (Tribunal de la famille)"),
            s(JUGEMENT, "Jugement"),
            s(APPEL, "Appel (Cour d'appel)"),
            s(CASSATION, "Pourvoi en cassation"),
            s(EXECUTION, "Exécution"));

    /**
     * Fallback générique = liste civile FR travail (les 8 phases historiques).
     * Garantit qu'une combinaison (domaine, pays) inconnue ne casse rien.
     */
    static final List<CasePhaseSuggestion> FALLBACK = TRAVAIL_FRANCE;

    /** Clé interne (domaine, pays normalisés). */
    private static String key(String legalDomain, String country) {
        return legalDomain + "|" + country;
    }

    private static final Map<String, List<CasePhaseSuggestion>> CATALOG = Map.of(
            key("DROIT_DU_TRAVAIL", "FRANCE"), TRAVAIL_FRANCE,
            key("DROIT_DU_TRAVAIL", "BELGIQUE"), TRAVAIL_BELGIQUE,
            key("DROIT_IMMIGRATION", "FRANCE"), IMMIGRATION_FRANCE,
            key("DROIT_IMMIGRATION", "BELGIQUE"), IMMIGRATION_BELGIQUE,
            key("DROIT_FAMILLE", "FRANCE"), FAMILLE_FRANCE,
            key("DROIT_FAMILLE", "BELGIQUE"), FAMILLE_BELGIQUE);

    /**
     * Liste ordonnée de phases suggérées pour un (domaine, pays).
     * Combinaison inconnue / null → {@link #FALLBACK} (liste civile FR travail).
     */
    public static List<CasePhaseSuggestion> forDomainAndCountry(String legalDomain, String country) {
        if (legalDomain == null || country == null) {
            return FALLBACK;
        }
        return CATALOG.getOrDefault(
                key(legalDomain.trim().toUpperCase(), country.trim().toUpperCase()),
                FALLBACK);
    }
}
