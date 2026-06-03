package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SF-221-03 — calculateur d'éligibilité au statut de RÉSIDENT LONGUE DURÉE UE
 * en Belgique.
 *
 * <p>Sources <i>(à vérifier par avocat BE 2026)</i> :
 * <ul>
 *   <li>Loi du 15/12/1980 art. 15bis — statut de résident de longue durée, transposant
 *       la directive 2003/109/CE : 5 ans de séjour légal ininterrompu + ressources
 *       stables, régulières et suffisantes + assurance maladie + condition
 *       d'intégration (seuil indicatif, exceptions selon la situation).</li>
 * </ul>
 *
 * <p>Seuil de durée : 5 ans = 60 mois de séjour légal ininterrompu.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — un outil = une situation : ne traite que
 * l'accès au statut de RÉSIDENT LONGUE DURÉE UE (conditions propres ressources /
 * assurance / intégration ET mobilité intra-UE). DISTINCT de F-IM-54 (carte B,
 * séjour ILLIMITÉ NATIONAL sans mobilité UE).
 */
public final class ResidenceLongueDureeUeBeCalculator {

    /** Seuil de durée — 5 ans = 60 mois de séjour légal ininterrompu (art. 15bis, indicatif). */
    public static final int SEUIL_DUREE_MOIS = 60;

    private static final List<String> BASES_JURIDIQUES = List.of(
            "Loi du 15/12/1980 art. 15bis (statut de résident de longue durée — 5 ans de "
                    + "séjour légal ininterrompu) (à vérifier par avocat)",
            "Directive 2003/109/CE (résidents de pays tiers de longue durée — conditions "
                    + "ressources / assurance maladie / intégration + mobilité intra-UE) "
                    + "(à vérifier par avocat)");

    private ResidenceLongueDureeUeBeCalculator() {
    }

    /** Surcharge utilisant la date du jour comme référence. */
    public static ResidenceLongueDureeUeBeResult compute(LocalDate dateDebutSejourLegal,
                                                         Boolean sejourLegalIninterrompu,
                                                         Boolean ressourcesStablesSuffisantes,
                                                         Boolean assuranceMaladie,
                                                         Boolean conditionIntegrationRemplie,
                                                         Boolean absencesHorsUeExcessives) {
        return compute(dateDebutSejourLegal, sejourLegalIninterrompu, ressourcesStablesSuffisantes,
                assuranceMaladie, conditionIntegrationRemplie, absencesHorsUeExcessives,
                LocalDate.now());
    }

    /**
     * Calcule le verdict d'éligibilité avec une date de référence injectée (testabilité).
     *
     * @param today date de référence (généralement {@code LocalDate.now()}).
     */
    public static ResidenceLongueDureeUeBeResult compute(LocalDate dateDebutSejourLegal,
                                                         Boolean sejourLegalIninterrompu,
                                                         Boolean ressourcesStablesSuffisantes,
                                                         Boolean assuranceMaladie,
                                                         Boolean conditionIntegrationRemplie,
                                                         Boolean absencesHorsUeExcessives,
                                                         LocalDate today) {
        validate(dateDebutSejourLegal, sejourLegalIninterrompu, ressourcesStablesSuffisantes,
                assuranceMaladie, conditionIntegrationRemplie, absencesHorsUeExcessives, today);

        boolean ininterrompu = sejourLegalIninterrompu;
        boolean ressources = ressourcesStablesSuffisantes;
        boolean assurance = assuranceMaladie;
        boolean integration = conditionIntegrationRemplie;
        boolean absencesExcessives = absencesHorsUeExcessives;

        int dureeSejourMois = (int) ChronoUnit.MONTHS.between(dateDebutSejourLegal, today);
        int moisRestants = Math.max(0, SEUIL_DUREE_MOIS - dureeSejourMois);

        List<String> conditionsManquantes = new ArrayList<>();
        if (!ressources) {
            conditionsManquantes.add("Ressources stables, régulières et suffisantes");
        }
        if (!assurance) {
            conditionsManquantes.add("Assurance maladie couvrant l'ensemble des risques");
        }
        if (!integration) {
            conditionsManquantes.add("Condition d'intégration");
        }

        List<String> messages = new ArrayList<>();

        ResidenceLongueDureeUeBeVerdict verdict;
        if (dureeSejourMois < SEUIL_DUREE_MOIS) {
            verdict = ResidenceLongueDureeUeBeVerdict.DUREE_INSUFFISANTE;
            messages.add("Durée de séjour légal insuffisante : " + dureeSejourMois
                    + " mois sur les 60 requis. Il reste " + moisRestants
                    + " mois avant l'ouverture du droit (seuil indicatif art. 15bis).");
        } else if (!ininterrompu || absencesExcessives) {
            verdict = ResidenceLongueDureeUeBeVerdict.CONTINUITE_ROMPUE;
            if (!ininterrompu) {
                messages.add("Le séjour légal n'a pas été ininterrompu : la continuité de "
                        + "5 ans requise est rompue.");
            }
            if (absencesExcessives) {
                messages.add("Les absences hors UE dépassent les limites admises : la "
                        + "continuité du séjour est rompue.");
            }
        } else if (!conditionsManquantes.isEmpty()) {
            verdict = ResidenceLongueDureeUeBeVerdict.CONDITIONS_MATERIELLES_NON_REUNIES;
            messages.add("Durée et continuité acquises mais une ou plusieurs conditions "
                    + "matérielles ne sont pas réunies : "
                    + String.join(", ", conditionsManquantes) + ".");
        } else {
            verdict = ResidenceLongueDureeUeBeVerdict.ELIGIBLE;
            messages.add("Conditions réunies : au moins 5 ans (60 mois) de séjour légal "
                    + "ininterrompu, ressources stables et suffisantes, assurance maladie et "
                    + "condition d'intégration remplie. L'accès au statut de résident longue "
                    + "durée UE (mobilité intra-UE) est en principe ouvert.");
        }

        if (verdict != ResidenceLongueDureeUeBeVerdict.DUREE_INSUFFISANTE) {
            messages.add("Durée de séjour légal écoulée : " + dureeSejourMois + " mois "
                    + "(seuil indicatif art. 15bis = 60 mois).");
        }

        return new ResidenceLongueDureeUeBeResult(
                dateDebutSejourLegal,
                ininterrompu,
                ressources,
                assurance,
                integration,
                absencesExcessives,
                verdict,
                dureeSejourMois,
                moisRestants,
                Collections.unmodifiableList(conditionsManquantes),
                BASES_JURIDIQUES,
                Collections.unmodifiableList(messages));
    }

    private static void validate(LocalDate dateDebutSejourLegal,
                                 Boolean sejourLegalIninterrompu,
                                 Boolean ressourcesStablesSuffisantes,
                                 Boolean assuranceMaladie,
                                 Boolean conditionIntegrationRemplie,
                                 Boolean absencesHorsUeExcessives,
                                 LocalDate today) {
        if (dateDebutSejourLegal == null) {
            throw new IllegalArgumentException("dateDebutSejourLegal est requise");
        }
        if (sejourLegalIninterrompu == null) {
            throw new IllegalArgumentException("sejourLegalIninterrompu est requis");
        }
        if (ressourcesStablesSuffisantes == null) {
            throw new IllegalArgumentException("ressourcesStablesSuffisantes est requis");
        }
        if (assuranceMaladie == null) {
            throw new IllegalArgumentException("assuranceMaladie est requis");
        }
        if (conditionIntegrationRemplie == null) {
            throw new IllegalArgumentException("conditionIntegrationRemplie est requis");
        }
        if (absencesHorsUeExcessives == null) {
            throw new IllegalArgumentException("absencesHorsUeExcessives est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateDebutSejourLegal.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateDebutSejourLegal ne peut pas être dans le futur");
        }
    }
}
