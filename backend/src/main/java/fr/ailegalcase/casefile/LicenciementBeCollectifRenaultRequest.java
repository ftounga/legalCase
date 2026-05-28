package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * SF-219-07 : payload de création / mise à jour de la checklist de
 * conformité de la procédure de licenciement collectif Loi Renault
 * (Loi du 13/02/1998, CCT n° 24, CCT n° 39).
 *
 * <p>Outil <b>BE-only</b> — le PSE français (Code du travail
 * art. L. 1233-21 et s.) est procéduralement très différent (DIRECCTE
 * / DDETS, validation/homologation, calendrier d'expertise CSE) et ne
 * peut pas être mappé à la procédure Renault.</p>
 *
 * <h2>Logique métier (checklist procédurale, pas calcul)</h2>
 * <ol>
 *   <li><b>Seuil de déclenchement</b> — selon la taille de l'entreprise
 *       (PME 20-99 → ≥ 10 licenciements/60 j ; grande 100-299 → ≥ 10 %
 *       de l'effectif ; très grande ≥ 300 → ≥ 30 licenciements/60 j).
 *       Sous le seuil : pas de procédure Renault (procédure CCT n° 9
 *       générale subsiste, hors scope).</li>
 *   <li><b>Phase 1 — Information CE / CPPT / DS</b> — notification
 *       écrite préalable + documents légaux (motifs économiques, nombre
 *       et catégories de travailleurs, critères de sélection, période).</li>
 *   <li><b>Phase 2 — Consultation effective</b> — réunion(s) +
 *       réponses motivées écrites de l'employeur aux questions et
 *       contre-propositions.</li>
 *   <li><b>Phase 3 — Décision + notification autorité régionale</b> —
 *       notification écrite à la directrice du service subrégional de
 *       l'emploi (Forem / Actiris / VDAB) déclenche le délai d'attente
 *       de 30 jours pendant lequel <b>aucun préavis ne peut être
 *       notifié</b>.</li>
 *   <li><b>Délai d'attente 30 jours</b> — calculé à partir de la date de
 *       notification à l'autorité ; tout préavis notifié pendant ce
 *       délai est nul + indemnité spéciale art. 67 due.</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateProjet} — date à laquelle l'employeur a annoncé son
 *       intention de procéder à un licenciement collectif (point de
 *       départ du calcul de la fenêtre 60 jours).</li>
 *   <li>{@code tailleEntreprise} — tranche de taille (PME/grande/très
 *       grande).</li>
 *   <li>{@code effectifMoyen} — effectif moyen sur les 4 trimestres
 *       civils précédents (Loi 13/02/1998 art. 2).</li>
 *   <li>{@code nombreLicenciementsEnvisages} — nombre de licenciements
 *       projetés sur la fenêtre de 60 jours.</li>
 *   <li>{@code phaseAtteinte} — phase courante de la procédure (aucune
 *       / information / consultation / décision).</li>
 *   <li>{@code informationEcriteCePrealable} — notification écrite
 *       préalable au CE / CPPT / DS effectuée.</li>
 *   <li>{@code documentsLegauxCommuniques} — documents légaux (motifs,
 *       catégories, critères) communiqués aux représentants.</li>
 *   <li>{@code consultationEffectiveTenue} — réunion(s) effectivement
 *       tenue(s) avec débat réel.</li>
 *   <li>{@code reponsesMotiveesEmployeur} — réponses écrites motivées
 *       de l'employeur aux questions / contre-propositions.</li>
 *   <li>{@code notificationAutoriteRegionaleFaite} — notification écrite
 *       à l'autorité régionale (Forem / Actiris / VDAB) effectuée.</li>
 *   <li>{@code dateNotificationAutorite} — date de la notification à
 *       l'autorité (point de départ du délai d'attente 30 jours) —
 *       requise uniquement si {@code notificationAutoriteRegionaleFaite}
 *       est vrai.</li>
 *   <li>{@code datePremierPreavisNotifie} — date du 1er préavis
 *       individuel notifié (facultatif : si non renseigné, on
 *       considère qu'aucun préavis n'a encore été envoyé).</li>
 * </ul>
 */
public record LicenciementBeCollectifRenaultRequest(

        @NotNull(message = "dateProjet est requise")
        LocalDate dateProjet,

        @NotNull(message = "tailleEntreprise est requise")
        LicenciementBeCollectifRenaultTailleEntreprise tailleEntreprise,

        @NotNull(message = "effectifMoyen est requis")
        @PositiveOrZero(message = "effectifMoyen doit être ≥ 0")
        Integer effectifMoyen,

        @NotNull(message = "nombreLicenciementsEnvisages est requis")
        @PositiveOrZero(message = "nombreLicenciementsEnvisages doit être ≥ 0")
        Integer nombreLicenciementsEnvisages,

        @NotNull(message = "phaseAtteinte est requise")
        LicenciementBeCollectifRenaultPhase phaseAtteinte,

        @NotNull(message = "informationEcriteCePrealable est requis")
        Boolean informationEcriteCePrealable,

        @NotNull(message = "documentsLegauxCommuniques est requis")
        Boolean documentsLegauxCommuniques,

        @NotNull(message = "consultationEffectiveTenue est requis")
        Boolean consultationEffectiveTenue,

        @NotNull(message = "reponsesMotiveesEmployeur est requis")
        Boolean reponsesMotiveesEmployeur,

        @NotNull(message = "notificationAutoriteRegionaleFaite est requis")
        Boolean notificationAutoriteRegionaleFaite,

        LocalDate dateNotificationAutorite,

        LocalDate datePremierPreavisNotifie
) {
}
