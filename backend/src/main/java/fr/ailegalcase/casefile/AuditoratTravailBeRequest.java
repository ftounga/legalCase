package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-219-25 : payload de création / mise à jour de l'analyse <i>Auditorat
 * du travail BE — orientation et checklist de saisine</i> (Code judiciaire
 * art. 138bis + Code d'instruction criminelle art. 24 + Loi du 03/08/1992
 * sur le Code judiciaire + Loi du 06/06/2010 introduisant le Code pénal
 * social).
 *
 * <h2>Logique métier — éléments cumulatifs de l'orientation</h2>
 * <ol>
 *   <li><b>Nature des faits</b> ({@code natureFait}) — voir
 *       {@link AuditoratTravailBeNatureFait}. Détermine si la
 *       compétence de l'auditorat est engagée (infraction au droit
 *       social pénal, accident grave, harcèlement pénal, etc.) ou
 *       si les faits relèvent du seul tribunal du travail
 *       (art. 578 C. jud.).</li>
 *   <li><b>Mode de saisine envisagé</b> ({@code modeSaisineEnvisage})
 *       — plainte directe, dénonciation simple, signalement
 *       inspection ou voie civile parallèle. Conditionne la
 *       checklist procédurale restituée.</li>
 *   <li><b>Date des faits</b> ({@code dateFaits}) — la date de
 *       commission de l'infraction conditionne la prescription
 *       (art. 81 C. pén. soc. : 5 ans à compter du jour de
 *       l'infraction pour les niveaux 2-4 ; 3 ans pour le niveau 1 ;
 *       interruption art. 22-23 Titre prél. CIC).</li>
 *   <li><b>Faits prescrits</b> ({@code faitsPrescrits}) — drapeau
 *       explicite de l'avocat lorsque la prescription pénale est
 *       acquise ; l'outil bloque alors la saisine recommandée.</li>
 *   <li><b>Inspection déjà saisie</b> ({@code inspectionDejaSaisie})
 *       — l'inspection a déjà été saisie / un PV existe ; conditionne
 *       le basculement vers la voie auditorat directe (art. 76
 *       C. pén. soc.).</li>
 *   <li><b>Plainte civile déposée</b> ({@code plainteCivileDeposee})
 *       — une procédure civile parallèle est en cours au tribunal du
 *       travail (paiement arriérés, indemnité de rupture). N'affecte
 *       pas la voie pénale (autonomie de la voie répressive
 *       art. 4 Titre prél. CIC).</li>
 *   <li><b>Urgence sécurité personnes</b> ({@code urgenceSecuritePersonnes})
 *       — situation de danger immédiat (accident grave imminent,
 *       harcèlement persistant avec atteinte à l'intégrité) :
 *       l'auditorat dispose d'un pouvoir de mesures conservatoires
 *       et de réquisition d'inspection en urgence.</li>
 *   <li><b>Recours pénal envisagé</b> ({@code recoursPenalEnvisage})
 *       — l'avocat envisage la voie pénale comme axe principal ;
 *       l'outil ajuste la checklist (constitution partie civile,
 *       articulation avec demande civile, intérêt à agir).</li>
 *   <li><b>Employeur personne morale</b> ({@code employeurPersonneMorale})
 *       — qualité de l'employeur (multiplication × 5 art. 105
 *       C. pén. soc. en cas de poursuite ; régime de responsabilité
 *       personne morale art. 5 C. pén.).</li>
 * </ol>
 */
public record AuditoratTravailBeRequest(

        @NotNull(message = "natureFait est requis")
        AuditoratTravailBeNatureFait natureFait,

        @NotNull(message = "modeSaisineEnvisage est requis")
        AuditoratTravailBeModeSaisine modeSaisineEnvisage,

        LocalDate dateFaits,

        @NotNull(message = "faitsPrescrits est requis")
        Boolean faitsPrescrits,

        @NotNull(message = "inspectionDejaSaisie est requis")
        Boolean inspectionDejaSaisie,

        @NotNull(message = "plainteCivileDeposee est requis")
        Boolean plainteCivileDeposee,

        @NotNull(message = "urgenceSecuritePersonnes est requis")
        Boolean urgenceSecuritePersonnes,

        @NotNull(message = "recoursPenalEnvisage est requis")
        Boolean recoursPenalEnvisage,

        @NotNull(message = "employeurPersonneMorale est requis")
        Boolean employeurPersonneMorale
) {
}
