package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-214-41 — analyseur de validité d'un retrait de titre de séjour pour fraude
 * (art. L. 412-7 CESEDA). Outil single-country FR.
 *
 * <p>Le retrait d'un titre de séjour pour fraude doit être précédé d'une
 * procédure contradictoire : l'étranger doit être mis à même de présenter ses
 * observations (mise en demeure / invitation préalable). L'absence de
 * contradictoire constitue un vice de procédure substantiel. L'analyseur
 * identifie les vices de procédure, les moyens de contestation au fond selon le
 * motif invoqué, et calcule le délai de recours devant le tribunal administratif
 * (2 mois à compter de la notification du retrait).
 *
 * <p>Sources :
 * <ul>
 *   <li>L. 412-7 CESEDA — retrait / refus pour fraude (ex-L. 313-5) ;</li>
 *   <li>L. 411-5 CESEDA — procédure contradictoire préalable ;</li>
 *   <li>CE 15 juillet 2004 n° 258040 — conditions du retrait (mariage gris) ;</li>
 *   <li>CE 23 octobre 2009 n° 317866 — retrait pour documentation frauduleuse.</li>
 * </ul>
 */
public final class RetraitTitreFraudeAnalyzer {

    /** Délai de saisine du tribunal administratif contre le retrait — 2 mois. */
    static final int DELAI_RECOURS_TA_MOIS = 2;

    /** Seuil (exclusif) en jours restants en deçà duquel le statut bascule en URGENT. */
    static final int SEUIL_URGENT_JOURS = 15;

    private static final String BASE_JURIDIQUE =
            "Art. L. 412-7 CESEDA (retrait/refus de titre pour fraude, ex-L. 313-5) ; "
                    + "L. 411-5 CESEDA (procédure contradictoire préalable) ; "
                    + "CE 15 juillet 2004 n° 258040 (mariage gris) ; "
                    + "CE 23 octobre 2009 n° 317866 (documentation frauduleuse) ; "
                    + "recours pour excès de pouvoir devant le tribunal administratif "
                    + "dans un délai de 2 mois";

    private RetraitTitreFraudeAnalyzer() {
    }

    /** Surcharge utilisant la date du jour système comme référence. */
    public static RetraitTitreFraudeResult analyze(LocalDate dateRetrait,
                                                   RetraitTitreFraudeMotifEnum motifRetrait,
                                                   boolean miseEnDemeurePrealable,
                                                   LocalDate dateMiseEnDemeure) {
        return analyze(dateRetrait, motifRetrait, miseEnDemeurePrealable, dateMiseEnDemeure,
                LocalDate.now());
    }

    /**
     * Analyse la validité du retrait et calcule le délai et le statut du recours.
     *
     * @param today date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     */
    public static RetraitTitreFraudeResult analyze(LocalDate dateRetrait,
                                                   RetraitTitreFraudeMotifEnum motifRetrait,
                                                   boolean miseEnDemeurePrealable,
                                                   LocalDate dateMiseEnDemeure,
                                                   LocalDate today) {
        validate(dateRetrait, motifRetrait, today);

        LocalDate delaiRecoursTA = dateRetrait.plusMonths(DELAI_RECOURS_TA_MOIS);

        long joursRestants = ChronoUnit.DAYS.between(today, delaiRecoursTA);
        RetraitTitreFraudeStatut statut;
        if (joursRestants < 0) {
            statut = RetraitTitreFraudeStatut.PRESCRIT;
        } else if (joursRestants < SEUIL_URGENT_JOURS) {
            statut = RetraitTitreFraudeStatut.URGENT;
        } else {
            statut = RetraitTitreFraudeStatut.RECOURS_POSSIBLE;
        }
        boolean recoursPossible = statut != RetraitTitreFraudeStatut.PRESCRIT;

        List<String> vicesDeProcedure = buildVicesDeProcedure(
                miseEnDemeurePrealable, dateMiseEnDemeure, dateRetrait);
        List<String> motifsContestation = buildMotifsContestation(motifRetrait);

        return new RetraitTitreFraudeResult(
                dateRetrait,
                motifRetrait,
                miseEnDemeurePrealable,
                dateMiseEnDemeure,
                vicesDeProcedure,
                motifsContestation,
                delaiRecoursTA,
                statut,
                recoursPossible,
                buildRecommandation(statut, vicesDeProcedure),
                BASE_JURIDIQUE);
    }

    private static List<String> buildVicesDeProcedure(boolean miseEnDemeurePrealable,
                                                      LocalDate dateMiseEnDemeure,
                                                      LocalDate dateRetrait) {
        List<String> vices = new ArrayList<>();
        if (!miseEnDemeurePrealable) {
            vices.add("Absence de mise en demeure / de procédure contradictoire préalable "
                    + "(L. 411-5 CESEDA) : l'étranger n'a pas été mis à même de présenter ses "
                    + "observations avant le retrait — vice substantiel.");
        } else if (dateMiseEnDemeure != null
                && ChronoUnit.DAYS.between(dateMiseEnDemeure, dateRetrait) < SEUIL_URGENT_JOURS) {
            vices.add("Délai insuffisant entre la mise en demeure et la décision de retrait "
                    + "(< 15 jours) : délai utile de présentation des observations non respecté.");
        }
        vices.add("Défaut ou insuffisance de motivation de la décision de retrait au regard de "
                + "la matérialité de la fraude reprochée (charge de la preuve à l'administration).");
        return vices;
    }

    private static List<String> buildMotifsContestation(RetraitTitreFraudeMotifEnum motif) {
        return switch (motif) {
            case MARIAGE_GRIS -> List.of(
                    "Preuve d'une communauté de vie effective (domicile commun, vie commune continue)",
                    "Existence d'enfant(s) commun(s) attestant de l'intention matrimoniale",
                    "Éléments matériels de la vie de couple (photos, correspondances, témoignages, comptes joints)",
                    "Absence d'élément intentionnel de fraude établi par l'administration (charge de la preuve)");
            case FRAUDE_DOCUMENTAIRE -> List.of(
                    "Contestation de l'authenticité de l'expertise documentaire de l'administration",
                    "Bonne foi de l'intéressé / erreur sur la régularité du document produit",
                    "Absence de caractère déterminant du document litigieux dans la délivrance du titre",
                    "Défaut de preuve de l'intention frauduleuse imputable à l'intéressé");
            case FAUSSES_DECLARATIONS -> List.of(
                    "Inexactitude matérielle des faits retenus comme fausses déclarations",
                    "Absence de caractère intentionnel et déterminant de la déclaration litigieuse",
                    "Régularité de la situation au fond malgré la déclaration contestée",
                    "Disproportion de la mesure au regard de la gravité réelle des faits");
            case PERTE_CONDITIONS -> List.of(
                    "Maintien effectif des conditions de délivrance du titre",
                    "Erreur d'appréciation de l'administration sur la perte des conditions",
                    "Atteinte disproportionnée à la vie privée et familiale (art. 8 CEDH)",
                    "Existence d'un autre fondement de séjour ouvrant droit au maintien");
        };
    }

    private static String buildRecommandation(RetraitTitreFraudeStatut statut,
                                              List<String> vicesDeProcedure) {
        return switch (statut) {
            case PRESCRIT -> "Délai de recours de 2 mois expiré : la voie du recours contentieux "
                    + "ordinaire est close — examiner un recours gracieux ou un réexamen au regard "
                    + "d'éléments nouveaux.";
            case URGENT -> "Délai de recours expirant sous 15 jours : saisir sans délai le tribunal "
                    + "administratif (recours pour excès de pouvoir) en soulevant les vices de "
                    + "procédure (" + vicesDeProcedure.size() + ") et les moyens au fond.";
            case RECOURS_POSSIBLE -> "Recours pour excès de pouvoir ouvert devant le tribunal "
                    + "administratif (2 mois) : soulever en priorité les vices de procédure "
                    + "(contradictoire préalable) puis les moyens au fond selon le motif invoqué.";
        };
    }

    private static void validate(LocalDate dateRetrait,
                                 RetraitTitreFraudeMotifEnum motifRetrait,
                                 LocalDate today) {
        if (dateRetrait == null) {
            throw new IllegalArgumentException("dateRetrait est requise");
        }
        if (motifRetrait == null) {
            throw new IllegalArgumentException("motifRetrait est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
    }
}
