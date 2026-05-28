package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-219-20 : calculateur du <i>pécule de vacances BE</i> (Lois
 * coordonnées du 28/06/1971 relatives aux vacances annuelles des
 * travailleurs salariés ; AR du 30/03/1967 déterminant les modalités
 * générales d'exécution) — fonction <b>pure</b> (sans dépendance
 * HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Lois coordonnées du 28/06/1971</b> (M.B. 30/09/1971)
 *       — vacances annuelles des travailleurs salariés. Art. 3 :
 *       durée minimale (4 semaines à temps plein). Art. 6 : double
 *       pécule. Art. 7 : pécule de sortie.</li>
 *   <li><b>AR du 30/03/1967</b> (M.B. 06/04/1967) — modalités
 *       générales d'exécution. Art. 9 : vacances jeunes. Art. 19 :
 *       pécule ouvrier 15,38 % (8 % pécule simple + 7,38 %
 *       double pécule, après les 7 % réservés au précompte ONSS)
 *       de la rémunération brute annuelle 108 % de l'exercice de
 *       vacances. Art. 38 : pécule employé — rémunération maintenue
 *       (simple) + 85 % du salaire mensuel brut (double). Art. 46 :
 *       pécule de départ employé — 15,34 % de la rémunération brute
 *       de l'exercice en cours + 15,34 % de la fraction de
 *       l'exercice précédent non encore liquidée.</li>
 *   <li><b>Loi du 03/07/1978 sur les contrats de travail</b>,
 *       art. 15 — prescription des actions naissant du contrat de
 *       travail : <b>1 an</b> après la cessation du contrat (5 ans
 *       depuis le fait générateur, plafonné à 1 an post-rupture).</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>A_ANALYSER</b> — statut {@code INDETERMINE}.</li>
 *   <li><b>NON_DU_DEJA_PAYE</b> — {@code peculeDejaPaye=true}.</li>
 *   <li><b>NON_DU_OUVRIER_VIA_ONVA</b> — statut OUVRIER et
 *       typeCalcul ≠ PECULE_DEPART (l'ouvrier doit s'adresser à
 *       l'ONVA / Caisse de vacances, pas à l'employeur).</li>
 *   <li><b>PRESCRIT</b> — créance prescrite (≥ 1 an depuis fin
 *       du contrat).</li>
 *   <li><b>NON_DU_JOURS_INSUFFISANTS</b> — JEUNE_TRAVAILLEUR sans
 *       jours pris.</li>
 *   <li><b>DU_PECULE_SIMPLE_CALCULE</b> / <b>DU_DOUBLE_PECULE_CALCULE</b>
 *       / <b>DU_PECULE_DEPART_CALCULE</b> — sinon, selon
 *       {@code typeCalcul}.</li>
 * </ol>
 */
public final class PeculeVacancesBeCalculator {

    /** Pécule simple ouvrier (8 %) + double pécule ouvrier (7,38 %)
     *  combinés sur la rémunération annuelle brute 108 % — art. 19
     *  AR 30/03/1967 (calcul théorique non-utilisé par l'outil, car
     *  l'ouvrier passe par l'ONVA). */
    static final BigDecimal POURCENT_OUVRIER_ONVA_TOTAL =
            new BigDecimal("0.1538");

    /** Fraction double pécule employé (85 % du salaire mensuel brut)
     *  — art. 38 AR 30/03/1967. */
    static final BigDecimal POURCENT_DOUBLE_PECULE_EMPLOYE =
            new BigDecimal("0.85");

    /** Fraction pécule de départ employé (15,34 % par exercice à
     *  liquider) — art. 46 AR 30/03/1967. */
    static final BigDecimal POURCENT_PECULE_DEPART_EMPLOYE =
            new BigDecimal("0.1534");

    /** Nombre de jours de vacances ouvrant droit au pécule complet
     *  (4 semaines régime 6 jours = 24 jours, régime 5 jours = 20).
     *  Le calcul retenu prend le régime 6 jours par défaut — l'avocat
     *  doit confirmer le régime applicable. */
    static final BigDecimal JOURS_VACANCES_REFERENCE_REGIME_6J =
            new BigDecimal("24");

    /** Prescription post-contrat — art. 15 Loi 03/07/1978 (1 an). */
    static final long JOURS_PRESCRIPTION_POST_CONTRAT = 365L;

    static final String BASE_JURIDIQUE =
            "Lois coordonnées du 28/06/1971 relatives aux vacances"
                    + " annuelles des travailleurs salariés (M.B."
                    + " 30/09/1971), art. 3 (durée minimale 4 semaines"
                    + " à temps plein), art. 6 (double pécule), art. 7"
                    + " (pécule de sortie) ; AR du 30/03/1967 (M.B."
                    + " 06/04/1967) déterminant les modalités générales"
                    + " d'exécution, art. 9 (vacances jeunes), art. 19"
                    + " (pécule ouvrier 15,38 % de la rémunération brute"
                    + " annuelle 108 % de l'exercice de vacances —"
                    + " liquidé par l'ONVA ou la Caisse de vacances"
                    + " sectorielle), art. 38 (pécule employé —"
                    + " rémunération maintenue pendant les jours de"
                    + " congé pris + 85 % du salaire mensuel brut comme"
                    + " double pécule au moment des vacances principales),"
                    + " art. 46 (pécule de départ employé — 15,34 % de la"
                    + " rémunération brute de l'exercice en cours"
                    + " + 15,34 % de la fraction de l'exercice précédent"
                    + " non encore liquidée, payable au plus tard à"
                    + " l'expiration du mois qui suit la fin du contrat) ;"
                    + " Loi du 03/07/1978 sur les contrats de travail,"
                    + " art. 15 (prescription des actions naissant du"
                    + " contrat — 1 an après la cessation, 5 ans depuis"
                    + " le fait générateur).";

    static final String AVERT_AVOCAT_BE =
            "Les Lois coordonnées 28/06/1971 et l'AR 30/03/1967 sur les"
                    + " vacances annuelles sont d'application complexe."
                    + " L'avocat spécialisé en droit social BE doit"
                    + " confirmer pour la situation considérée : (i) le"
                    + " statut effectif du travailleur (employé / ouvrier"
                    + " — distinction qui détermine le débiteur du pécule :"
                    + " employeur direct pour l'employé, ONVA / Caisse"
                    + " spéciale de vacances sectorielle pour l'ouvrier) ;"
                    + " (ii) la base de calcul exacte (rémunération brute"
                    + " incluant le salaire, les avantages en nature, les"
                    + " primes périodiques et la prime de fin d'année dans"
                    + " la limite de l'art. 19 AR 30/03/1967, à l'exclusion"
                    + " des indemnités occasionnelles) ; (iii) l'application"
                    + " du coefficient 108 % pour la rémunération ouvrier"
                    + " (passage de la rémunération brute en rémunération"
                    + " brute annuelle « majorée » 108 % avant application"
                    + " du 15,38 % — art. 19 AR 30/03/1967) ; (iv) le"
                    + " régime de jours (6 j vs 5 j) pour la conversion"
                    + " entre jours pris et fraction du droit annuel ; (v)"
                    + " les régularisations en cas de variation de durée"
                    + " du travail / passage temps plein-temps partiel ;"
                    + " (vi) l'éventuelle indemnité supplémentaire de"
                    + " vacances jeunes (art. 9bis AR 30/03/1967) ou"
                    + " vacances seniors (art. 9ter) ; (vii) la prescription"
                    + " applicable (art. 15 Loi 03/07/1978 — 1 an post-"
                    + "contrat, 5 ans depuis le fait générateur) et la"
                    + " portée des actes interruptifs (lettre recommandée"
                    + " mise en demeure, citation, reconnaissance par"
                    + " l'employeur).";

    private PeculeVacancesBeCalculator() {
    }

    public static PeculeVacancesBeResult analyze(
            PeculeVacancesBeRequest request) {
        validateInputs(request);

        BigDecimal remunExercice = request
                .remunerationBruteAnnuelleExerciceEur();
        BigDecimal remunMensuel = request.remunerationMensuelleBruteEur();
        BigDecimal remunExercicePrec = request
                .remunerationBruteAnnuelleExercicePrecedentEur();
        BigDecimal joursPris = request.joursCongesPris();

        // ── Calculs montants (toujours produits — utiles dans la
        // restitution même si verdict NON_DU / PRESCRIT, pour
        // permettre une référence au montant théorique) ──────────────
        BigDecimal montantSimple = computePeculeSimple(
                request.statut(), remunMensuel, joursPris);
        BigDecimal montantDouble = computeDoublePecule(
                request.statut(), remunMensuel, remunExercice);
        BigDecimal montantDepart = computePeculeDepart(
                request.statut(), remunExercice, remunExercicePrec);
        BigDecimal fraction = fractionLegale(
                request.statut(), request.typeCalcul());

        long joursDepuisFaitGenerateur = computeJoursPrescription(
                request.dateFinContrat(), request.dateReclamation());
        boolean prescrit = isPrescrit(request.dateFinContrat(),
                request.dateReclamation());

        // ── Hiérarchie 1 : statut indéterminé ────────────────────────
        if (request.statut() == PeculeVacancesBeStatut.INDETERMINE) {
            String synthese = "Statut du travailleur indéterminé"
                    + " (employé / ouvrier / jeune travailleur) :"
                    + " impossible de fixer le débiteur du pécule"
                    + " (employeur direct ou ONVA / Caisse de"
                    + " vacances) ni le mode de calcul applicable."
                    + " Analyse cas par cas requise.";
            return build(request,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    prescrit, joursDepuisFaitGenerateur,
                    PeculeVacancesBeVerdict.A_ANALYSER,
                    "STATUT_INDETERMINE", synthese);
        }

        // ── Hiérarchie 2 : déjà payé ─────────────────────────────────
        if (request.peculeDejaPaye()) {
            String synthese = "Le pécule réclamé a déjà été liquidé"
                    + " par l'employeur (ou l'ONVA pour les ouvriers)."
                    + " L'action en paiement est sans objet — le"
                    + " contrôle reste opportun sur le montant"
                    + " effectivement versé (cohérence avec la base"
                    + " art. 19 ou art. 38/46 AR 30/03/1967).";
            return build(request,
                    montantSimple, montantDouble, montantDepart,
                    BigDecimal.ZERO, fraction,
                    prescrit, joursDepuisFaitGenerateur,
                    PeculeVacancesBeVerdict.NON_DU_DEJA_PAYE,
                    "PECULE_DEJA_PAYE", synthese);
        }

        // ── Hiérarchie 3 : ouvrier → ONVA (sauf pécule de départ) ────
        // L'ouvrier touche son pécule auprès de l'ONVA / Caisse de
        // vacances. L'action contre l'employeur n'est recevable que
        // dans l'hypothèse d'un défaut de versement des cotisations
        // (qui dépasse le calcul opéré ici). Le pécule de départ
        // n'existe pas pour l'ouvrier dans la même configuration : il
        // est intégré dans le pécule ONVA de l'année suivante.
        if (request.statut() == PeculeVacancesBeStatut.OUVRIER) {
            String synthese = "L'ouvrier perçoit son pécule de vacances"
                    + " (simple et double) directement auprès de"
                    + " l'Office National des Vacances Annuelles"
                    + " (ONVA) ou de la Caisse spéciale de vacances"
                    + " sectorielle compétente — pas d'action directe"
                    + " contre l'employeur (art. 19 AR 30/03/1967)."
                    + " Le pécule de départ ouvrier est intégré dans"
                    + " la liquidation ONVA de l'année suivante. Une"
                    + " action contre l'employeur ne reste ouverte que"
                    + " si celui-ci a omis de verser les cotisations"
                    + " trimestrielles à l'ONSS / ONVA (à vérifier).";
            return build(request,
                    montantSimple, montantDouble, montantDepart,
                    BigDecimal.ZERO, fraction,
                    prescrit, joursDepuisFaitGenerateur,
                    PeculeVacancesBeVerdict.NON_DU_OUVRIER_VIA_ONVA,
                    "OUVRIER_VIA_ONVA", synthese);
        }

        // ── Hiérarchie 4 : prescription (art. 15 Loi 03/07/1978) ─────
        if (prescrit) {
            String synthese = "La créance est prescrite : plus d'1 an"
                    + " s'est écoulé entre la fin du contrat (" + request
                    .dateFinContrat() + ") et la réclamation (" + request
                    .dateReclamation() + "). L'art. 15 Loi 03/07/1978"
                    + " sur les contrats de travail prévoit une"
                    + " prescription d'1 an post-contrat (5 ans depuis"
                    + " le fait générateur). Sauf acte interruptif"
                    + " antérieur (lettre recommandée mise en demeure,"
                    + " citation, reconnaissance écrite par l'employeur)"
                    + " l'action est éteinte.";
            return build(request,
                    montantSimple, montantDouble, montantDepart,
                    BigDecimal.ZERO, fraction,
                    true, joursDepuisFaitGenerateur,
                    PeculeVacancesBeVerdict.PRESCRIT,
                    "PRESCRIT_ART_15_LOI_03_07_1978", synthese);
        }

        // ── Hiérarchie 5 : jeune travailleur sans jours pris ────────
        if (request.statut() == PeculeVacancesBeStatut.JEUNE_TRAVAILLEUR
                && joursPris.compareTo(BigDecimal.ZERO) <= 0) {
            String synthese = "Jeune travailleur 1re année (art. 9 AR"
                    + " 30/03/1967) sans aucun jour de congé pris : le"
                    + " droit aux vacances jeunes n'est pas activé."
                    + " Le jeune travailleur peut prendre jusqu'à 4"
                    + " semaines de vacances supplémentaires l'année"
                    + " civile suivant celle de l'entrée en service,"
                    + " indemnisées via l'allocation vacances jeunes"
                    + " (art. 9bis AR 30/03/1967, prise en charge"
                    + " ONEM). Action contre l'employeur sans objet"
                    + " tant qu'aucun jour n'a été pris.";
            return build(request,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, fraction,
                    false, joursDepuisFaitGenerateur,
                    PeculeVacancesBeVerdict.NON_DU_JOURS_INSUFFISANTS,
                    "JEUNE_TRAVAILLEUR_AUCUN_JOUR_PRIS", synthese);
        }

        // ── Cas qualifiant : EMPLOYE (ou JEUNE_TRAVAILLEUR avec jours)
        // selon le type de calcul demandé ─────────────────────────────
        switch (request.typeCalcul()) {
            case PECULE_SIMPLE: {
                String synthese = "Pécule simple employé (art. 38 AR"
                        + " 30/03/1967) : rémunération normale maintenue"
                        + " pendant les " + joursPris + " jours de"
                        + " congé pris, soit "
                        + montantSimple.setScale(2, RoundingMode.HALF_UP)
                        + " EUR (base mensuelle "
                        + remunMensuel.setScale(2, RoundingMode.HALF_UP)
                        + " EUR proratisée sur "
                        + JOURS_VACANCES_REFERENCE_REGIME_6J + " jours"
                        + " du régime 6 jours). À ajuster si régime"
                        + " 5 jours applicable (avocat à confirmer).";
                return build(request,
                        montantSimple, montantDouble, montantDepart,
                        montantSimple, fraction,
                        false, joursDepuisFaitGenerateur,
                        PeculeVacancesBeVerdict.DU_PECULE_SIMPLE_CALCULE,
                        "PECULE_SIMPLE_CALCULE", synthese);
            }
            case DOUBLE_PECULE: {
                String synthese = "Double pécule employé (art. 6 Lois"
                        + " coord. 28/06/1971 + art. 38 AR 30/03/1967) :"
                        + " 85 % du salaire mensuel brut = "
                        + montantDouble.setScale(2, RoundingMode.HALF_UP)
                        + " EUR (base mensuelle "
                        + remunMensuel.setScale(2, RoundingMode.HALF_UP)
                        + " EUR × 85 %), payable au moment des"
                        + " vacances principales annuelles.";
                return build(request,
                        montantSimple, montantDouble, montantDepart,
                        montantDouble, fraction,
                        false, joursDepuisFaitGenerateur,
                        PeculeVacancesBeVerdict.DU_DOUBLE_PECULE_CALCULE,
                        "DOUBLE_PECULE_CALCULE", synthese);
            }
            case PECULE_DEPART: {
                String synthese = "Pécule de départ employé (art. 7 Lois"
                        + " coord. 28/06/1971 + art. 46 AR 30/03/1967) :"
                        + " 15,34 % de la rémunération brute de"
                        + " l'exercice de vacances en cours ("
                        + remunExercice.setScale(2, RoundingMode.HALF_UP)
                        + " EUR × 15,34 % = "
                        + remunExercice.multiply(
                                POURCENT_PECULE_DEPART_EMPLOYE)
                                .setScale(2, RoundingMode.HALF_UP)
                        + " EUR) + 15,34 % de la rémunération brute de"
                        + " l'exercice précédent non encore liquidée ("
                        + remunExercicePrec.setScale(2, RoundingMode.HALF_UP)
                        + " EUR × 15,34 % = "
                        + remunExercicePrec.multiply(
                                POURCENT_PECULE_DEPART_EMPLOYE)
                                .setScale(2, RoundingMode.HALF_UP)
                        + " EUR), soit un total de "
                        + montantDepart.setScale(2, RoundingMode.HALF_UP)
                        + " EUR dû par l'employeur au plus tard à"
                        + " l'expiration du mois qui suit la fin du"
                        + " contrat.";
                return build(request,
                        montantSimple, montantDouble, montantDepart,
                        montantDepart, fraction,
                        false, joursDepuisFaitGenerateur,
                        PeculeVacancesBeVerdict.DU_PECULE_DEPART_CALCULE,
                        "PECULE_DEPART_CALCULE", synthese);
            }
            default:
                // Garde-fou — typeCalcul exhaustif sur l'enum.
                String syntheseGarde = "Type de calcul inconnu — analyse"
                        + " cas par cas requise.";
                return build(request,
                        montantSimple, montantDouble, montantDepart,
                        BigDecimal.ZERO, fraction,
                        false, joursDepuisFaitGenerateur,
                        PeculeVacancesBeVerdict.A_ANALYSER,
                        "TYPE_CALCUL_INCONNU", syntheseGarde);
        }
    }

    /** Pécule simple employé : rémunération mensuelle × (joursPris /
     *  jours de référence). Pour l'ouvrier, retourne 0 (via ONVA). */
    private static BigDecimal computePeculeSimple(
            PeculeVacancesBeStatut statut,
            BigDecimal remunMensuel,
            BigDecimal joursPris) {
        if (statut != PeculeVacancesBeStatut.EMPLOYE
                && statut != PeculeVacancesBeStatut.JEUNE_TRAVAILLEUR) {
            return BigDecimal.ZERO;
        }
        if (remunMensuel == null
                || remunMensuel.compareTo(BigDecimal.ZERO) <= 0
                || joursPris == null
                || joursPris.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return remunMensuel
                .multiply(joursPris)
                .divide(JOURS_VACANCES_REFERENCE_REGIME_6J, 4,
                        RoundingMode.HALF_UP);
    }

    /** Double pécule employé : 85 % du salaire mensuel brut. */
    private static BigDecimal computeDoublePecule(
            PeculeVacancesBeStatut statut,
            BigDecimal remunMensuel,
            BigDecimal remunExercice) {
        if (statut != PeculeVacancesBeStatut.EMPLOYE
                && statut != PeculeVacancesBeStatut.JEUNE_TRAVAILLEUR) {
            return BigDecimal.ZERO;
        }
        if (remunMensuel == null
                || remunMensuel.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return remunMensuel.multiply(POURCENT_DOUBLE_PECULE_EMPLOYE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Pécule de départ employé : 15,34 % × (exercice + exercice
     *  précédent non liquidé). */
    private static BigDecimal computePeculeDepart(
            PeculeVacancesBeStatut statut,
            BigDecimal remunExercice,
            BigDecimal remunExercicePrec) {
        if (statut != PeculeVacancesBeStatut.EMPLOYE
                && statut != PeculeVacancesBeStatut.JEUNE_TRAVAILLEUR) {
            return BigDecimal.ZERO;
        }
        BigDecimal sumBase = (remunExercice != null ? remunExercice
                : BigDecimal.ZERO).add(remunExercicePrec != null
                        ? remunExercicePrec : BigDecimal.ZERO);
        return sumBase.multiply(POURCENT_PECULE_DEPART_EMPLOYE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal fractionLegale(
            PeculeVacancesBeStatut statut,
            PeculeVacancesBeTypeCalcul typeCalcul) {
        if (statut == PeculeVacancesBeStatut.OUVRIER) {
            return POURCENT_OUVRIER_ONVA_TOTAL;
        }
        if (typeCalcul == PeculeVacancesBeTypeCalcul.DOUBLE_PECULE) {
            return POURCENT_DOUBLE_PECULE_EMPLOYE;
        }
        if (typeCalcul == PeculeVacancesBeTypeCalcul.PECULE_DEPART) {
            return POURCENT_PECULE_DEPART_EMPLOYE;
        }
        return BigDecimal.ZERO;
    }

    private static long computeJoursPrescription(
            LocalDate dateFinContrat, LocalDate dateReclamation) {
        if (dateFinContrat == null || dateReclamation == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(dateFinContrat, dateReclamation);
    }

    private static boolean isPrescrit(
            LocalDate dateFinContrat, LocalDate dateReclamation) {
        if (dateFinContrat == null || dateReclamation == null) {
            return false;
        }
        long jours = ChronoUnit.DAYS.between(dateFinContrat,
                dateReclamation);
        return jours > JOURS_PRESCRIPTION_POST_CONTRAT;
    }

    private static PeculeVacancesBeResult build(
            PeculeVacancesBeRequest request,
            BigDecimal montantSimple,
            BigDecimal montantDouble,
            BigDecimal montantDepart,
            BigDecimal montantTotal,
            BigDecimal fraction,
            boolean prescrit,
            long joursDepuisFaitGenerateur,
            PeculeVacancesBeVerdict verdict,
            String raison,
            String synthese) {
        return new PeculeVacancesBeResult(
                request.statut(),
                request.typeCalcul(),
                request.remunerationBruteAnnuelleExerciceEur(),
                request.remunerationMensuelleBruteEur(),
                request.joursCongesPris(),
                request.peculeDejaPaye(),
                request.remunerationBruteAnnuelleExercicePrecedentEur(),
                request.dateSortie(),
                request.dateFinContrat(),
                request.dateReclamation(),
                montantSimple.setScale(2, RoundingMode.HALF_UP),
                montantDouble.setScale(2, RoundingMode.HALF_UP),
                montantDepart.setScale(2, RoundingMode.HALF_UP),
                montantTotal.setScale(2, RoundingMode.HALF_UP),
                fraction,
                prescrit,
                joursDepuisFaitGenerateur,
                verdict,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(PeculeVacancesBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.statut() == null) {
            throw new IllegalArgumentException("statut est requis");
        }
        if (request.typeCalcul() == null) {
            throw new IllegalArgumentException("typeCalcul est requis");
        }
        if (request.remunerationBruteAnnuelleExerciceEur() == null) {
            throw new IllegalArgumentException(
                    "remunerationBruteAnnuelleExerciceEur est requise");
        }
        if (request.remunerationBruteAnnuelleExerciceEur()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "remunerationBruteAnnuelleExerciceEur doit être positive ou nulle");
        }
        if (request.remunerationMensuelleBruteEur() == null) {
            throw new IllegalArgumentException(
                    "remunerationMensuelleBruteEur est requise");
        }
        if (request.remunerationMensuelleBruteEur()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "remunerationMensuelleBruteEur doit être positive ou nulle");
        }
        if (request.joursCongesPris() == null) {
            throw new IllegalArgumentException("joursCongesPris est requis");
        }
        if (request.joursCongesPris().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "joursCongesPris doit être positif ou nul");
        }
        if (request.peculeDejaPaye() == null) {
            throw new IllegalArgumentException("peculeDejaPaye est requis");
        }
        if (request.remunerationBruteAnnuelleExercicePrecedentEur() == null) {
            throw new IllegalArgumentException(
                    "remunerationBruteAnnuelleExercicePrecedentEur est requise");
        }
        if (request.remunerationBruteAnnuelleExercicePrecedentEur()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "remunerationBruteAnnuelleExercicePrecedentEur doit être positive ou nulle");
        }
        if (request.dateReclamation() == null) {
            throw new IllegalArgumentException("dateReclamation est requise");
        }
    }
}
