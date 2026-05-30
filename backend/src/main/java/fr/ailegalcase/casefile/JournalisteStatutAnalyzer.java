package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-218-15 : analyseur du statut de journaliste professionnel (art. L.7111-1 et
 * s. CT) lors d'une rupture — qualification du statut, validité de la clause de
 * cession / de conscience (art. L.7112-5), indemnité de congédiement
 * (art. L.7112-3) et signalement de la commission arbitrale paritaire
 * (art. L.7112-4). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Qualification</b> : journaliste professionnel présumé si détention de
 *       la carte d'identité de journaliste professionnel (CCIJP) → {@code
 *       CONFIRME} ; sinon {@code A_QUALIFIER}.</li>
 *   <li><b>Clause de cession</b> (art. L.7112-5 1°) : valide si la cession ou
 *       cessation de publication du titre est constatée — rupture assimilée à un
 *       licenciement ouvrant droit à indemnité.</li>
 *   <li><b>Clause de conscience</b> (art. L.7112-5 2°/3°) : valide si un
 *       changement notable du caractère / de l'orientation du journal est
 *       constaté — rupture assimilée à un licenciement.</li>
 *   <li><b>Indemnité de congédiement</b> (art. L.7112-3) : 1 mois de salaire par
 *       année ou fraction d'année d'ancienneté, plafonnée à 15 mensualités hors
 *       commission arbitrale.</li>
 *   <li><b>Commission arbitrale</b> (art. L.7112-4) : ancienneté &gt; 15 ans ou
 *       faute grave / fautes répétées → indemnité fixée souverainement par la
 *       commission arbitrale paritaire.</li>
 *   <li><b>Exclusions</b> : démission → pas d'indemnité de congédiement de droit ;
 *       faute grave → renvoi commission, pas d'indemnité de droit.</li>
 * </ul>
 */
public final class JournalisteStatutAnalyzer {

    static final String BASE_JURIDIQUE =
            "art. L.7111-1 à L.7113-12 du Code du travail (statut du journaliste "
                    + "professionnel) ; art. L.7112-3 (indemnité de congédiement — 1 mois "
                    + "de salaire par année d'ancienneté) ; art. L.7112-4 (commission "
                    + "arbitrale paritaire au-delà de 15 ans d'ancienneté ou en cas de "
                    + "faute grave / fautes répétées) ; art. L.7112-5 (clause de cession "
                    + "et clause de conscience) ; carte d'identité de journaliste "
                    + "professionnel délivrée par la CCIJP — présomption de la qualité de "
                    + "journaliste (à vérifier par avocat)";

    private JournalisteStatutAnalyzer() {
    }

    /**
     * Analyse le statut journaliste, la validité de la clause invoquée,
     * l'indemnité de congédiement, le passage par la commission arbitrale et le
     * verdict global.
     *
     * @param dateEntree début du contrat (requis).
     * @param dateRupture date de notification de la rupture (requis, ≥ dateEntree).
     * @param typeRupture type de rupture invoqué (requis).
     * @param salaireMensuelMoyen base de l'indemnité (€, strictement positif).
     * @param carteIdentiteProfessionnelle détention de la carte de presse (défaut true).
     * @param cessionTitreConstatee fait générateur de la clause de cession (défaut false).
     * @param changementOrientationConstate fait générateur de la clause de conscience (défaut false).
     */
    public static JournalisteStatutResult analyze(LocalDate dateEntree,
                                                  LocalDate dateRupture,
                                                  JournalisteStatutTypeRupture typeRupture,
                                                  BigDecimal salaireMensuelMoyen,
                                                  Boolean carteIdentiteProfessionnelle,
                                                  Boolean cessionTitreConstatee,
                                                  Boolean changementOrientationConstate) {
        validate(dateEntree, dateRupture, typeRupture, salaireMensuelMoyen);

        boolean carte = carteIdentiteProfessionnelle == null || carteIdentiteProfessionnelle;
        boolean cession = Boolean.TRUE.equals(cessionTitreConstatee);
        boolean changementOrientation = Boolean.TRUE.equals(changementOrientationConstate);

        // Ancienneté en années : toute année commencée compte pour une année
        // entière (art. L.7112-3 — « par année ou fraction d'année »).
        int ancienneteAnnees = ancienneteAnnees(dateEntree, dateRupture);

        // Qualification du statut.
        JournalisteStatutQualification statut = carte
                ? JournalisteStatutQualification.CONFIRME
                : JournalisteStatutQualification.A_QUALIFIER;

        // Validité de la clause invoquée.
        JournalisteStatutClauseValidite clauseValide;
        String motifClause;
        switch (typeRupture) {
            case CLAUSE_CESSION -> {
                if (cession) {
                    clauseValide = JournalisteStatutClauseValidite.VALIDE;
                    motifClause = null;
                } else {
                    clauseValide = JournalisteStatutClauseValidite.NON_VALIDE;
                    motifClause = "Clause de cession invoquée sans cession ni cessation de "
                            + "publication du titre constatée (art. L.7112-5 1°)";
                }
            }
            case CLAUSE_CONSCIENCE -> {
                if (changementOrientation) {
                    clauseValide = JournalisteStatutClauseValidite.VALIDE;
                    motifClause = null;
                } else {
                    clauseValide = JournalisteStatutClauseValidite.NON_VALIDE;
                    motifClause = "Clause de conscience invoquée sans changement notable du "
                            + "caractère ou de l'orientation du journal constaté (art. L.7112-5 2°/3°)";
                }
            }
            default -> {
                clauseValide = JournalisteStatutClauseValidite.SANS_OBJET;
                motifClause = null;
            }
        }

        // La rupture ouvre-t-elle droit à l'indemnité de congédiement ?
        // Licenciement, clause de cession valide, clause de conscience valide.
        boolean ouvreDroitIndemnite =
                typeRupture == JournalisteStatutTypeRupture.LICENCIEMENT
                        || ((typeRupture == JournalisteStatutTypeRupture.CLAUSE_CESSION
                        || typeRupture == JournalisteStatutTypeRupture.CLAUSE_CONSCIENCE)
                        && clauseValide == JournalisteStatutClauseValidite.VALIDE);

        // Commission arbitrale : faute grave / fautes répétées, OU ancienneté > 15 ans.
        boolean fauteGrave = typeRupture == JournalisteStatutTypeRupture.FAUTE_GRAVE;
        boolean ancienneteAuDela = ancienneteAnnees > BaremeJournalisteStatut.SEUIL_COMMISSION_ARBITRALE_ANNEES;
        boolean commissionArbitraleRequise = fauteGrave || (ouvreDroitIndemnite && ancienneteAuDela);

        // Indemnité de congédiement (art. L.7112-3) — base 1 mois/année, plafonnée
        // à 15 mensualités hors commission arbitrale.
        BigDecimal indemnite;
        String noteCommission = null;
        if (!ouvreDroitIndemnite || fauteGrave) {
            indemnite = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            int moisPlafonnes = Math.min(
                    ancienneteAnnees * BaremeJournalisteStatut.INDEMNITE_MOIS_PAR_ANNEE,
                    BaremeJournalisteStatut.PLAFOND_INDEMNITE_MOIS);
            indemnite = salaireMensuelMoyen
                    .multiply(BigDecimal.valueOf(moisPlafonnes))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (commissionArbitraleRequise) {
            noteCommission = fauteGrave
                    ? "Faute grave / fautes répétées invoquées : l'indemnité est fixée "
                    + "souverainement par la commission arbitrale paritaire (art. L.7112-4)"
                    : "Le montant ci-dessus est plafonné à 15 mois ; au-delà de 15 ans "
                    + "d'ancienneté, compétence exclusive de la commission arbitrale "
                    + "paritaire (art. L.7112-4)";
        }

        // Verdict global.
        JournalisteStatutVerdict verdict;
        if (commissionArbitraleRequise) {
            verdict = JournalisteStatutVerdict.COMMISSION_ARBITRALE;
        } else if (!ouvreDroitIndemnite) {
            verdict = JournalisteStatutVerdict.INDEMNITE_NON_DUE;
        } else if (typeRupture == JournalisteStatutTypeRupture.CLAUSE_CESSION
                || typeRupture == JournalisteStatutTypeRupture.CLAUSE_CONSCIENCE) {
            verdict = JournalisteStatutVerdict.RUPTURE_ASSIMILEE_LICENCIEMENT;
        } else {
            verdict = JournalisteStatutVerdict.INDEMNITE_DUE;
        }

        return new JournalisteStatutResult(
                dateEntree,
                dateRupture,
                typeRupture,
                salaireMensuelMoyen.setScale(2, RoundingMode.HALF_UP),
                carte,
                ancienneteAnnees,
                statut,
                clauseValide,
                motifClause,
                indemnite,
                commissionArbitraleRequise,
                noteCommission,
                verdict,
                BASE_JURIDIQUE);
    }

    /**
     * Ancienneté en années : toute année commencée compte pour une année entière
     * (art. L.7112-3 — « par année ou fraction d'année d'ancienneté »).
     */
    private static int ancienneteAnnees(LocalDate dateEntree, LocalDate dateRupture) {
        int anneesPleines = (int) ChronoUnit.YEARS.between(dateEntree, dateRupture);
        boolean fraction = dateEntree.plusYears(anneesPleines).isBefore(dateRupture);
        return fraction ? anneesPleines + 1 : Math.max(anneesPleines, 0);
    }

    private static void validate(LocalDate dateEntree,
                                 LocalDate dateRupture,
                                 JournalisteStatutTypeRupture typeRupture,
                                 BigDecimal salaireMensuelMoyen) {
        if (dateEntree == null) {
            throw new IllegalArgumentException("dateEntree est requise");
        }
        if (dateRupture == null) {
            throw new IllegalArgumentException("dateRupture est requise");
        }
        if (typeRupture == null) {
            throw new IllegalArgumentException("typeRupture est requis");
        }
        if (dateRupture.isBefore(dateEntree)) {
            throw new IllegalArgumentException("dateRupture ne peut pas être antérieure à dateEntree");
        }
        if (salaireMensuelMoyen == null || salaireMensuelMoyen.signum() <= 0) {
            throw new IllegalArgumentException("salaireMensuelMoyen doit être strictement positif");
        }
    }
}
