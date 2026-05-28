package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-23 : payload de création / mise à jour de l'analyse <i>refus
 * d'aménagements raisonnables handicap BE</i> (Loi du 10/05/2007
 * tendant à lutter contre certaines formes de discrimination M.B.
 * 30/05/2007 + CCT n° 95 + Directive 2000/78/CE art. 5 + Convention
 * ONU 13/12/2006 art. 27).
 *
 * <h2>Logique métier — conditions cumulatives</h2>
 * <ol>
 *   <li><b>Statut handicap</b> ({@code statutHandicap}) — aiguille
 *       la qualification préalable à toute analyse discrimination
 *       (Loi 10/05/2007 art. 4 4° + jurisprudence CJUE Ring/Werge).</li>
 *   <li><b>Date demande aménagement</b> ({@code dateDemandeAmenagement})
 *       — formalisée par écrit par le travailleur ou suggérée par
 *       le médecin du travail SEPP.</li>
 *   <li><b>Type d'aménagement demandé</b> ({@code typeAmenagementDemande})
 *       — texte libre court (poste / horaires / télétravail /
 *       équipement / réaffectation / formation / autre).</li>
 *   <li><b>Coût estimé aménagement</b> ({@code coutEstimeAmenagement})
 *       — en euros, indicatif — apprécié <b>après</b> subsides
 *       mobilisables.</li>
 *   <li><b>Subsides demandés</b> ({@code subsidesDemandes}) — AVIQ
 *       / VDAB / PHARE / Actiris / Bruxelles Formation — l'absence
 *       de demande de subside affaiblit substantiellement la défense
 *       employeur charge disproportionnée.</li>
 *   <li><b>Effectif entreprise</b> ({@code effectifEntreprise}) —
 *       indicatif de proportionnalité art. 14 al. 2 Loi 10/05/2007.</li>
 *   <li><b>Chiffre d'affaires annuel</b>
 *       ({@code chiffreAffairesAnnuel}) — en euros, élément de
 *       proportionnalité.</li>
 *   <li><b>Réponse employeur</b> ({@code reponseEmployeur}) —
 *       déterminant pour le verdict.</li>
 *   <li><b>Motivation détaillée fournie</b>
 *       ({@code motivationDetailleeFournie}) — pour qualifier la
 *       présomption art. 28 § 3 (charge de la preuve).</li>
 *   <li><b>Avis SEPP favorable</b> ({@code avisSeppFavorable}) —
 *       avis du médecin du travail / service externe de prévention
 *       favorable à l'aménagement (preuve majeure).</li>
 *   <li><b>Charge disproportionnée invoquée</b>
 *       ({@code chargeDisproportionneeInvoquee}) — l'employeur
 *       invoque-t-il ce moyen.</li>
 *   <li><b>Devis externe à l'appui</b>
 *       ({@code devisExterneFourni}) — l'employeur produit-il un
 *       devis professionnel à l'appui du coût allégué.</li>
 *   <li><b>Mesures alternatives proposées</b>
 *       ({@code mesuresAlternativesProposees}) — l'employeur
 *       propose-t-il des alternatives raisonnables.</li>
 *   <li><b>Sanction subie</b> ({@code sanctionSubie}) — déclenche
 *       la présomption de représailles art. 17 si autre que
 *       AUCUNE.</li>
 *   <li><b>Date sanction</b> ({@code dateSanction}) — chronologie
 *       par rapport à la demande pour caractériser le lien.</li>
 *   <li><b>Salaire mensuel brut</b> ({@code salaireMensuelBrut}) —
 *       en euros, sert au calcul indicatif de l'indemnité
 *       forfaitaire 6 mois (art. 28 § 2 1°).</li>
 *   <li><b>Procédure UNIA saisie</b> ({@code procedureUniaSaisie})
 *       — Centre interfédéral pour l'égalité des chances UNIA
 *       saisi ou non (indicateur de la stratégie contentieuse).</li>
 * </ol>
 */
public record DiscriminationBeHandicapAmenagementRequest(

        @NotNull(message = "statutHandicap est requis")
        DiscriminationBeHandicapAmenagementStatutHandicap statutHandicap,

        LocalDate dateDemandeAmenagement,

        @Size(max = 500, message = "typeAmenagementDemande limité à 500 caractères")
        String typeAmenagementDemande,

        @DecimalMin(value = "0.0", message = "coutEstimeAmenagement doit être positif ou nul")
        @Digits(integer = 10, fraction = 2, message = "coutEstimeAmenagement au format XXXXXXXXXX.XX")
        BigDecimal coutEstimeAmenagement,

        @NotNull(message = "subsidesDemandes est requis")
        Boolean subsidesDemandes,

        @NotNull(message = "effectifEntreprise est requis")
        @Min(value = 0, message = "effectifEntreprise doit être positif ou nul")
        @Max(value = 1000000, message = "effectifEntreprise plafonné à 1 000 000")
        Integer effectifEntreprise,

        @DecimalMin(value = "0.0", message = "chiffreAffairesAnnuel doit être positif ou nul")
        @Digits(integer = 13, fraction = 2, message = "chiffreAffairesAnnuel au format")
        BigDecimal chiffreAffairesAnnuel,

        @NotNull(message = "reponseEmployeur est requis")
        DiscriminationBeHandicapAmenagementReponseEmployeur reponseEmployeur,

        @NotNull(message = "motivationDetailleeFournie est requis")
        Boolean motivationDetailleeFournie,

        @NotNull(message = "avisSeppFavorable est requis")
        Boolean avisSeppFavorable,

        @NotNull(message = "chargeDisproportionneeInvoquee est requis")
        Boolean chargeDisproportionneeInvoquee,

        @NotNull(message = "devisExterneFourni est requis")
        Boolean devisExterneFourni,

        @NotNull(message = "mesuresAlternativesProposees est requis")
        Boolean mesuresAlternativesProposees,

        @NotNull(message = "sanctionSubie est requis")
        DiscriminationBeHandicapAmenagementSanctionSubie sanctionSubie,

        LocalDate dateSanction,

        @DecimalMin(value = "0.0", message = "salaireMensuelBrut doit être positif ou nul")
        @Digits(integer = 10, fraction = 2, message = "salaireMensuelBrut au format XXXXXXXXXX.XX")
        BigDecimal salaireMensuelBrut,

        @NotNull(message = "procedureUniaSaisie est requis")
        Boolean procedureUniaSaisie
) {
}
