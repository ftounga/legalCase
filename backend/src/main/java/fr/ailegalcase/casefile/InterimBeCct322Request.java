package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-14 : payload de création / mise à jour de l'analyse <i>statut
 * intérim BE — CCT n° 322</i> (Loi du 24/07/1987 + CCT n° 322 du
 * 14/06/2010 du CNT).
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Motif autorisé</b> ({@code motifMission}) — l'un des
 *       motifs limitativement énumérés à l'art. 1er § 1 / art. 1bis
 *       Loi 24/07/1987. {@code AUTRE_NON_AUTORISE} → rejet immédiat.</li>
 *   <li><b>Remplacement grève / lock-out</b>
 *       ({@code remplacementGreveOuLockout}) : interdit en toute
 *       hypothèse (art. 19) — présomption irréfragable de fraude.</li>
 *   <li><b>Durée mission</b> ({@code dureeTotaleMissionJours} +
 *       {@code dureeMaxLegaleJours}) : durée totale cumulée (contrats
 *       successifs inclus) vs plafond légal applicable au motif. Le
 *       plafond est passé en input pour rester paramétrable selon
 *       l'AR / la CCT applicable.</li>
 *   <li><b>Parité salariale</b> ({@code salaireHoraireIntimaireBrut}
 *       et {@code salaireHoraireReferenceBrut}) : l'intérimaire
 *       perçoit au minimum le salaire d'un permanent de même
 *       qualification chez l'utilisateur (art. 10).</li>
 *   <li><b>Formalisme</b> ({@code contratEcritSigne} +
 *       {@code dimonaDeclareeParEti}) : contrat de travail écrit
 *       (art. 8 § 1) + déclaration Dimona faite par l'ETI à peine
 *       de requalification.</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateDebutMission} — date de début de la première
 *       prestation intérimaire (ancrage temporel de la mission).</li>
 *   <li>{@code motifMission} — motif invoqué pour le recours
 *       à l'intérim.</li>
 *   <li>{@code remplacementGreveOuLockout} — drapeau interdiction
 *       art. 19 (grève ou lock-out).</li>
 *   <li>{@code dureeTotaleMissionJours} — durée totale cumulée
 *       en jours (contrats successifs inclus) ≥ 0.</li>
 *   <li>{@code dureeMaxLegaleJours} — plafond légal applicable au
 *       motif en jours, &gt; 0.</li>
 *   <li>{@code salaireHoraireIntimaireBrut} — salaire brut horaire
 *       de l'intérimaire (€) ≥ 0.</li>
 *   <li>{@code salaireHoraireReferenceBrut} — salaire brut horaire
 *       du permanent de référence chez l'utilisateur (€) &gt; 0.</li>
 *   <li>{@code contratEcritSigne} — contrat de travail écrit signé
 *       entre ETI et intérimaire.</li>
 *   <li>{@code dimonaDeclareeParEti} — déclaration Dimona effectuée
 *       par l'ETI (employeur formel).</li>
 * </ul>
 */
public record InterimBeCct322Request(

        @NotNull(message = "dateDebutMission est requise")
        LocalDate dateDebutMission,

        @NotNull(message = "motifMission est requis")
        InterimBeCct322MotifMission motifMission,

        @NotNull(message = "remplacementGreveOuLockout est requis")
        Boolean remplacementGreveOuLockout,

        @NotNull(message = "dureeTotaleMissionJours est requise")
        @Min(value = 0, message = "dureeTotaleMissionJours doit être positive ou nulle")
        Integer dureeTotaleMissionJours,

        @NotNull(message = "dureeMaxLegaleJours est requise")
        @Min(value = 1, message = "dureeMaxLegaleJours doit être strictement positive")
        Integer dureeMaxLegaleJours,

        @NotNull(message = "salaireHoraireIntimaireBrut est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "salaireHoraireIntimaireBrut doit être positif ou nul")
        BigDecimal salaireHoraireIntimaireBrut,

        @NotNull(message = "salaireHoraireReferenceBrut est requis")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "salaireHoraireReferenceBrut doit être strictement positif")
        BigDecimal salaireHoraireReferenceBrut,

        @NotNull(message = "contratEcritSigne est requis")
        Boolean contratEcritSigne,

        @NotNull(message = "dimonaDeclareeParEti est requis")
        Boolean dimonaDeclareeParEti
) {
}
