package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-45 : analyseur du <b>congé parental d'éducation</b> (art. L.1225-47 à
 * L.1225-60 CT, F-DT-78). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ; distinct du
 * congé de paternité/maternité F-212 et du congé pour évènements familiaux
 * F-DT-76) :
 * <ul>
 *   <li><b>Éligibilité (L.1225-47)</b> — un an d'ancienneté minimum à la date de
 *       naissance / d'arrivée de l'enfant adopté : {@code eligible =
 *       ancienneteMois >= 12}. Sinon {@code statut = NON_ELIGIBLE}, pas de date
 *       de fin.</li>
 *   <li><b>Date de fin maximale (L.1225-48)</b> — {@code dateFinMax =
 *       dateNaissanceOuAdoption + 3 ans} (jusqu'au 3e anniversaire de l'enfant ;
 *       un an renouvelable deux fois). En cas d'adoption, durée également
 *       plafonnée à 3 ans à compter de l'arrivée — règles spécifiques signalées
 *       en note.</li>
 *   <li><b>Protection / réintégration (L.1225-55)</b> — à l'issue du congé, le
 *       salarié retrouve son précédent emploi ou un emploi similaire assorti
 *       d'une rémunération au moins équivalente.</li>
 *   <li><b>PreParE (information)</b> — le congé peut ouvrir droit à la prestation
 *       partagée d'éducation de l'enfant (PreParE) versée par la CAF ; montant
 *       non calculé ici.</li>
 * </ul>
 *
 * <p>Base juridique annotée « à vérifier par avocat ».
 */
public final class CongeParentalEducationAnalyzer {

    /** Ancienneté minimale requise (mois) à la date de naissance / adoption. */
    static final int ANCIENNETE_MIN_MOIS = 12;

    /** Durée maximale du congé en mois (jusqu'au 3e anniversaire de l'enfant). */
    static final int DUREE_MAX_MOIS = 36;

    /** Nombre d'années jusqu'au 3e anniversaire de l'enfant. */
    static final int ANNEES_MAX = 3;

    static final String BASE_JURIDIQUE =
            "art. L.1225-47 à L.1225-60 du Code du travail — congé parental "
                    + "d'éducation : droit ouvert au salarié justifiant d'au moins un an "
                    + "d'ancienneté à la date de naissance ou de l'arrivée au foyer de "
                    + "l'enfant adopté de moins de 16 ans (art. L.1225-47) ; congé total "
                    + "ou activité à temps partiel ; durée initiale d'un an, renouvelable "
                    + "deux fois, jusqu'au 3e anniversaire de l'enfant (règles spécifiques "
                    + "en cas d'adoption ou de naissances multiples, art. L.1225-48) ; à "
                    + "l'issue du congé, réintégration dans le précédent emploi ou un "
                    + "emploi similaire assorti d'une rémunération au moins équivalente "
                    + "(art. L.1225-55) ; le congé peut ouvrir droit à la PreParE versée "
                    + "par la CAF (à vérifier par avocat)";

    private CongeParentalEducationAnalyzer() {
    }

    /**
     * Analyse l'éligibilité au congé parental d'éducation et sa date de fin
     * maximale.
     */
    public static CongeParentalEducationResult analyze(
            Integer ancienneteMois,
            CongeParentalEducationModalite modalite,
            Integer nombreEnfants,
            LocalDate dateNaissanceOuAdoption) {

        validate(ancienneteMois, modalite, nombreEnfants, dateNaissanceOuAdoption);

        List<String> notes = new ArrayList<>();
        boolean eligible = ancienneteMois >= ANCIENNETE_MIN_MOIS;

        if (!eligible) {
            notes.add("Ancienneté insuffisante : un an d'ancienneté minimum à la date de "
                    + "naissance ou d'arrivée de l'enfant est requis pour bénéficier du "
                    + "congé parental d'éducation (art. L.1225-47 CT). Ancienneté retenue : "
                    + ancienneteMois + " mois (< 12 mois).");
            return new CongeParentalEducationResult(
                    CongeParentalEducationStatut.NON_ELIGIBLE,
                    ancienneteMois,
                    modalite,
                    nombreEnfants,
                    dateNaissanceOuAdoption,
                    null,
                    0,
                    true,
                    true,
                    List.copyOf(notes),
                    BASE_JURIDIQUE);
        }

        LocalDate dateFinMax = dateNaissanceOuAdoption.plusYears(ANNEES_MAX);

        notes.add("Condition d'ancienneté remplie : " + ancienneteMois + " mois "
                + "(≥ 12 mois requis à la date de naissance / adoption, art. L.1225-47 CT).");
        notes.add("Date de fin maximale du droit : " + dateFinMax + " — le congé parental "
                + "d'éducation peut être pris jusqu'au 3e anniversaire de l'enfant (durée "
                + "initiale d'un an, renouvelable deux fois, art. L.1225-48 CT).");
        switch (modalite) {
            case TEMPS_PLEIN -> notes.add("Modalité retenue : congé total (suspension du "
                    + "contrat de travail).");
            case TEMPS_PARTIEL -> notes.add("Modalité retenue : activité à temps partiel "
                    + "(la durée du travail est réduite ; le salarié continue de travailler "
                    + "pour élever l'enfant).");
        }
        if (nombreEnfants > 1) {
            notes.add("Naissances / adoptions multiples (" + nombreEnfants + " enfants) : "
                    + "des règles spécifiques de durée peuvent s'appliquer (art. L.1225-48 "
                    + "CT — à vérifier par avocat).");
        }
        notes.add("Adoption : en cas d'adoption d'un enfant, la durée du congé est également "
                + "plafonnée à 3 ans à compter de l'arrivée de l'enfant au foyer ; pour un "
                + "enfant adopté de plus de 3 ans, la durée maximale est réduite (règles "
                + "spécifiques d'adoption — à vérifier par avocat).");
        notes.add("À l'issue du congé, le salarié retrouve son précédent emploi ou un emploi "
                + "similaire assorti d'une rémunération au moins équivalente (art. L.1225-55 CT).");
        notes.add("Le congé parental d'éducation peut ouvrir droit à la prestation partagée "
                + "d'éducation de l'enfant (PreParE) versée par la CAF — information, montant "
                + "non calculé ici.");

        return new CongeParentalEducationResult(
                CongeParentalEducationStatut.ELIGIBLE,
                ancienneteMois,
                modalite,
                nombreEnfants,
                dateNaissanceOuAdoption,
                dateFinMax,
                DUREE_MAX_MOIS,
                true,
                true,
                List.copyOf(notes),
                BASE_JURIDIQUE);
    }

    private static void validate(Integer ancienneteMois,
                                 CongeParentalEducationModalite modalite,
                                 Integer nombreEnfants,
                                 LocalDate dateNaissanceOuAdoption) {
        if (ancienneteMois == null) {
            throw new IllegalArgumentException("ancienneteMois est requis");
        }
        if (ancienneteMois < 0) {
            throw new IllegalArgumentException("ancienneteMois doit être positif ou nul");
        }
        if (modalite == null) {
            throw new IllegalArgumentException("modalite est requise");
        }
        if (nombreEnfants == null) {
            throw new IllegalArgumentException("nombreEnfants est requis");
        }
        if (nombreEnfants < 1) {
            throw new IllegalArgumentException("nombreEnfants doit être supérieur ou égal à 1");
        }
        if (dateNaissanceOuAdoption == null) {
            throw new IllegalArgumentException("dateNaissanceOuAdoption est requise");
        }
    }
}
