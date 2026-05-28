package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-219-31 : payload de création / mise à jour de l'analyse <i>Congé
 * paternité / naissance BE</i> (Loi du 03/07/1978 art. 30 § 2 + Loi du
 * 12/08/2000 + Loi du 07/04/2023 réforme Deal pour l'emploi 2022).
 *
 * <h2>Champs requis</h2>
 * <ul>
 *   <li>{@link #statutTravailleur} — salarié / indépendant / agent
 *       public / autre (impacte l'éligibilité au régime art. 30 § 2 Loi
 *       03/07/1978 par opposition aux régimes spécifiques indépendant
 *       Loi 13/04/2019 et public AR 19/11/1998).</li>
 *   <li>{@link #lienFiliation} — type de lien parental ouvrant droit
 *       (père biologique, comaternité, reconnaissance, cohabitant légal
 *       ou marié) — base Loi 03/07/1978 art. 30 § 2 al. 1 modifié par
 *       Loi 07/04/2023 ouverte à tous les co-parents.</li>
 *   <li>{@link #etapeProcedure} — étape d'avancement (avant naissance,
 *       naissance survenue, notification faite, prise en cours, prise
 *       complète, délai dépassé).</li>
 *   <li>{@link #filiationEtablie} — preuve du lien (acte de naissance,
 *       reconnaissance, comaternité, acte de mariage / cohabitation
 *       légale).</li>
 *   <li>{@link #contratTravailEnCours} — contrat salarié en cours au
 *       moment de la naissance (condition d'ouverture art. 30 § 2).</li>
 *   <li>{@link #notificationEmployeurFaite} — l'employeur a-t-il été
 *       avisé conformément à l'AR du 17/10/1994 ?</li>
 * </ul>
 *
 * <h2>Champs optionnels</h2>
 * <ul>
 *   <li>{@link #dateNaissance} — date de naissance de l'enfant (clé
 *       pour déterminer la durée applicable et calculer l'échéance des
 *       4 mois art. 30 § 2 al. 2).</li>
 *   <li>{@link #dateNotificationEmployeur} — date d'envoi du document
 *       d'information à l'employeur (point de départ de la fenêtre de
 *       protection 5 mois art. 30 § 4).</li>
 *   <li>{@link #joursDejaPrisOuvrables} — nombre de jours ouvrables
 *       déjà pris (un demi-jour compte pour 0.5 — modélisé arrondi
 *       inférieur côté UI, valeur entière côté backend).</li>
 *   <li>{@link #dateFinPriseEffective} — date du dernier jour pris si
 *       le congé est terminé (point de départ de la protection 5 mois
 *       résiduelle).</li>
 * </ul>
 */
public record CongePaterniteNaissanceBeRequest(

        @NotNull(message = "statutTravailleur est requis")
        CongePaterniteNaissanceBeStatutTravailleur statutTravailleur,

        @NotNull(message = "lienFiliation est requis")
        CongePaterniteNaissanceBeLienFiliation lienFiliation,

        @NotNull(message = "etapeProcedure est requis")
        CongePaterniteNaissanceBeEtape etapeProcedure,

        @NotNull(message = "filiationEtablie est requis")
        Boolean filiationEtablie,

        @NotNull(message = "contratTravailEnCours est requis")
        Boolean contratTravailEnCours,

        @NotNull(message = "notificationEmployeurFaite est requis")
        Boolean notificationEmployeurFaite,

        LocalDate dateNaissance,

        LocalDate dateNotificationEmployeur,

        @Min(value = 0,
                message = "joursDejaPrisOuvrables ne peut pas être négatif")
        Integer joursDejaPrisOuvrables,

        LocalDate dateFinPriseEffective
) {
}
