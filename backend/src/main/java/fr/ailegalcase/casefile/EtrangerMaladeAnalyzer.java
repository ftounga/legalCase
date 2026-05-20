package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-214-01 : analyseur de l'éligibilité à la protection médicale L. 425-9 CESEDA
 * (étranger malade — état de santé nécessitant soins indisponibles dans le pays
 * d'origine, avis collège médical OFII) — outil single-country FR.
 *
 * <p>Source juridique :
 * <ul>
 *   <li>L. 425-9 CESEDA (ancien L. 313-11 11°) — Ordonnance 2020-1733 du 16/12/2020</li>
 *   <li>R. 425-9 à R. 425-16 CESEDA — procédure collège médical OFII</li>
 *   <li>Loi 7 mars 2016 (Collomb) : transfert compétence OFPRA → OFII pour l'avis médical</li>
 *   <li>CE 7 avril 2010, n° 301640 — critères d'appréciation de l'accessibilité des soins</li>
 *   <li>Délai recours TA : 2 mois de droit commun CJA (R. 421-1 CJA)</li>
 * </ul>
 */
public final class EtrangerMaladeAnalyzer {

    public static final String VERDICT_ELIGIBLE_PROBABLE = "ELIGIBLE_PROBABLE";
    public static final String VERDICT_ELIGIBLE_SOUS_RESERVE = "ELIGIBLE_SOUS_RESERVE";
    public static final String VERDICT_NON_ELIGIBLE = "NON_ELIGIBLE";
    public static final String VERDICT_EN_ATTENTE_AVIS_OFII = "EN_ATTENTE_AVIS_OFII";

    public static final String AVIS_FAVORABLE = "FAVORABLE";
    public static final String AVIS_DEFAVORABLE = "DEFAVORABLE";
    public static final String AVIS_EN_ATTENTE = "EN_ATTENTE";

    public static final Set<String> AVIS_VALIDES = Set.of(
            AVIS_FAVORABLE, AVIS_DEFAVORABLE, AVIS_EN_ATTENTE);

    /** Délai de recours TA contre décision préfectorale fondée sur avis OFII défavorable (2 mois CJA). */
    public static final int DELAI_RECOURS_TA_MOIS = 2;

    private static final String BASE_JURIDIQUE =
            "CESEDA L. 425-9 ; R. 425-9 à R. 425-16 ; Loi 7 mars 2016 (avis OFII) ; "
            + "CE 7 avril 2010 n° 301640 (accessibilité des soins) ; R. 421-1 CJA (délai 2 mois TA)";

    private EtrangerMaladeAnalyzer() {}

    /**
     * Analyse l'éligibilité à la protection L. 425-9.
     *
     * @param pathologiePrincipale  description de la pathologie (requis, ≤ 500 chars)
     * @param paysOrigine           pays d'origine (requis)
     * @param traitementDisponible  le traitement est-il disponible dans le pays d'origine
     * @param avisOFII              avis rendu par le collège OFII (FAVORABLE/DEFAVORABLE/EN_ATTENTE/null)
     * @param dateAvisOFII          date de l'avis OFII (requis si DEFAVORABLE)
     * @return résultat de l'analyse
     */
    public static EtrangerMaladeResult analyze(String pathologiePrincipale,
                                                String paysOrigine,
                                                boolean traitementDisponible,
                                                String avisOFII,
                                                LocalDate dateAvisOFII) {
        validateInputs(pathologiePrincipale, paysOrigine, avisOFII, dateAvisOFII);

        List<String> criteesNonRemplis = new ArrayList<>();
        String verdict;
        LocalDate delaiRecoursTA = null;
        String motifRecours = null;

        // Critère 1 : état de santé grave
        boolean critere1 = pathologiePrincipale != null && !pathologiePrincipale.isBlank();
        if (!critere1) {
            criteesNonRemplis.add("PATHOLOGIE_NON_RENSEIGNEE");
        }

        // Critère 2 : traitement indisponible dans le pays d'origine
        boolean critere2 = !traitementDisponible;
        if (!critere2) {
            criteesNonRemplis.add("TRAITEMENT_DISPONIBLE_PAYS_ORIGINE");
        }

        // Détermination du verdict selon l'avis OFII
        if (AVIS_EN_ATTENTE.equals(avisOFII) || avisOFII == null) {
            verdict = VERDICT_EN_ATTENTE_AVIS_OFII;
        } else if (AVIS_DEFAVORABLE.equals(avisOFII)) {
            criteesNonRemplis.add("AVIS_OFII_DEFAVORABLE");
            // Délai de recours TA = date avis + 2 mois
            if (dateAvisOFII != null) {
                delaiRecoursTA = dateAvisOFII.plusMonths(DELAI_RECOURS_TA_MOIS);
                motifRecours = buildMotifRecours(pathologiePrincipale, paysOrigine, dateAvisOFII);
            }
            // Même avec avis défavorable, on peut être ELIGIBLE_PROBABLE si les critères médicaux sont là
            verdict = criteesNonRemplis.contains("PATHOLOGIE_NON_RENSEIGNEE")
                    || criteesNonRemplis.contains("TRAITEMENT_DISPONIBLE_PAYS_ORIGINE")
                    ? VERDICT_NON_ELIGIBLE : VERDICT_ELIGIBLE_PROBABLE;
        } else if (AVIS_FAVORABLE.equals(avisOFII)) {
            verdict = criteesNonRemplis.isEmpty()
                    ? VERDICT_ELIGIBLE_PROBABLE : VERDICT_ELIGIBLE_SOUS_RESERVE;
        } else {
            verdict = criteesNonRemplis.isEmpty()
                    ? VERDICT_ELIGIBLE_PROBABLE
                    : criteesNonRemplis.size() == 1 ? VERDICT_ELIGIBLE_SOUS_RESERVE : VERDICT_NON_ELIGIBLE;
        }

        return new EtrangerMaladeResult(
                pathologiePrincipale,
                paysOrigine,
                traitementDisponible,
                avisOFII,
                dateAvisOFII,
                verdict,
                delaiRecoursTA,
                motifRecours,
                criteesNonRemplis,
                BASE_JURIDIQUE);
    }

    private static void validateInputs(String pathologiePrincipale,
                                        String paysOrigine,
                                        String avisOFII,
                                        LocalDate dateAvisOFII) {
        if (pathologiePrincipale == null || pathologiePrincipale.isBlank()) {
            throw new IllegalArgumentException("pathologiePrincipale est requise");
        }
        if (pathologiePrincipale.length() > 500) {
            throw new IllegalArgumentException("pathologiePrincipale dépasse 500 caractères");
        }
        if (paysOrigine == null || paysOrigine.isBlank()) {
            throw new IllegalArgumentException("paysOrigine est requis");
        }
        if (avisOFII != null && !AVIS_VALIDES.contains(avisOFII)) {
            throw new IllegalArgumentException(
                    "avisOFII inconnu — valeurs attendues : " + AVIS_VALIDES);
        }
        if (AVIS_DEFAVORABLE.equals(avisOFII) && dateAvisOFII == null) {
            throw new IllegalArgumentException(
                    "dateAvisOFII est requise si avisOFII = DEFAVORABLE");
        }
        if (dateAvisOFII != null && dateAvisOFII.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dateAvisOFII ne peut pas être dans le futur");
        }
    }

    private static String buildMotifRecours(String pathologie, String paysOrigine, LocalDate dateAvis) {
        return String.format(
                "Recours contre l'avis défavorable du collège médical OFII en date du %s. "
                + "Le demandeur souffre de : %s. "
                + "Le traitement nécessaire est indisponible ou inaccessible au %s, "
                + "ce qui constitue une atteinte au droit à la vie (art. 2 CEDH) et "
                + "au respect de la dignité humaine. "
                + "Critères L. 425-9 CESEDA remplis : état de santé grave + indisponibilité du traitement. "
                + "Délai de recours TA : 2 mois à compter de la décision préfectorale (R. 421-1 CJA).",
                dateAvis, pathologie, paysOrigine);
    }
}
