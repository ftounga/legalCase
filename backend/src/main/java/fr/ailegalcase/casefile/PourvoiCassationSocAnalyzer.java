package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-05 : analyseur d'un pourvoi en cassation devant la chambre sociale de
 * la Cour de cassation. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Trois axes d'analyse (invariant CLAUDE.md — un outil = une situation
 * métier : le contentieux de cassation sociale) :
 * <ul>
 *   <li><b>Délai</b> : 2 mois à compter de la notification de l'arrêt de la
 *       Cour d'appel (art. 612 CPC). Produit la date limite, les jours restants
 *       et le verdict délai (DELAI_OUVERT / DELAI_URGENT ≤ 14 j /
 *       DELAI_EXPIRE).</li>
 *   <li><b>Cas d'ouverture</b> : pour chaque cas invoqué (art. 604 CPC),
 *       restitue son libellé, sa base juridique et sa force probatoire
 *       (FORTE / MOYENNE / FAIBLE).</li>
 *   <li><b>Filtre NPC (non-admission)</b> : si aucun cas n'a une force FORTE et
 *       qu'aucun moyen sérieux n'est identifié → risque ELEVE (art. 1014 CPC,
 *       procédure de non-admission par la formation restreinte).</li>
 * </ul>
 *
 * <p>La représentation par un avocat aux Conseils est obligatoire (art. 973
 * CPC) : son absence produit un item bloquant dans la checklist.
 *
 * <p>Sources :
 * <ul>
 *   <li>art. 901 et s. CPC — déclaration de pourvoi en cassation ;</li>
 *   <li>art. 604 CPC — le pourvoi tend à censurer la non-conformité de l'arrêt
 *       aux règles de droit ;</li>
 *   <li>art. 612 CPC — délai du pourvoi : 2 mois ;</li>
 *   <li>art. 973 CPC — représentation obligatoire par avocat aux Conseils ;</li>
 *   <li>art. 1014 CPC — procédure de non-admission (filtre NPC).</li>
 * </ul>
 */
public final class PourvoiCassationSocAnalyzer {

    /** Délai du pourvoi en cassation : 2 mois (art. 612 CPC). */
    private static final int DELAI_POURVOI_MOIS = 2;

    /** Seuil d'urgence : 14 jours ou moins avant l'expiration du délai. */
    private static final int SEUIL_URGENCE_JOURS = 14;

    private static final String BASE_JURIDIQUE =
            "art. 901 et s. CPC (déclaration de pourvoi) ; art. 604 CPC (cas "
                    + "d'ouverture — non-conformité aux règles de droit) ; art. 612 CPC "
                    + "(délai de 2 mois) ; art. 973 CPC (représentation obligatoire par "
                    + "avocat aux Conseils) ; art. 1014 CPC (procédure de non-admission)";

    private PourvoiCassationSocAnalyzer() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static PourvoiCassationSocResult analyze(LocalDate dateNotificationArret,
                                                    List<PourvoiCassationSocCasOuverture> casOuverture,
                                                    Boolean representationAvocatCassation,
                                                    Boolean moyenSerieuxIdentifie) {
        return analyze(dateNotificationArret, casOuverture, representationAvocatCassation,
                moyenSerieuxIdentifie, LocalDate.now());
    }

    /**
     * Analyse le pourvoi et détermine le verdict délai, la force des cas
     * d'ouverture, le risque de non-admission et le verdict global.
     *
     * @param today date de référence injectée (testabilité).
     */
    public static PourvoiCassationSocResult analyze(LocalDate dateNotificationArret,
                                                    List<PourvoiCassationSocCasOuverture> casOuverture,
                                                    Boolean representationAvocatCassation,
                                                    Boolean moyenSerieuxIdentifie,
                                                    LocalDate today) {
        validate(dateNotificationArret, casOuverture, today);

        boolean avocatCassation = representationAvocatCassation != null && representationAvocatCassation;
        boolean moyenSerieux = moyenSerieuxIdentifie != null && moyenSerieuxIdentifie;

        // ── Délai (art. 612 CPC) ──
        LocalDate dateLimitePourvoi = dateNotificationArret.plusMonths(DELAI_POURVOI_MOIS);
        long joursRestants = ChronoUnit.DAYS.between(today, dateLimitePourvoi);
        PourvoiCassationSocVerdictDelai verdictDelai;
        if (joursRestants < 0) {
            verdictDelai = PourvoiCassationSocVerdictDelai.DELAI_EXPIRE;
        } else if (joursRestants <= SEUIL_URGENCE_JOURS) {
            verdictDelai = PourvoiCassationSocVerdictDelai.DELAI_URGENT;
        } else {
            verdictDelai = PourvoiCassationSocVerdictDelai.DELAI_OUVERT;
        }

        // ── Cas d'ouverture (art. 604 CPC) ──
        List<PourvoiCassationSocCasOuvertureAnalyse> analyses = new ArrayList<>();
        boolean hasForte = false;
        boolean hasMoyenne = false;
        for (PourvoiCassationSocCasOuverture cas : casOuverture) {
            analyses.add(new PourvoiCassationSocCasOuvertureAnalyse(
                    cas, cas.libelle(), cas.baseJuridique(), cas.force()));
            if (cas.force() == PourvoiCassationSocForce.FORTE) {
                hasForte = true;
            } else if (cas.force() == PourvoiCassationSocForce.MOYENNE) {
                hasMoyenne = true;
            }
        }

        // ── Filtre NPC / non-admission (art. 1014 CPC) ──
        PourvoiCassationSocRisqueNonAdmission risqueNonAdmission;
        if (hasForte || moyenSerieux) {
            risqueNonAdmission = PourvoiCassationSocRisqueNonAdmission.FAIBLE;
        } else if (hasMoyenne) {
            risqueNonAdmission = PourvoiCassationSocRisqueNonAdmission.MODERE;
        } else {
            risqueNonAdmission = PourvoiCassationSocRisqueNonAdmission.ELEVE;
        }

        // ── Verdict global ──
        PourvoiCassationSocVerdict verdict;
        if (verdictDelai == PourvoiCassationSocVerdictDelai.DELAI_EXPIRE) {
            verdict = PourvoiCassationSocVerdict.DELAI_EXPIRE;
        } else {
            verdict = switch (risqueNonAdmission) {
                case FAIBLE -> PourvoiCassationSocVerdict.POURVOI_RECOMMANDE;
                case MODERE -> PourvoiCassationSocVerdict.POURVOI_RISQUE;
                case ELEVE -> PourvoiCassationSocVerdict.POURVOI_DECONSEILLE;
            };
        }

        List<PourvoiCassationSocChecklistItem> checklist =
                buildChecklist(avocatCassation, verdictDelai, risqueNonAdmission);

        return new PourvoiCassationSocResult(
                dateNotificationArret,
                dateLimitePourvoi,
                joursRestants,
                verdictDelai,
                analyses,
                risqueNonAdmission,
                avocatCassation,
                moyenSerieux,
                verdict,
                checklist,
                BASE_JURIDIQUE);
    }

    private static List<PourvoiCassationSocChecklistItem> buildChecklist(
            boolean avocatCassation,
            PourvoiCassationSocVerdictDelai verdictDelai,
            PourvoiCassationSocRisqueNonAdmission risqueNonAdmission) {
        List<PourvoiCassationSocChecklistItem> items = new ArrayList<>();

        // Item bloquant : représentation obligatoire par avocat aux Conseils.
        items.add(new PourvoiCassationSocChecklistItem(
                avocatCassation
                        ? "Avocat au Conseil d'État et à la Cour de cassation constitué "
                                + "(représentation obligatoire acquise)"
                        : "Constituer un avocat au Conseil d'État et à la Cour de cassation : "
                                + "la représentation est OBLIGATOIRE pour former le pourvoi "
                                + "(information actuellement manquante)",
                true,
                !avocatCassation,
                "art. 973 CPC"));

        // Déclaration de pourvoi dans le délai.
        items.add(new PourvoiCassationSocChecklistItem(
                verdictDelai == PourvoiCassationSocVerdictDelai.DELAI_EXPIRE
                        ? "Délai de pourvoi de 2 mois EXPIRÉ : le pourvoi est irrecevable "
                                + "(forclusion)"
                        : "Former la déclaration de pourvoi au greffe de la Cour de cassation "
                                + "dans le délai de 2 mois à compter de la notification de l'arrêt",
                true,
                verdictDelai == PourvoiCassationSocVerdictDelai.DELAI_EXPIRE,
                "art. 612 CPC ; art. 974 et s. CPC"));

        // Mémoire ampliatif.
        items.add(new PourvoiCassationSocChecklistItem(
                "Déposer le mémoire ampliatif exposant les moyens de cassation dans le "
                        + "délai imparti (4 mois à compter du pourvoi)",
                true,
                false,
                "art. 978 CPC"));

        // Anti-filtre NPC : consolider le caractère sérieux des moyens.
        items.add(new PourvoiCassationSocChecklistItem(
                risqueNonAdmission == PourvoiCassationSocRisqueNonAdmission.ELEVE
                        ? "Risque de NON-ADMISSION élevé : consolider le caractère sérieux du "
                                + "moyen avant de former le pourvoi (filtre NPC)"
                        : "Vérifier le caractère sérieux des moyens au regard de la procédure "
                                + "de non-admission",
                false,
                false,
                "art. 1014 CPC"));

        return items;
    }

    private static void validate(LocalDate dateNotificationArret,
                                 List<PourvoiCassationSocCasOuverture> casOuverture,
                                 LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateNotificationArret == null) {
            throw new IllegalArgumentException("dateNotificationArret est requise");
        }
        if (dateNotificationArret.isAfter(today)) {
            throw new IllegalArgumentException("dateNotificationArret ne peut pas être dans le futur");
        }
        if (casOuverture == null || casOuverture.isEmpty()) {
            throw new IllegalArgumentException("casOuverture doit contenir au moins un cas d'ouverture");
        }
        if (casOuverture.contains(null)) {
            throw new IllegalArgumentException("casOuverture contient une valeur inconnue");
        }
    }
}
