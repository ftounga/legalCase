package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-18 : payload de création / mise à jour de l'analyse
 * <i>semaine de 4 jours BE</i> (Loi du 03/10/2022 « Deal pour
 * l'emploi », art. 5).
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Statut demande</b> ({@code statutDemande}) — aiguille le
 *       verdict (accord / refus motivé / refus non motivé / en
 *       attente / licenciement / indéterminé).</li>
 *   <li><b>Travailleur à temps plein</b> ({@code travailleurTempsPlein})
 *       — art. 5 § 1 (régime fermé aux temps partiels).</li>
 *   <li><b>Demande écrite du travailleur</b> ({@code demandeEcriteTravailleur})
 *       — art. 5 § 2 (lettre recommandée / accusé de réception).</li>
 *   <li><b>Date demande</b> ({@code dateDemande}) — point de départ
 *       du délai d'1 mois pour la réponse employeur.</li>
 *   <li><b>Durée hebdomadaire (h)</b> ({@code dureeHebdomadaireHeures})
 *       — 38 à 40 h selon la CCT sectorielle.</li>
 *   <li><b>Journée maximale (h)</b> ({@code journeeMaximaleHeures}) —
 *       art. 5 § 3 : doit être ≤ 9 h 30 (sauf CCT autorisant 10 h).</li>
 *   <li><b>CCT autorise 10 h</b> ({@code cctAutorise10h}) — extension
 *       possible jusqu'à 10 h par CCT sectorielle.</li>
 *   <li><b>Avenant écrit au contrat</b> ({@code avenantEcritSigne})
 *       — art. 5 § 4 (formalisation contractuelle obligatoire).</li>
 *   <li><b>Règlement de travail modifié</b> ({@code reglementTravailModifie})
 *       — art. 6 (inscription de l'horaire compressé).</li>
 *   <li><b>Durée avenant (mois)</b> ({@code dureeAvenantMois}) —
 *       art. 5 § 4 : 6 mois max renouvelable.</li>
 *   <li><b>Avenant renouvelé</b> ({@code avenantRenouvele}) —
 *       autorise une durée &gt; 6 mois.</li>
 *   <li><b>Refus motivé par écrit</b> ({@code refusMotiveParEcrit})
 *       — applicable si {@code statutDemande} ∈
 *       {REFUSE_*} : indique si la motivation a été formalisée.</li>
 *   <li><b>Date licenciement</b> ({@code dateLicenciement}) —
 *       applicable si {@code statutDemande} =
 *       {@code LICENCIE_APRES_DEMANDE} : déclenche la présomption de
 *       représailles si postérieure à {@code dateDemande}.</li>
 *   <li><b>Motif licenciement objectif</b>
 *       ({@code motifLicenciementObjectifEtabli}) — applicable si
 *       licenciement : l'employeur a-t-il démontré un motif objectif
 *       sans rapport avec la demande ?</li>
 * </ol>
 */
public record Semaine4JoursBeRequest(

        @NotNull(message = "statutDemande est requis")
        Semaine4JoursBeStatutDemande statutDemande,

        @NotNull(message = "travailleurTempsPlein est requis")
        Boolean travailleurTempsPlein,

        @NotNull(message = "demandeEcriteTravailleur est requise")
        Boolean demandeEcriteTravailleur,

        @NotNull(message = "dateDemande est requise")
        LocalDate dateDemande,

        @NotNull(message = "dureeHebdomadaireHeures est requise")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "dureeHebdomadaireHeures doit être strictement positive")
        @DecimalMax(value = "60.0", inclusive = true,
                message = "dureeHebdomadaireHeures plafonnée à 60h")
        BigDecimal dureeHebdomadaireHeures,

        @NotNull(message = "journeeMaximaleHeures est requise")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "journeeMaximaleHeures doit être strictement positive")
        @DecimalMax(value = "16.0", inclusive = true,
                message = "journeeMaximaleHeures plafonnée à 16h")
        BigDecimal journeeMaximaleHeures,

        @NotNull(message = "cctAutorise10h est requis")
        Boolean cctAutorise10h,

        @NotNull(message = "avenantEcritSigne est requis")
        Boolean avenantEcritSigne,

        @NotNull(message = "reglementTravailModifie est requis")
        Boolean reglementTravailModifie,

        @NotNull(message = "dureeAvenantMois est requise")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "dureeAvenantMois doit être positive ou nulle")
        @DecimalMax(value = "120.0", inclusive = true,
                message = "dureeAvenantMois plafonnée à 120 mois")
        BigDecimal dureeAvenantMois,

        @NotNull(message = "avenantRenouvele est requis")
        Boolean avenantRenouvele,

        @NotNull(message = "refusMotiveParEcrit est requis")
        Boolean refusMotiveParEcrit,

        LocalDate dateLicenciement,

        @NotNull(message = "motifLicenciementObjectifEtabli est requis")
        Boolean motifLicenciementObjectifEtabli
) {
}
