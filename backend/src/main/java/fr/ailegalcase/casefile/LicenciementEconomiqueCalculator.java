package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-DT-13-01 : calculateur du risque de requalification d'un licenciement pour motif
 * économique (FR — art. L.1233-3, L.1233-4, L.1233-5 et L.1233-45 Code du travail).
 *
 * <p>Évalue 3 axes indépendants notés 0-100, puis agrège en un score global et un verdict :</p>
 * <ul>
 *   <li><strong>Causalité (L.1233-3)</strong> : motif économique reconnu + preuves matérielles.</li>
 *   <li><strong>Critères d'ordre (L.1233-5)</strong> : âge, ancienneté, charges famille, qualités
 *       professionnelles obligatoires ; situation handicap optionnelle.</li>
 *   <li><strong>Reclassement (L.1233-4 + L.1233-45)</strong> : tentatives effectives + priorité
 *       de réembauche 1 an.</li>
 * </ul>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — le régime BE de licenciement collectif
 * (Loi Renault) est traité dans F-DT-14 PSE.</p>
 */
public final class LicenciementEconomiqueCalculator {

    /** Motifs économiques invoqués (L.1233-3). */
    public enum MotifEconomique {
        DIFFICULTES_ECONOMIQUES,
        MUTATIONS_TECHNOLOGIQUES,
        REORGANISATION_COMPETITIVITE,
        CESSATION_ACTIVITE,
        AUTRE
    }

    /** Preuves matérielles du motif économique. */
    public enum PreuveMotif {
        BAISSE_CHIFFRE_AFFAIRES,
        PERTES_EXPLOITATION,
        BAISSE_COMMANDES,
        BAISSE_TRESORERIE,
        BAISSE_ENE,
        MUTATION_TECHNOLOGIQUE_PROUVEE,
        RAPPORT_EXPERT,
        AUTRE
    }

    /** Critères d'ordre du licenciement (L.1233-5). 4 obligatoires + handicap optionnel. */
    public enum CritereOrdre {
        AGE,
        ANCIENNETE,
        CHARGES_FAMILLE,
        QUALITES_PROFESSIONNELLES,
        SITUATION_HANDICAP
    }

    /** Évaluation des qualités professionnelles. */
    public enum QualitesProf {
        EXCELLENT,
        BON,
        MOYEN,
        INSUFFISANT
    }

    /** Tentatives de reclassement (L.1233-4). */
    public enum TentativeReclassement {
        FORMATION_INTERNE,
        MUTATION_GROUPE,
        OFFRE_POSTE_GROUPE,
        OFFRE_POSTE_EXTERIEUR,
        AUCUNE
    }

    /** Verdict de risque de requalification (synthèse). */
    public enum VerdictRisque {
        FAIBLE,
        MOYENNE,
        ELEVEE
    }

    /** Critères d'ordre obligatoires au sens de L.1233-5. */
    private static final Set<CritereOrdre> CRITERES_OBLIGATOIRES = EnumSet.of(
            CritereOrdre.AGE,
            CritereOrdre.ANCIENNETE,
            CritereOrdre.CHARGES_FAMILLE,
            CritereOrdre.QUALITES_PROFESSIONNELLES
    );

    private LicenciementEconomiqueCalculator() {}

    /**
     * Calcule les 3 sous-scores, le score global et le verdict de risque de requalification.
     *
     * @param motifEconomiqueInvoque   motif invoqué par l'employeur (obligatoire)
     * @param preuvesMotif             preuves matérielles (peut être vide ; null traité comme vide)
     * @param criteresOrdreAppliques   critères d'ordre appliqués (peut être vide ; null traité comme vide)
     * @param salarieAge               âge du salarié (≥ 0)
     * @param salarieAncienneteMois    ancienneté en mois (≥ 0)
     * @param salarieChargesFamille    nombre de personnes à charge (≥ 0)
     * @param salarieQualitesProf      évaluation des qualités professionnelles (peut être null)
     * @param tentativesReclassement   tentatives de reclassement (peut être vide)
     * @param prioriteReembauchePropose priorité de réembauche L.1233-45 proposée
     * @param congeReclassementPropose congé de reclassement L.1233-71 proposé
     * @param dateNotification         date de notification du licenciement (obligatoire)
     * @param country                  pays du workspace (FRANCE attendu)
     * @return résultat structuré (3 scores + global + verdict + listes dérivées + messages)
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static LicenciementEconomiqueResult compute(MotifEconomique motifEconomiqueInvoque,
                                                       List<PreuveMotif> preuvesMotif,
                                                       List<CritereOrdre> criteresOrdreAppliques,
                                                       int salarieAge,
                                                       int salarieAncienneteMois,
                                                       int salarieChargesFamille,
                                                       QualitesProf salarieQualitesProf,
                                                       List<TentativeReclassement> tentativesReclassement,
                                                       boolean prioriteReembauchePropose,
                                                       boolean congeReclassementPropose,
                                                       LocalDate dateNotification,
                                                       String country) {
        if (motifEconomiqueInvoque == null) {
            throw new IllegalArgumentException("Motif économique invoqué requis");
        }
        if (criteresOrdreAppliques == null) {
            throw new IllegalArgumentException("Critères d'ordre appliqués requis (liste, peut être vide)");
        }
        if (tentativesReclassement == null) {
            throw new IllegalArgumentException("Tentatives de reclassement requises (liste, peut être vide)");
        }
        if (dateNotification == null) {
            throw new IllegalArgumentException("Date de notification requise");
        }
        if (salarieAge < 0) {
            throw new IllegalArgumentException("Âge du salarié doit être ≥ 0");
        }
        if (salarieAncienneteMois < 0) {
            throw new IllegalArgumentException("Ancienneté du salarié (mois) doit être ≥ 0");
        }
        if (salarieChargesFamille < 0) {
            throw new IllegalArgumentException("Charges famille doit être ≥ 0");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (Loi Renault) est traité dans F-DT-14 PSE.");
        }

        // Normalisation des listes (dédup + null-safe).
        List<PreuveMotif> preuves = dedupAndNullSafe(preuvesMotif);
        List<CritereOrdre> criteres = dedupAndNullSafe(criteresOrdreAppliques);
        List<TentativeReclassement> tentatives = dedupAndNullSafe(tentativesReclassement);

        // 1) Score causalité.
        int scoreCausalite = computeScoreCausalite(motifEconomiqueInvoque, preuves);

        // 2) Score critères d'ordre.
        List<CritereOrdre> manquants = new ArrayList<>();
        for (CritereOrdre obligatoire : CRITERES_OBLIGATOIRES) {
            if (!criteres.contains(obligatoire)) {
                manquants.add(obligatoire);
            }
        }
        int scoreCriteresOrdre = Math.max(0, 100 - 25 * manquants.size());
        boolean criteresOrdreObligatoiresOk = manquants.isEmpty();

        // 3) Score reclassement.
        int scoreReclassement = computeScoreReclassement(tentatives, prioriteReembauchePropose);
        boolean obligationReclassementRespectee = isObligationReclassementRespectee(tentatives);

        // 4) Score global = moyenne arithmétique arrondie.
        int scoreGlobal = (int) Math.round((scoreCausalite + scoreCriteresOrdre + scoreReclassement) / 3.0);

        VerdictRisque verdict = verdict(scoreGlobal);

        String baseJuridique = "Art. L.1233-3 + L.1233-4 + L.1233-5 + L.1233-45 Code du travail";
        String formule = String.format(Locale.ROOT,
                "Causalité %d + Critères ordre %d + Reclassement %d = global %d → risque requalification %s",
                scoreCausalite, scoreCriteresOrdre, scoreReclassement, scoreGlobal, verdict.name());

        List<String> messages = buildMessages(motifEconomiqueInvoque, preuves, manquants, tentatives,
                prioriteReembauchePropose, congeReclassementPropose, salarieQualitesProf);

        return new LicenciementEconomiqueResult(
                motifEconomiqueInvoque,
                preuves,
                criteres,
                salarieAge,
                salarieAncienneteMois,
                salarieChargesFamille,
                salarieQualitesProf,
                tentatives,
                prioriteReembauchePropose,
                congeReclassementPropose,
                dateNotification,
                countryNormalized,
                scoreCausalite,
                scoreCriteresOrdre,
                scoreReclassement,
                scoreGlobal,
                verdict,
                manquants,
                criteresOrdreObligatoiresOk,
                obligationReclassementRespectee,
                baseJuridique,
                formule,
                messages
        );
    }

    /**
     * Score causalité — 0 si motif AUTRE.
     * Sinon : 20 base + 30 si ≥ 2 preuves + 30 si ≥ 4 preuves + 20 si RAPPORT_EXPERT (cap 100).
     */
    private static int computeScoreCausalite(MotifEconomique motif, List<PreuveMotif> preuves) {
        if (motif == MotifEconomique.AUTRE) {
            return 0;
        }
        int score = 20;
        int n = preuves.size();
        if (n >= 2) score += 30;
        if (n >= 4) score += 30;
        if (preuves.contains(PreuveMotif.RAPPORT_EXPERT)) score += 20;
        return Math.min(100, score);
    }

    /**
     * Score reclassement.
     * - 0 si liste vide ou contient uniquement AUCUNE.
     * - 1 tentative effective : 40.
     * - ≥ 2 tentatives effectives : 70.
     * - + 30 si priorité réembauche L.1233-45 proposée (cap 100).
     */
    private static int computeScoreReclassement(List<TentativeReclassement> tentatives,
                                                boolean prioriteReembauche) {
        long effectives = tentatives.stream()
                .filter(t -> t != TentativeReclassement.AUCUNE)
                .count();
        int score;
        if (tentatives.isEmpty() || effectives == 0) {
            score = 0;
        } else if (effectives == 1) {
            score = 40;
        } else {
            score = 70;
        }
        if (prioriteReembauche) {
            score += 30;
        }
        return Math.min(100, score);
    }

    /** Obligation de reclassement respectée si ≥ 2 tentatives effectives (hors AUCUNE). */
    private static boolean isObligationReclassementRespectee(List<TentativeReclassement> tentatives) {
        long effectives = tentatives.stream()
                .filter(t -> t != TentativeReclassement.AUCUNE)
                .count();
        return effectives >= 2;
    }

    private static VerdictRisque verdict(int scoreGlobal) {
        if (scoreGlobal >= 70) return VerdictRisque.FAIBLE;
        if (scoreGlobal >= 40) return VerdictRisque.MOYENNE;
        return VerdictRisque.ELEVEE;
    }

    private static List<String> buildMessages(MotifEconomique motif,
                                              List<PreuveMotif> preuves,
                                              List<CritereOrdre> manquants,
                                              List<TentativeReclassement> tentatives,
                                              boolean prioriteReembauche,
                                              boolean congeReclassement,
                                              QualitesProf qualitesProf) {
        List<String> msgs = new ArrayList<>();

        if (motif == MotifEconomique.AUTRE) {
            msgs.add("Motif « Autre » : risque élevé de requalification — préciser un motif "
                    + "économique reconnu (L.1233-3) sous peine de licenciement sans cause réelle et sérieuse.");
        } else if (preuves.isEmpty()) {
            msgs.add("Aucune preuve matérielle invoquée : la cause économique ne pourra pas être "
                    + "démontrée devant le Conseil de prud'hommes.");
        } else if (preuves.size() < 2) {
            msgs.add("Une seule preuve matérielle : renforcer le dossier (au moins 2 trimestres "
                    + "consécutifs de baisse, comptabilité, rapport d'expert).");
        }

        if (preuves.contains(PreuveMotif.RAPPORT_EXPERT)) {
            msgs.add("Rapport d'expert présent : très fort indice probatoire en cas de contentieux.");
        }

        if (!manquants.isEmpty()) {
            msgs.add("Critères d'ordre obligatoires manquants (L.1233-5) : "
                    + manquants + ". Risque de nullité partielle de la procédure.");
        } else {
            msgs.add("Les 4 critères d'ordre obligatoires L.1233-5 sont appliqués (âge, ancienneté, "
                    + "charges famille, qualités professionnelles).");
        }

        long effectives = tentatives.stream()
                .filter(t -> t != TentativeReclassement.AUCUNE)
                .count();
        if (effectives == 0) {
            msgs.add("Aucune tentative de reclassement : violation directe de l'obligation L.1233-4 — "
                    + "licenciement sans cause réelle et sérieuse quasi automatique.");
        } else if (effectives == 1) {
            msgs.add("Une seule tentative de reclassement : insuffisant en jurisprudence — "
                    + "recherche dans le groupe + bassin d'emploi attendue.");
        } else {
            msgs.add("Reclassement : " + effectives + " tentatives effectives — obligation L.1233-4 "
                    + "respectée à première vue.");
        }

        if (prioriteReembauche) {
            msgs.add("Priorité de réembauche 1 an proposée (L.1233-45) ✓");
        } else {
            msgs.add("Priorité de réembauche L.1233-45 non proposée : à corriger immédiatement — "
                    + "la mention doit figurer dans la lettre de licenciement.");
        }

        if (!congeReclassement) {
            msgs.add("Congé de reclassement L.1233-71 non proposé : obligatoire si l'entreprise "
                    + "≥ 1 000 salariés (sinon CSP — Contrat de sécurisation professionnelle).");
        }

        if (qualitesProf == QualitesProf.INSUFFISANT) {
            msgs.add("Qualités professionnelles évaluées « insuffisant » : attention à ne pas glisser "
                    + "vers un motif disciplinaire déguisé qui priverait le motif économique d'effet.");
        }

        return msgs;
    }

    /** Déduplique en préservant l'ordre, null-safe. */
    private static <T> List<T> dedupAndNullSafe(List<T> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        Set<T> seen = new LinkedHashSet<>();
        for (T t : input) {
            if (t != null) seen.add(t);
        }
        return List.copyOf(seen);
    }
}
