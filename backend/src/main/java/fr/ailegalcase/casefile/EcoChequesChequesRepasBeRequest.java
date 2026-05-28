package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-21 : payload de création / mise à jour de l'analyse
 * <i>éco-chèques + chèques-repas BE</i> (CCT n°98 du CNT du 20/02/2009
 * pour les éco-chèques ; Loi du 25/04/2014 + AR du 03/02/2010 art.
 * 19bis AR 28/11/1969 pour les chèques-repas).
 *
 * <h2>Logique métier — champs aiguilleurs</h2>
 * <ol>
 *   <li><b>Type d'avantage</b> ({@code typeAvantage}) — ECO_CHEQUES /
 *       CHEQUES_REPAS / INDETERMINE — pilote l'application du plafond
 *       et des conditions.</li>
 *   <li><b>Montant annuel total</b> ({@code montantAnnuelEur}) —
 *       cumul attribué sur l'année civile. Comparé au plafond 250
 *       EUR/an (éco-chèques) ou au prorata jours × valeur faciale
 *       (chèques-repas).</li>
 *   <li><b>Valeur faciale unitaire</b> ({@code valeurFacialeUnitaireEur})
 *       — montant d'un chèque (pour chèques-repas : max 8 EUR ; pour
 *       éco-chèques : pas de plafond unitaire mais cumul ≤ 250 EUR/an).</li>
 *   <li><b>Contribution travailleur unitaire</b>
 *       ({@code contributionTravailleurUnitaireEur}) — pour
 *       chèques-repas : minimum 1,09 EUR (sinon non conforme).</li>
 *   <li><b>Jours effectivement prestés</b> ({@code joursEffectivementPrestes})
 *       — pour chèques-repas : le calcul doit être au prorata du
 *       nombre de jours réellement prestés (art. 19bis §2 AR
 *       28/11/1969).</li>
 *   <li><b>CCT sectorielle ou d'entreprise</b>
 *       ({@code cctSectorielleOuEntrepriseExiste}) — pour éco-chèques :
 *       requis (art. 3 CCT n°98) ; à défaut, convention individuelle
 *       écrite seule possible.</li>
 *   <li><b>Convention individuelle écrite</b>
 *       ({@code conventionIndividuelleEcrite}) — pour éco-chèques :
 *       requis si pas de CCT (art. 3 CCT n°98).</li>
 *   <li><b>Paiement électronique</b> ({@code paiementElectronique})
 *       — pour chèques-repas : obligatoire depuis 01/01/2016.</li>
 *   <li><b>Cumul frais de bouche</b> ({@code cumulFraisBouche}) —
 *       pour chèques-repas : interdit (art. 19bis §2 al. 4 AR
 *       28/11/1969).</li>
 *   <li><b>Substitution à rémunération</b>
 *       ({@code substitutionRemuneration}) — interdit (art. 4 CCT
 *       n°98 ; jurisprudence constante Cour de cassation).</li>
 *   <li><b>Date d'attribution</b> ({@code dateAttribution}) — sert
 *       au calcul de la validité (2 ans pour éco-chèques, AR
 *       12/04/2009 art. 13).</li>
 * </ol>
 */
public record EcoChequesChequesRepasBeRequest(

        @NotNull(message = "typeAvantage est requis")
        EcoChequesChequesRepasBeTypeAvantage typeAvantage,

        @NotNull(message = "montantAnnuelEur est requis")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "montantAnnuelEur doit être positif ou nul")
        @DecimalMax(value = "100000.0", inclusive = true,
                message = "montantAnnuelEur plafonné à 100 000 EUR")
        BigDecimal montantAnnuelEur,

        @NotNull(message = "valeurFacialeUnitaireEur est requise")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "valeurFacialeUnitaireEur doit être positive ou nulle")
        @DecimalMax(value = "1000.0", inclusive = true,
                message = "valeurFacialeUnitaireEur plafonnée à 1 000 EUR")
        BigDecimal valeurFacialeUnitaireEur,

        @NotNull(message = "contributionTravailleurUnitaireEur est requise")
        @DecimalMin(value = "0.0", inclusive = true,
                message = "contributionTravailleurUnitaireEur doit être positive ou nulle")
        @DecimalMax(value = "1000.0", inclusive = true,
                message = "contributionTravailleurUnitaireEur plafonnée à 1 000 EUR")
        BigDecimal contributionTravailleurUnitaireEur,

        @NotNull(message = "joursEffectivementPrestes est requis")
        @Min(value = 0, message = "joursEffectivementPrestes doit être positif ou nul")
        Integer joursEffectivementPrestes,

        @NotNull(message = "cctSectorielleOuEntrepriseExiste est requis")
        Boolean cctSectorielleOuEntrepriseExiste,

        @NotNull(message = "conventionIndividuelleEcrite est requis")
        Boolean conventionIndividuelleEcrite,

        @NotNull(message = "paiementElectronique est requis")
        Boolean paiementElectronique,

        @NotNull(message = "cumulFraisBouche est requis")
        Boolean cumulFraisBouche,

        @NotNull(message = "substitutionRemuneration est requis")
        Boolean substitutionRemuneration,

        LocalDate dateAttribution
) {
}
