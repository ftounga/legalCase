package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-219-19 : payload de création / mise à jour de l'analyse <i>droit
 * à la déconnexion BE</i> (Loi du 03/10/2022 « Deal pour l'emploi »,
 * art. 16 + AR du 19/02/2023 + CCT n° 149).
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Effectif entreprise</b> ({@code effectifEntreprise}) —
 *       art. 16 § 1 : seuil de 20 travailleurs pour déclencher
 *       l'obligation.</li>
 *   <li><b>Statut accord</b> ({@code statutAccord}) — aiguille le
 *       verdict (CCT déposée / règlement de travail / négociation /
 *       aucune initiative / indéterminé).</li>
 *   <li><b>Date entrée en vigueur instrument</b>
 *       ({@code dateEntreeVigueurInstrument}) — date de dépôt CCT ou
 *       de publication du règlement de travail modifié (≥ 01/04/2023
 *       pour les entreprises atteignant le seuil à cette date).</li>
 *   <li><b>Modalités pratiques de déconnexion définies</b>
 *       ({@code modalitesPratiquesDeconnexionDefinies}) — art. 16 § 2
 *       1° : règles pour les e-mails / appels / messages hors heures,
 *       jours fériés, congés.</li>
 *   <li><b>Sensibilisation / formation prévue</b>
 *       ({@code sensibilisationFormationPrevue}) — art. 16 § 2 2° :
 *       actions de sensibilisation à l'usage raisonné des outils
 *       numériques.</li>
 *   <li><b>Modalités d'organisation du travail définies</b>
 *       ({@code modalitesOrganisationTravailDefinies}) — art. 16 § 2
 *       3° : règles d'organisation visant à garantir le respect du
 *       temps de repos.</li>
 *   <li><b>Consultation organe concertation effectuée</b>
 *       ({@code consultationOrganeConcertationEffectuee}) — CE /
 *       CPPT / DS consultés avant adoption (art. 16 § 1 +
 *       Loi 04/12/2007 sur les élections sociales, art. 65).</li>
 *   <li><b>Manquement signalé au CBE / SPF Emploi</b>
 *       ({@code manquementSignaleCbeOuSpf}) — indicateur d'un
 *       contentieux en cours (action en cessation, contrôle SPF).</li>
 * </ol>
 */
public record DroitDeconnexionBeRequest(

        @NotNull(message = "effectifEntreprise est requis")
        @Min(value = 0, message = "effectifEntreprise doit être positif ou nul")
        @Max(value = 1000000, message = "effectifEntreprise plafonné à 1 000 000")
        Integer effectifEntreprise,

        @NotNull(message = "statutAccord est requis")
        DroitDeconnexionBeStatutAccord statutAccord,

        LocalDate dateEntreeVigueurInstrument,

        @NotNull(message = "modalitesPratiquesDeconnexionDefinies est requis")
        Boolean modalitesPratiquesDeconnexionDefinies,

        @NotNull(message = "sensibilisationFormationPrevue est requis")
        Boolean sensibilisationFormationPrevue,

        @NotNull(message = "modalitesOrganisationTravailDefinies est requis")
        Boolean modalitesOrganisationTravailDefinies,

        @NotNull(message = "consultationOrganeConcertationEffectuee est requis")
        Boolean consultationOrganeConcertationEffectuee,

        @NotNull(message = "manquementSignaleCbeOuSpf est requis")
        Boolean manquementSignaleCbeOuSpf
) {
}
