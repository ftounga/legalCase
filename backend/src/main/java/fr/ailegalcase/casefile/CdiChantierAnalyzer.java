package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-25 : analyseur du <b>licenciement pour fin de chantier / d'opération</b>
 * du CDI de chantier (art. L.1223-8 et L.1223-9 ; L.1236-8 ; R.1234-2 CT,
 * F-DT-37). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Validité du recours</b> : le CDI de chantier suppose un accord de
 *       branche étendu (art. L.1223-8) ou, à défaut, un usage constant du
 *       secteur (BTP / ingénierie — art. L.1223-9). {@code fondementRecours} ∈
 *       { ACCORD_BRANCHE_ETENDU, USAGE_CONSTANT_SECTEUR } → {@code recoursValide
 *       = true}. {@code AUCUN} → {@code recoursValide = false} (risque de
 *       requalification en CDI de droit commun). Cas particulier : un usage
 *       constant n'est reconnu que dans les secteurs BTP / ingénierie ;
 *       {@code USAGE_CONSTANT_SECTEUR} + secteur {@code AUTRE} est fragilisé
 *       (l'usage constant n'est pas établi hors BTP / ingénierie).</li>
 *   <li><b>Qualification du motif</b> : si {@code recoursValide} et
 *       {@code chantierAcheve} → {@code FIN_CHANTIER_CRS} (cause réelle et
 *       sérieuse, motif spécifique L.1236-8) ; sinon {@code MOTIF_NON_FONDE}.</li>
 *   <li><b>Indemnité de licenciement</b> (art. R.1234-2) : ancienneté (années
 *       pleines) × 1/4 de salaire mensuel pour les 10 premières années +
 *       1/3 au-delà. Le CDI de chantier ouvre droit à l'indemnité de
 *       licenciement (et non à l'indemnité de précarité du CDD). CCN BTP /
 *       Syntec éventuellement plus favorable à vérifier.</li>
 *   <li><b>Procédure</b> : licenciement soumis à la procédure de droit commun
 *       (entretien préalable, notification) — {@code procedureRequise = true}.</li>
 *   <li><b>Verdict global</b> : recours invalide → {@code RECOURS_INVALIDE} ;
 *       recours valide + chantier achevé + (usage constant hors BTP/ingénierie
 *       exclu) → {@code LICENCIEMENT_FONDE} ; recours valide mais motif fragile
 *       (chantier non achevé, reclassement sur un autre chantier non tracé, ou
 *       usage constant hors secteur reconnu) → {@code LICENCIEMENT_A_SECURISER}.</li>
 * </ul>
 *
 * <p>Hors périmètre : chiffrage de l'indemnité pour licenciement sans cause
 * réelle et sérieuse en cas de requalification (barème Macron, outil F-DT
 * existant), régime du CDD de chantier, détail des obligations conventionnelles
 * de reclassement BTP. Base juridique annotée « à vérifier par avocat ».
 */
public final class CdiChantierAnalyzer {

    /** Seuil d'ancienneté de bascule du taux d'indemnité (R.1234-2) : 10 ans. */
    static final int SEUIL_ANNEES_INDEMNITE = 10;

    /**
     * Barème légal R.1234-2 — vérifier CCN BTP / ingénierie plus favorable, à
     * actualiser : 1/4 de mois de salaire par année pour les 10 premières
     * années.
     */
    static final BigDecimal TAUX_INDEMNITE_JUSQUA_10_ANS = new BigDecimal("0.25");

    /**
     * Barème légal R.1234-2 — vérifier CCN BTP / ingénierie plus favorable, à
     * actualiser : 1/3 de mois de salaire par année au-delà de 10 ans.
     */
    static final BigDecimal TAUX_INDEMNITE_AU_DELA_10_ANS = new BigDecimal("0.333333");

    static final String BASE_JURIDIQUE =
            "art. L.1223-8 et L.1223-9 CT — contrat de chantier ou d'opération "
                    + "(CDI de chantier) : recours subordonné à un accord de branche "
                    + "étendu ou, à défaut, à un usage constant dans certains secteurs "
                    + "(BTP, ingénierie) ; art. L.1236-8 CT — la rupture à la fin du "
                    + "chantier repose sur une cause réelle et sérieuse (motif spécifique), "
                    + "procédure de licenciement de droit commun ; art. R.1234-2 CT — "
                    + "indemnité légale de licenciement ; CCN BTP / ingénierie (Syntec) — "
                    + "modalités et indemnités plus favorables éventuelles (à vérifier par "
                    + "avocat)";

    private CdiChantierAnalyzer() {
    }

    /**
     * Analyse le licenciement pour fin de chantier : apprécie la validité du
     * recours, qualifie le motif, calcule l'indemnité de licenciement (R.1234-2)
     * et rend le verdict global.
     */
    public static CdiChantierResult analyze(LocalDate dateEntree,
                                            LocalDate dateRupture,
                                            CdiChantierFondementRecours fondementRecours,
                                            CdiChantierSecteur secteur,
                                            Boolean chantierAcheve,
                                            BigDecimal salaireMensuelMoyen,
                                            Boolean reclassementAutreChantierPropose) {
        validate(dateEntree, dateRupture, fondementRecours, secteur, chantierAcheve, salaireMensuelMoyen);

        boolean acheve = chantierAcheve;
        boolean reclassementPropose = reclassementAutreChantierPropose != null
                && reclassementAutreChantierPropose;

        int ancienneteAnnees = (int) ChronoUnit.YEARS.between(dateEntree, dateRupture);

        List<String> consequences = new ArrayList<>();

        // ── Validité du recours (art. L.1223-8 / L.1223-9) ──────────────────
        boolean recoursValide;
        boolean usageConstantHorsSecteurReconnu = false;
        String motifRecours;
        switch (fondementRecours) {
            case ACCORD_BRANCHE_ETENDU -> {
                recoursValide = true;
                motifRecours = "Recours au CDI de chantier fondé sur un accord de branche "
                        + "étendu (art. L.1223-8) : fondement valable.";
            }
            case USAGE_CONSTANT_SECTEUR -> {
                recoursValide = true;
                if (secteur == CdiChantierSecteur.BTP || secteur == CdiChantierSecteur.INGENIERIE) {
                    motifRecours = "Recours admis par l'usage constant du secteur "
                            + "(BTP / ingénierie — art. L.1223-9) : fondement valable.";
                } else {
                    usageConstantHorsSecteurReconnu = true;
                    motifRecours = "Recours invoqué sur un usage constant dans un secteur "
                            + "autre que le BTP / l'ingénierie : l'usage constant n'y est "
                            + "pas établi (art. L.1223-9) — à sécuriser par un accord de "
                            + "branche étendu.";
                    consequences.add("Vérifier l'existence d'un accord de branche étendu : "
                            + "hors BTP / ingénierie, l'usage constant ne suffit pas à "
                            + "fonder le recours (risque de requalification).");
                }
            }
            case AUCUN -> {
                recoursValide = false;
                motifRecours = "Aucun fondement au recours au CDI de chantier (ni accord de "
                        + "branche étendu, ni usage constant) : risque de requalification en "
                        + "CDI de droit commun (art. L.1223-8).";
                consequences.add("Risque de requalification du CDI de chantier en CDI de "
                        + "droit commun : le licenciement pour fin de chantier serait alors "
                        + "privé de cause (à chiffrer via le barème de l'art. L.1235-3).");
            }
            default -> throw new IllegalArgumentException("fondementRecours inconnu");
        }

        // ── Qualification du motif (art. L.1236-8) ──────────────────────────
        CdiChantierMotifLicenciement motifLicenciement;
        String motif;
        if (recoursValide && acheve) {
            motifLicenciement = CdiChantierMotifLicenciement.FIN_CHANTIER_CRS;
            motif = "Licenciement pour fin de chantier : le chantier / l'opération pour "
                    + "lequel le salarié a été engagé est réalisé. La rupture repose sur une "
                    + "cause réelle et sérieuse (motif spécifique, art. L.1236-8).";
        } else if (recoursValide) {
            motifLicenciement = CdiChantierMotifLicenciement.MOTIF_NON_FONDE;
            motif = "Le chantier / l'opération n'est pas achevé : le motif de fin de "
                    + "chantier n'est pas caractérisé (art. L.1236-8). Le licenciement ne "
                    + "peut être fondé sur ce motif spécifique en l'état.";
            consequences.add("Attendre l'achèvement effectif du chantier ou retenir un autre "
                    + "motif de licenciement dûment justifié.");
        } else {
            motifLicenciement = CdiChantierMotifLicenciement.MOTIF_NON_FONDE;
            motif = "Recours au CDI de chantier non fondé : le motif de fin de chantier "
                    + "(art. L.1236-8) est inopérant tant que le recours n'est pas sécurisé.";
        }

        // ── Indemnité de licenciement (art. R.1234-2) ───────────────────────
        // barème légal R.1234-2 — vérifier CCN BTP/ingénierie plus favorable, à actualiser
        BigDecimal indemnite = computeIndemnite(salaireMensuelMoyen, ancienneteAnnees);
        consequences.add("Indemnité légale de licenciement due (le CDI de chantier ouvre "
                + "droit à l'indemnité de licenciement, et non à l'indemnité de précarité "
                + "du CDD) — art. R.1234-2 ; vérifier une CCN BTP / Syntec plus favorable.");

        // ── Procédure de droit commun ───────────────────────────────────────
        boolean procedureRequise = true;
        consequences.add("Respecter la procédure de licenciement de droit commun : "
                + "convocation à entretien préalable, entretien, notification motivée "
                + "(art. L.1232-2 et s.).");
        if (recoursValide && acheve && !reclassementPropose) {
            consequences.add("Aucune proposition de poursuite sur un autre chantier n'est "
                    + "tracée : documenter l'absence de poste disponible pour sécuriser le "
                    + "motif (selon stipulations conventionnelles BTP / Syntec).");
        }

        // ── Verdict global ──────────────────────────────────────────────────
        CdiChantierVerdict verdict;
        if (!recoursValide) {
            verdict = CdiChantierVerdict.RECOURS_INVALIDE;
        } else if (acheve && !usageConstantHorsSecteurReconnu && reclassementPropose) {
            verdict = CdiChantierVerdict.LICENCIEMENT_FONDE;
        } else if (acheve && !usageConstantHorsSecteurReconnu) {
            // Chantier achevé, recours solide, mais reclassement non tracé → à sécuriser.
            verdict = CdiChantierVerdict.LICENCIEMENT_A_SECURISER;
        } else {
            // Chantier non achevé, ou usage constant hors secteur reconnu → à sécuriser.
            verdict = CdiChantierVerdict.LICENCIEMENT_A_SECURISER;
        }

        return new CdiChantierResult(
                dateEntree,
                dateRupture,
                fondementRecours,
                secteur,
                acheve,
                salaireMensuelMoyen,
                reclassementPropose,
                ancienneteAnnees,
                recoursValide,
                motifRecours,
                motifLicenciement,
                indemnite,
                procedureRequise,
                verdict,
                List.copyOf(consequences),
                motif,
                BASE_JURIDIQUE);
    }

    /**
     * Indemnité légale de licenciement (art. R.1234-2) : 1/4 de mois de salaire
     * par année d'ancienneté pour les 10 premières années + 1/3 par année
     * au-delà. Le résultat est arrondi au centime.
     */
    static BigDecimal computeIndemnite(BigDecimal salaireMensuelMoyen, int ancienneteAnnees) {
        if (ancienneteAnnees <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int anneesJusqua10 = Math.min(ancienneteAnnees, SEUIL_ANNEES_INDEMNITE);
        int anneesAuDela = Math.max(0, ancienneteAnnees - SEUIL_ANNEES_INDEMNITE);

        BigDecimal partBase = salaireMensuelMoyen
                .multiply(TAUX_INDEMNITE_JUSQUA_10_ANS)
                .multiply(BigDecimal.valueOf(anneesJusqua10));
        BigDecimal partAuDela = salaireMensuelMoyen
                .multiply(TAUX_INDEMNITE_AU_DELA_10_ANS)
                .multiply(BigDecimal.valueOf(anneesAuDela));

        return partBase.add(partAuDela).setScale(2, RoundingMode.HALF_UP);
    }

    private static void validate(LocalDate dateEntree,
                                 LocalDate dateRupture,
                                 CdiChantierFondementRecours fondementRecours,
                                 CdiChantierSecteur secteur,
                                 Boolean chantierAcheve,
                                 BigDecimal salaireMensuelMoyen) {
        if (dateEntree == null) {
            throw new IllegalArgumentException("dateEntree est requise");
        }
        if (dateRupture == null) {
            throw new IllegalArgumentException("dateRupture est requise");
        }
        if (dateRupture.isBefore(dateEntree)) {
            throw new IllegalArgumentException("dateRupture ne peut pas précéder dateEntree");
        }
        if (fondementRecours == null) {
            throw new IllegalArgumentException("fondementRecours est requis");
        }
        if (secteur == null) {
            throw new IllegalArgumentException("secteur est requis");
        }
        if (chantierAcheve == null) {
            throw new IllegalArgumentException("chantierAcheve est requis");
        }
        if (salaireMensuelMoyen == null) {
            throw new IllegalArgumentException("salaireMensuelMoyen est requis");
        }
        if (salaireMensuelMoyen.signum() < 0) {
            throw new IllegalArgumentException("salaireMensuelMoyen ne peut pas être négatif");
        }
    }
}
