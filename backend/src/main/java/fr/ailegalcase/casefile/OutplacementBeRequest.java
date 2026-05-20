package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-207-08 : payload de création/upsert d'une analyse de conformité de
 * l'<b>outplacement obligatoire pour salariés 45+</b> (CCT n°82 ; CCT n°82
 * bis ; Loi du 5 septembre 2001 art. 13).
 *
 * <p>Bean Validation appliquée côté contrôleur (400 si incomplet ou hors
 * borne). Les contrôles inter-champs supplémentaires (date naissance future,
 * date licenciement future, salarié majeur, validations conditionnelles de
 * l'offre) sont portés par {@link OutplacementBeService}.</p>
 *
 * <p>Champs :
 * <ul>
 *   <li>{@code dateLicenciement} — date du licenciement (ISO). Obligatoire.
 *       Sert au calcul de l'âge et de la date limite de l'offre
 *       (J + 15 jours).</li>
 *   <li>{@code dateNaissanceSalarie} — date de naissance (ISO). Obligatoire.
 *       Réutilisée de SF-207-06.</li>
 *   <li>{@code ancienneteAnnees} — ancienneté du salarié dans l'entreprise
 *       à la date du licenciement, en années avec décimales (BigDecimal,
 *       ≥ 0). Obligatoire. Comparée au seuil 1 an.</li>
 *   <li>{@code motifLicenciement} — motif de rupture conditionnant
 *       l'obligation (enum 4 valeurs). Obligatoire.</li>
 *   <li>{@code contratTempsPlein} — true = CCT n°82 ; false = CCT n°82 bis
 *       (régime temps partiel ≥ mi-temps). Obligatoire.</li>
 *   <li>{@code offreOutplacementRecue} — l'offre formelle d'outplacement
 *       a-t-elle été reçue ? Obligatoire.</li>
 *   <li>{@code dateOffreOutplacement} — date de réception de l'offre (ISO).
 *       Optionnel ; requis si {@code offreOutplacementRecue=true} (validé
 *       côté service, 400 sinon).</li>
 *   <li>{@code offreConformeCCT82} — l'offre est-elle conforme aux exigences
 *       CCT 82 (60 h / 12 mois, écrit, etc.) ? Optionnel ; requis si
 *       {@code offreOutplacementRecue=true}.</li>
 *   <li>{@code salarieAcceptantOffre} — le salarié a-t-il accepté l'offre ?
 *       Optionnel — null = pas encore décidé.</li>
 * </ul>
 *
 * <p>Pattern mirroré de {@link C4OnemChecklistRequest} (SF-207-02) — record
 * + Bean Validation + dates ISO {@link LocalDate} + ancienneté
 * {@link BigDecimal} pour précision sub-annuelle.</p>
 *
 * @see OutplacementBeCalculator
 */
public record OutplacementBeRequest(
        @NotNull(message = "dateLicenciement est requise")
        LocalDate dateLicenciement,

        @NotNull(message = "dateNaissanceSalarie est requise")
        LocalDate dateNaissanceSalarie,

        @NotNull(message = "ancienneteAnnees est requise")
        @DecimalMin(value = "0.00", message = "ancienneteAnnees doit être ≥ 0")
        BigDecimal ancienneteAnnees,

        @NotNull(message = "motifLicenciement est requis")
        OutplacementBeMotifLicenciement motifLicenciement,

        @NotNull(message = "contratTempsPlein est requis")
        Boolean contratTempsPlein,

        @NotNull(message = "offreOutplacementRecue est requise")
        Boolean offreOutplacementRecue,

        /**
         * Date de réception de l'offre. Requise si {@code offreOutplacementRecue=true}
         * (vérifié côté service — 400 sinon).
         */
        LocalDate dateOffreOutplacement,

        /**
         * Conformité de l'offre aux exigences CCT 82 / 82 bis. Requise si
         * {@code offreOutplacementRecue=true} (vérifié côté service).
         */
        Boolean offreConformeCCT82,

        /**
         * Acceptation/refus du salarié. Optionnel — null = pas encore décidé
         * (verdict OFFRE_CONFORME_RESPECTEE par défaut quand l'offre est
         * conforme).
         */
        Boolean salarieAcceptantOffre
) {}
