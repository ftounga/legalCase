package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * SF-219-05 : payload de création / mise à jour de l'analyse de conformité
 * de l'outplacement BE général au titre du régime "préavis ≥ 30 semaines".
 *
 * <p>Outil <b>BE-only</b> — Loi du 05/09/2001 art. 11 + AR du 21/10/2007.
 * Aucun équivalent strict en droit français. Distinct de l'outplacement
 * obligatoire 45+ ans (F-207 SF-207-08) qui s'applique quelle que soit la
 * durée du préavis.</p>
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Licenciement effectif</b> à l'initiative de l'employeur
 *       (la démission ferme le régime).</li>
 *   <li>Motif <b>non grave</b> (le licenciement pour motif grave exclut
 *       le droit à l'outplacement — art. 11/3 § 1er Loi 05/09/2001).</li>
 *   <li>Préavis notifié (ou indemnité de rupture équivalente) ≥
 *       <b>30 semaines</b>.</li>
 *   <li><b>Offre écrite et formelle</b> dans les <b>15 jours civils</b>
 *       suivant la fin du contrat.</li>
 *   <li><b>Programme ≥ 60 heures</b> réparties sur une période
 *       ≤ <b>12 mois</b> (AR 21/10/2007 — art. 1er § 1er).</li>
 *   <li>Forme conforme : offre individuelle écrite, contenu minimum
 *       (bilan personnel, accompagnement projet professionnel, support
 *       psychologique, coaching, formation).</li>
 * </ol>
 *
 * <p>Si toutes remplies → verdict {@code CONFORME}. Sinon, hiérarchie de
 * raisons (la première non remplie l'emporte) — cf.
 * {@link OutplacementBeGeneral30semVerdict}.</p>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code licenciementEffectif} — {@code true} si l'employeur licencie
 *       (vs démission).</li>
 *   <li>{@code motifGrave} — {@code true} si licenciement pour motif grave
 *       (au sens de l'art. 35 Loi du 03/07/1978).</li>
 *   <li>{@code semainesPreavisOuIndemnite} — durée du préavis notifié (ou
 *       indemnité de rupture équivalente) en semaines.</li>
 *   <li>{@code dateFinContrat} — date à laquelle le contrat prend fin
 *       (fin du préavis presté ou date du congé pour indemnité de rupture).</li>
 *   <li>{@code offreNotifiee} — {@code true} si l'employeur a offert
 *       l'outplacement.</li>
 *   <li>{@code dateNotificationOffre} — date à laquelle l'employeur a
 *       remis l'offre au travailleur (nullable si pas d'offre).</li>
 *   <li>{@code heuresProgramme} — nombre d'heures du programme proposé.</li>
 *   <li>{@code dureeProgrammeMois} — durée du programme en mois.</li>
 *   <li>{@code offreEcrite} — {@code true} si l'offre est écrite.</li>
 *   <li>{@code offreIndividuelle} — {@code true} si l'offre est
 *       individuelle.</li>
 *   <li>{@code contenuMinimumRespecte} — {@code true} si le contenu
 *       minimum (bilan, projet, coaching, formation, soutien
 *       psychologique) est présent.</li>
 * </ul>
 */
public record OutplacementBeGeneral30semRequest(

        @NotNull(message = "licenciementEffectif est requis")
        Boolean licenciementEffectif,

        @NotNull(message = "motifGrave est requis")
        Boolean motifGrave,

        @NotNull(message = "semainesPreavisOuIndemnite est requis")
        @PositiveOrZero(message = "semainesPreavisOuIndemnite doit être ≥ 0")
        Integer semainesPreavisOuIndemnite,

        @NotNull(message = "dateFinContrat est requise")
        LocalDate dateFinContrat,

        @NotNull(message = "offreNotifiee est requis")
        Boolean offreNotifiee,

        LocalDate dateNotificationOffre,

        @NotNull(message = "heuresProgramme est requis")
        @PositiveOrZero(message = "heuresProgramme doit être ≥ 0")
        Integer heuresProgramme,

        @NotNull(message = "dureeProgrammeMois est requise")
        @PositiveOrZero(message = "dureeProgrammeMois doit être ≥ 0")
        Integer dureeProgrammeMois,

        @NotNull(message = "offreEcrite est requis")
        Boolean offreEcrite,

        @NotNull(message = "offreIndividuelle est requis")
        Boolean offreIndividuelle,

        @NotNull(message = "contenuMinimumRespecte est requis")
        Boolean contenuMinimumRespecte
) {
}
