package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-219-08 : payload de création / mise à jour de l'analyse
 * <i>transfert d'entreprise CCT n° 32bis</i> (transfert conventionnel
 * BE — Loi 17/03/1965, Directive 2001/23/CE).
 *
 * <h2>Logique métier</h2>
 * <ol>
 *   <li><b>Qualification de l'opération</b> — vente, fusion, scission,
 *       apport, démembrement (qualifient) vs faillite, liquidation,
 *       cession d'actions, intra-groupe (n'qualifient pas).</li>
 *   <li><b>Préservation de l'identité économique</b> — l'entité
 *       transférée doit garder son identité (continuité d'activité,
 *       reprise des moyens essentiels). Si non préservée → pas de
 *       CCT 32bis (jp Süzen, Abler).</li>
 *   <li><b>Procédure d'information-consultation préalable</b> —
 *       obligatoire (CE, DS, CPPT selon présence dans l'entreprise) avant
 *       toute décision irréversible. Non-respect = sanction pénale Loi
 *       17/03/1965 + DI individuels, mais maintien des contrats inchangé
 *       (ordre public).</li>
 *   <li><b>Effets sur les contrats</b> — maintien automatique de plein
 *       droit (ancienneté, rémunération, avantages individuels).
 *       Conventions collectives maintenues jusqu'à l'échéance ou un
 *       nouvelle CCT (art. 23 Directive ; CCT 32bis art. 7).</li>
 *   <li><b>Responsabilité solidaire</b> — cédant et cessionnaire
 *       solidairement responsables des dettes salariales antérieures
 *       durant 1 an post-transfert (art. 3 Directive 2001/23/CE).</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateTransfert} — date effective du transfert.</li>
 *   <li>{@code typeOperation} — nature juridique de l'opération.</li>
 *   <li>{@code identiteEconomique} — appréciation de la préservation de
 *       l'entité.</li>
 *   <li>{@code infoConsultPrealableRespectee} — procédure
 *       d'information-consultation préalable respectée.</li>
 *   <li>{@code conseilEntrepriseConsulte} — CE consulté préalablement
 *       (si applicable, sinon délégation syndicale ou CPPT).</li>
 *   <li>{@code maintienContratsAutomatique} — déclaration de maintien
 *       automatique des contrats individuels par le cessionnaire (devrait
 *       être true par défaut, sinon violation art. 7).</li>
 *   <li>{@code maintienAncienneteRespecte} — ancienneté reprise
 *       intégralement par le cessionnaire.</li>
 *   <li>{@code maintienRemunerationRespecte} — rémunération maintenue
 *       au moins au niveau pré-transfert.</li>
 *   <li>{@code conventionsCollectivesMaintenues} — CCT applicables au
 *       cédant maintenues jusqu'à échéance / nouvelle CCT.</li>
 * </ul>
 */
public record TransfertEntrepriseCct32bisRequest(

        @NotNull(message = "dateTransfert est requise")
        LocalDate dateTransfert,

        @NotNull(message = "typeOperation est requise")
        TransfertEntrepriseCct32bisTypeOperation typeOperation,

        @NotNull(message = "identiteEconomique est requise")
        TransfertEntrepriseCct32bisIdentiteEconomique identiteEconomique,

        @NotNull(message = "infoConsultPrealableRespectee est requise")
        Boolean infoConsultPrealableRespectee,

        @NotNull(message = "conseilEntrepriseConsulte est requise")
        Boolean conseilEntrepriseConsulte,

        @NotNull(message = "maintienContratsAutomatique est requise")
        Boolean maintienContratsAutomatique,

        @NotNull(message = "maintienAncienneteRespecte est requise")
        Boolean maintienAncienneteRespecte,

        @NotNull(message = "maintienRemunerationRespecte est requise")
        Boolean maintienRemunerationRespecte,

        @NotNull(message = "conventionsCollectivesMaintenues est requise")
        Boolean conventionsCollectivesMaintenues
) {
}
