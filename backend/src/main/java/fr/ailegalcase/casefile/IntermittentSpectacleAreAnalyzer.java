package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-218-17 : analyseur d'ouverture des droits à l'allocation d'aide au retour à
 * l'emploi (ARE) de l'intermittent du spectacle relevant des annexes 8
 * (techniciens) et 10 (artistes) du règlement Unedic. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Conversion des cachets</b> (annexe 10) :
 *       {@code heuresCachets = nombreCachets × 12} (1 cachet = 12 h).</li>
 *   <li><b>Écrêtage des heures de formation</b> :
 *       {@code heuresFormationRetenues = min(heuresFormationDispensees, PLAFOND_FORMATION)}.</li>
 *   <li><b>Total d'heures retenues</b> :
 *       {@code heuresTotalesRetenues = heuresTravaillees12Mois + heuresCachets + heuresFormationRetenues}.</li>
 *   <li><b>Seuil d'affiliation</b> : 507 heures sur la période de référence de
 *       12 mois (constantes {@link BaremeIntermittentSpectacleAre#SEUIL_HEURES} et
 *       {@link BaremeIntermittentSpectacleAre#PERIODE_REFERENCE_MOIS}).</li>
 *   <li><b>Verdict d'ouverture</b> : total ≥ 507 → {@code OUVERTS} ; sinon
 *       {@code NON_OUVERTS}. Dans la marge de ± 10 h du seuil → {@code A_VERIFIER}
 *       (décompte à confirmer auprès de France Travail).</li>
 *   <li><b>Réexamen annuel</b> :
 *       {@code dateProchainExamen = dateFinContrat + 12 mois} (date anniversaire
 *       indicative du réexamen annuel des droits).</li>
 * </ul>
 *
 * <p>Le seuil (507 h), la période de référence (12 mois), la conversion du
 * cachet (12 h) et les plafonds sont fixés par la convention d'assurance
 * chômage — seuil et plafonds Unedic à actualiser à chaque renégociation de la
 * convention d'assurance chômage.
 */
public final class IntermittentSpectacleAreAnalyzer {

    static final String BASE_JURIDIQUE =
            "règlement d'assurance chômage Unedic — annexe 8 (ouvriers et "
                    + "techniciens du spectacle, de l'audiovisuel et du cinéma) et "
                    + "annexe 10 (artistes du spectacle) ; condition d'affiliation de "
                    + "507 heures sur les 12 mois précédant la fin du dernier contrat ; "
                    + "conversion des cachets (annexe 10) : 1 cachet = 12 heures ; "
                    + "heures d'enseignement assimilées dans la limite d'un plafond — "
                    + "seuil et plafonds à vérifier par l'avocat / France Travail (à "
                    + "actualiser à chaque renégociation de la convention d'assurance "
                    + "chômage)";

    private IntermittentSpectacleAreAnalyzer() {
    }

    /**
     * Analyse l'ouverture des droits à l'ARE : convertit les cachets, écrête les
     * heures de formation, calcule le total d'heures retenues, le compare au
     * seuil de 507 h et détermine le verdict ainsi que la date de réexamen
     * annuel.
     *
     * @param annexe annexe applicable (requis).
     * @param dateFinContrat fin du dernier contrat (requis, ne peut pas être
     *        future).
     * @param heuresTravaillees12Mois heures déclarées (requis, ≥ 0).
     * @param nombreCachets cachets à convertir (défaut 0, ≥ 0).
     * @param heuresFormationDispensees heures d'enseignement (défaut 0, ≥ 0).
     */
    public static IntermittentSpectacleAreResult analyze(IntermittentSpectacleAnnexe annexe,
                                                         LocalDate dateFinContrat,
                                                         Integer heuresTravaillees12Mois,
                                                         Integer nombreCachets,
                                                         Integer heuresFormationDispensees) {
        validate(annexe, dateFinContrat, heuresTravaillees12Mois, nombreCachets,
                heuresFormationDispensees);

        int heuresTravaillees = heuresTravaillees12Mois;
        int cachets = nombreCachets == null ? 0 : nombreCachets;
        int formationDispensees = heuresFormationDispensees == null ? 0 : heuresFormationDispensees;

        // Conversion des cachets (annexe 10) : 1 cachet = 12 heures.
        int heuresCachets = cachets * BaremeIntermittentSpectacleAre.HEURES_PAR_CACHET;
        // Écrêtage des heures de formation au plafond d'assimilation.
        int heuresFormationRetenues = Math.min(formationDispensees,
                BaremeIntermittentSpectacleAre.PLAFOND_FORMATION);

        int heuresTotalesRetenues = heuresTravaillees + heuresCachets + heuresFormationRetenues;

        int seuil = BaremeIntermittentSpectacleAre.SEUIL_HEURES;
        int heuresManquantes = Math.max(0, seuil - heuresTotalesRetenues);
        int heuresExcedentaires = Math.max(0, heuresTotalesRetenues - seuil);

        // Date d'anniversaire indicative du réexamen annuel des droits.
        LocalDate dateProchainExamen =
                dateFinContrat.plusMonths(BaremeIntermittentSpectacleAre.PERIODE_REFERENCE_MOIS);

        // Marge de vérification de ± 10 h autour du seuil → A_VERIFIER.
        int marge = BaremeIntermittentSpectacleAre.MARGE_VERIFICATION_HEURES;
        boolean dansLaMarge = Math.abs(heuresTotalesRetenues - seuil) <= marge;

        IntermittentSpectacleAreVerdict verdict;
        IntermittentSpectacleAreStatut statut;
        if (dansLaMarge) {
            verdict = IntermittentSpectacleAreVerdict.A_VERIFIER;
            statut = IntermittentSpectacleAreStatut.A_VERIFIER;
        } else if (heuresTotalesRetenues >= seuil) {
            verdict = IntermittentSpectacleAreVerdict.OUVERTS;
            statut = IntermittentSpectacleAreStatut.DROITS_OUVERTS;
        } else {
            verdict = IntermittentSpectacleAreVerdict.NON_OUVERTS;
            statut = IntermittentSpectacleAreStatut.DROITS_NON_OUVERTS;
        }

        return new IntermittentSpectacleAreResult(
                annexe,
                dateFinContrat,
                heuresTravaillees,
                heuresCachets,
                heuresFormationRetenues,
                heuresTotalesRetenues,
                seuil,
                heuresManquantes,
                heuresExcedentaires,
                dateProchainExamen,
                verdict,
                statut,
                BASE_JURIDIQUE);
    }

    private static void validate(IntermittentSpectacleAnnexe annexe,
                                 LocalDate dateFinContrat,
                                 Integer heuresTravaillees12Mois,
                                 Integer nombreCachets,
                                 Integer heuresFormationDispensees) {
        if (annexe == null) {
            throw new IllegalArgumentException("annexe est requise");
        }
        if (dateFinContrat == null) {
            throw new IllegalArgumentException("dateFinContrat est requise");
        }
        if (dateFinContrat.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("dateFinContrat ne peut pas être future");
        }
        if (heuresTravaillees12Mois == null) {
            throw new IllegalArgumentException("heuresTravaillees12Mois est requis");
        }
        if (heuresTravaillees12Mois < 0) {
            throw new IllegalArgumentException("heuresTravaillees12Mois ne peut pas être négatif");
        }
        if (nombreCachets != null && nombreCachets < 0) {
            throw new IllegalArgumentException("nombreCachets ne peut pas être négatif");
        }
        if (heuresFormationDispensees != null && heuresFormationDispensees < 0) {
            throw new IllegalArgumentException("heuresFormationDispensees ne peut pas être négatif");
        }
    }
}
