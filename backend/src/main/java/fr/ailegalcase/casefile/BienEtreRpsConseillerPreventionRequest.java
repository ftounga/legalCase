package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-219-30 : payload de création / mise à jour de l'analyse <i>Saisine
 * du Conseiller en Prévention Aspects Psychosociaux (CPAP)</i>
 * (Loi du 04/08/1996 art. 32sexies + AR du 10/04/2014).
 *
 * <h2>Champs requis</h2>
 * <ul>
 *   <li>{@link #typeRisque} — qualification provisoire du risque
 *       psychosocial (Loi 04/08/1996 art. 32/1 § 2).</li>
 *   <li>{@link #modeDemande} — informelle / formelle individuelle /
 *       formelle collective (AR 10/04/2014 art. 14 à 16).</li>
 *   <li>{@link #etapeProcedure} — étape de l'avancement de la
 *       procédure (intention → mesures employeur).</li>
 *   <li>{@link #conseillerIdentifie} — un CPAP interne ou externe a-t-il
 *       été identifié et désigné par l'employeur (art. 32sexies § 2) ?</li>
 *   <li>{@link #entretienPrealableRealise} — l'entretien préalable
 *       obligatoire avant demande formelle a-t-il eu lieu (art. 16 § 1
 *       AR 10/04/2014, délai 10 jours) ?</li>
 *   <li>{@link #demandeEcriteSignee} — la demande formelle est-elle
 *       écrite et signée par le travailleur (art. 16 § 2 AR 10/04/2014) ?</li>
 *   <li>{@link #accuseReceptionRecu} — le CPAP a-t-il remis l'accusé
 *       de réception au travailleur (art. 16 § 4 AR 10/04/2014) ?</li>
 *   <li>{@link #notificationEmployeurFaite} — la notification de la
 *       demande à l'employeur a-t-elle été faite par le CPAP après
 *       acceptation de la demande (art. 17 § 1 AR 10/04/2014) ?</li>
 * </ul>
 *
 * <h2>Champs optionnels</h2>
 * <ul>
 *   <li>{@link #dateDepotDemande} — date de dépôt de la demande
 *       (informelle ou formelle) ; utilisée pour calculer le délai
 *       3 mois de l'enquête (art. 22 § 1 AR 10/04/2014) et le délai
 *       2 mois mesures employeur (art. 32 AR 10/04/2014).</li>
 *   <li>{@link #dateAvisRendu} — date de l'avis motivé rendu par le
 *       CPAP (renseignée si {@link #etapeProcedure} =
 *       {@link BienEtreRpsConseillerPreventionEtape#AVIS_RENDU} ou
 *       postérieure).</li>
 *   <li>{@link #joursDepuisDepot} — délai en jours depuis le dépôt
 *       de la demande, calculé en amont par le frontend.</li>
 * </ul>
 */
public record BienEtreRpsConseillerPreventionRequest(

        @NotNull(message = "typeRisque est requis")
        BienEtreRpsConseillerPreventionTypeRisque typeRisque,

        @NotNull(message = "modeDemande est requis")
        BienEtreRpsConseillerPreventionModeDemande modeDemande,

        @NotNull(message = "etapeProcedure est requis")
        BienEtreRpsConseillerPreventionEtape etapeProcedure,

        @NotNull(message = "conseillerIdentifie est requis")
        Boolean conseillerIdentifie,

        @NotNull(message = "entretienPrealableRealise est requis")
        Boolean entretienPrealableRealise,

        @NotNull(message = "demandeEcriteSignee est requis")
        Boolean demandeEcriteSignee,

        @NotNull(message = "accuseReceptionRecu est requis")
        Boolean accuseReceptionRecu,

        @NotNull(message = "notificationEmployeurFaite est requis")
        Boolean notificationEmployeurFaite,

        LocalDate dateDepotDemande,

        LocalDate dateAvisRendu,

        @Min(value = 0,
                message = "joursDepuisDepot ne peut pas être négatif")
        Integer joursDepuisDepot
) {
}
