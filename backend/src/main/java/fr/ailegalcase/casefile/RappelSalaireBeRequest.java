package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-213-02 : payload de création / mise à jour d'une analyse de rappel de
 * salaire en droit belge du travail (Loi du 12/04/1965 art. 10 — taux
 * moratoires 10 % ; Loi du 03/07/1978 art. 15 — prescription).
 *
 * <p>Bean Validation appliquée côté contrôleur (400 si incomplet ou hors
 * bornes). Les validations inter-champs (cohérence des dates, présence de
 * {@code dateRupture} pour {@code POST_RUPTURE}) sont portées par
 * {@link RappelSalaireBeCalculator}.</p>
 *
 * <p>Pattern mirroré de {@link ClauseNonConcurrenceBeRequest} (SF-213-01) —
 * record + Bean Validation.</p>
 *
 * @see RappelSalaireBeCalculator
 */
public record RappelSalaireBeRequest(

        /** Arriéré de salaire brut réclamé (€). */
        @NotNull(message = "montantBrut est requis")
        @DecimalMin(value = "0.01", message = "montantBrut doit être strictement positif")
        BigDecimal montantBrut,

        /** Début de la période d'arriéré (date de début d'exigibilité). */
        @NotNull(message = "dateDebutPeriode est requise")
        LocalDate dateDebutPeriode,

        /** Fin de la période d'arriéré (= date d'exigibilité principale). */
        @NotNull(message = "dateFinPeriode est requise")
        LocalDate dateFinPeriode,

        /**
         * Date de rupture du contrat (optionnelle). Requise si
         * {@code typeArriere = POST_RUPTURE} — point de départ du délai de
         * prescription 1 an.
         */
        LocalDate dateRupture,

        /**
         * Date d'action envisagée (par défaut {@code LocalDate.now()} côté
         * calculateur). Pivot pour le calcul des intérêts courus et des
         * jours restants avant prescription.
         */
        LocalDate dateActionEnvisagee,

        /** Type d'arriéré — pilote le régime de prescription applicable. */
        @NotNull(message = "typeArriere est requis")
        RappelSalaireBeTypeArriereEnum typeArriere
) {}
