package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * SF-219-28 : payload de création / mise à jour de l'analyse
 * <i>Reconnaissance d'une maladie professionnelle Fedris BE</i>
 * (Lois coordonnées du 03/06/1970 + AR du 28/03/1969 modifié 06/12/2018
 * + AR du 16/12/1985 système ouvert + Loi du 11/01/2018 réformant
 * Fedris).
 *
 * <h2>Logique métier — éléments cumulatifs</h2>
 * <ol>
 *   <li><b>Type de maladie</b> ({@code typeMaladie}) — voir
 *       {@link MpFedrisReconnaissanceTypeMaladie}. Détermine la branche
 *       du verdict (liste fermée présomption art. 32 vs système ouvert
 *       preuve causalité art. 30bis).</li>
 *   <li><b>Code maladie de la liste</b> ({@code codeMaladieListe}) —
 *       code de la maladie selon la nomenclature AR 28/03/1969
 *       (sections 1.x à 6.x). Obligatoire si {@code typeMaladie =
 *       LISTE_FERMEE}. Optionnel sinon. Format libre — l'avocat saisit
 *       par ex. « 1.404.01 silicose » ou « 5.01.01 affection lombaire
 *       chronique ». Limité à 255 caractères.</li>
 *   <li><b>Libellé de la maladie</b> ({@code libelleMaladie}) — nom
 *       médical de la maladie déclarée. Obligatoire. Limité à 255
 *       caractères.</li>
 *   <li><b>Date de première constatation médicale</b>
 *       ({@code dateConstatationMedicale}) — date à laquelle la maladie
 *       a été médicalement constatée pour la première fois. Référence
 *       pour le calcul du délai de prescription art. 31 lois
 *       coordonnées 03/06/1970 (3 ans à compter de la connaissance du
 *       caractère professionnel).</li>
 *   <li><b>Date de connaissance du caractère professionnel</b>
 *       ({@code dateConnaissanceProfessionnel}) — date à laquelle la
 *       victime a eu connaissance du lien entre la maladie et son
 *       exposition professionnelle. Point de départ du délai de
 *       prescription. Si null, le calculateur prend
 *       {@code dateConstatationMedicale}.</li>
 *   <li><b>Date de déclaration envisagée</b>
 *       ({@code dateDeclarationEnvisagee}) — date à laquelle la
 *       déclaration sera effectivement transmise à Fedris (mode
 *       prospectif). Si null, le calculateur prend la date du jour
 *       Europe/Brussels.</li>
 *   <li><b>Exposition professionnelle au risque</b>
 *       ({@code exposition}) — voir
 *       {@link MpFedrisReconnaissanceExposition}. AVEREE active la
 *       présomption art. 32 en liste fermée ; INSUFFISANTE ou
 *       INDETERMINEE la bloquent.</li>
 *   <li><b>Causalité directe et déterminante</b>
 *       ({@code causalite}) — voir
 *       {@link MpFedrisReconnaissanceCausalite}. Décisive en système
 *       ouvert (Cass. BE 28/05/2008 S.07.0033.F). NON_APPLICABLE
 *       attendu en liste fermée.</li>
 * </ol>
 */
public record MpFedrisReconnaissanceRequest(

        @NotNull(message = "typeMaladie est requis")
        MpFedrisReconnaissanceTypeMaladie typeMaladie,

        @Size(max = 255, message = "codeMaladieListe max 255 caractères")
        String codeMaladieListe,

        @NotNull(message = "libelleMaladie est requis")
        @Size(min = 1, max = 255, message = "libelleMaladie : 1 à 255 caractères")
        String libelleMaladie,

        @NotNull(message = "dateConstatationMedicale est requis")
        LocalDate dateConstatationMedicale,

        LocalDate dateConnaissanceProfessionnel,

        LocalDate dateDeclarationEnvisagee,

        @NotNull(message = "exposition est requis")
        MpFedrisReconnaissanceExposition exposition,

        @NotNull(message = "causalite est requis")
        MpFedrisReconnaissanceCausalite causalite
) {
}
