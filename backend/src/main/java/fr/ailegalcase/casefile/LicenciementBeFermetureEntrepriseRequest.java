package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-06 (mini-spec SF-219-07) : payload de création / mise à jour
 * de l'analyse <i>licenciement BE fermeture entreprise</i> (Loi du
 * 26/06/2002 ; AR du 23/03/2007 ; Fonds Fermeture Entreprises FFE).
 *
 * <p>Outil <b>BE-only</b> — aucun équivalent strict en droit français
 * (le FNGS français couvre la garantie des salaires en cas de
 * redressement / liquidation, mais ne porte pas d'indemnité de
 * fermeture forfaitaire indépendante).</p>
 *
 * <h2>Logique métier</h2>
 * <ol>
 *   <li><b>Qualification de la fermeture</b> — seules certaines
 *       fermetures qualifient (cessation définitive, faillite
 *       judiciaire). Les cessations partielles, transferts CCT 32bis,
 *       fusions sans suppression et fermetures temporaires sont
 *       écartées.</li>
 *   <li><b>Seuil d'effectif</b> — V1 régime général : entreprise ≥ 20
 *       travailleurs ETP (l'AR 23/03/2007 prévoit des régimes
 *       dérogatoires pour la PME &lt; 20 ETP, non couverts V1).</li>
 *   <li><b>Ancienneté minimale</b> — 1 an au moins à la date de
 *       fermeture (Loi 26/06/2002 art. 4).</li>
 *   <li><b>Calcul indemnité</b> — montant forfaitaire par année
 *       d'ancienneté + montant variable selon l'âge (supplément ≥ 45
 *       ans). Montants indicatifs à confirmer par le FFE selon le
 *       barème en vigueur l'année de fermeture.</li>
 *   <li><b>Créances FFE</b> — si insolvabilité avérée / faillite, le
 *       FFE reprend salaires impayés + pécule + indemnité de rupture
 *       (plafonds légaux applicables — non recalculés V1, indicatif).</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateNaissance} — détermine l'âge à la fermeture (calcul
 *       du supplément ≥ 45 ans).</li>
 *   <li>{@code dateDebutContrat} — détermine l'ancienneté à la
 *       fermeture (seuil 1 an).</li>
 *   <li>{@code dateFermeture} — date de la fermeture (cessation
 *       officielle d'activité ou jugement de faillite).</li>
 *   <li>{@code remunerationMensuelleBrute} — base de référence pour le
 *       calcul des créances reprises FFE.</li>
 *   <li>{@code typeFermeture} — nature de la fermeture (qualifiante ou
 *       non).</li>
 *   <li>{@code statutEmployeur} — solvabilité (déclenche la reprise des
 *       créances par le FFE).</li>
 *   <li>{@code effectifEtp} — effectif équivalent temps plein moyen sur
 *       les 4 trimestres précédant la fermeture (seuil 20).</li>
 *   <li>{@code salairesImpayes} — montant des salaires impayés à
 *       reprendre par le FFE (0 si solvable).</li>
 *   <li>{@code peculeVacancesImpaye} — pécule de vacances dû et impayé.</li>
 *   <li>{@code indemniteRuptureImpayee} — indemnité compensatoire de
 *       préavis due et impayée.</li>
 * </ul>
 */
public record LicenciementBeFermetureEntrepriseRequest(

        @NotNull(message = "dateNaissance est requise")
        LocalDate dateNaissance,

        @NotNull(message = "dateDebutContrat est requise")
        LocalDate dateDebutContrat,

        @NotNull(message = "dateFermeture est requise")
        LocalDate dateFermeture,

        @NotNull(message = "remunerationMensuelleBrute est requise")
        @PositiveOrZero(message = "remunerationMensuelleBrute doit être ≥ 0")
        BigDecimal remunerationMensuelleBrute,

        @NotNull(message = "typeFermeture est requise")
        LicenciementBeFermetureEntrepriseTypeFermeture typeFermeture,

        @NotNull(message = "statutEmployeur est requise")
        LicenciementBeFermetureEntrepriseStatutEmployeur statutEmployeur,

        @NotNull(message = "effectifEtp est requise")
        @PositiveOrZero(message = "effectifEtp doit être ≥ 0")
        Integer effectifEtp,

        @NotNull(message = "salairesImpayes est requise")
        @PositiveOrZero(message = "salairesImpayes doit être ≥ 0")
        BigDecimal salairesImpayes,

        @NotNull(message = "peculeVacancesImpaye est requise")
        @PositiveOrZero(message = "peculeVacancesImpaye doit être ≥ 0")
        BigDecimal peculeVacancesImpaye,

        @NotNull(message = "indemniteRuptureImpayee est requise")
        @PositiveOrZero(message = "indemniteRuptureImpayee doit être ≥ 0")
        BigDecimal indemniteRuptureImpayee
) {
}
