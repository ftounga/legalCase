package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-219-24 : payload de création / mise à jour de l'analyse <i>Code
 * pénal social BE — qualification d'infraction sociale + niveau de
 * sanction</i> (Loi du 06/06/2010 introduisant le Code pénal social,
 * M.B. 01/07/2010, e.e.v. 01/07/2011).
 *
 * <h2>Logique métier — éléments cumulatifs de la qualification</h2>
 * <ol>
 *   <li><b>Type d'infraction</b> ({@code typeInfraction}) — voir
 *       {@link CodePenalSocialBeTypeInfraction} : travail non
 *       déclaré, absence Dimona / DmfA, non-paiement de la
 *       rémunération, dépassement temps de travail, manquement
 *       bien-être, accident grave non déclaré, discrimination,
 *       entrave inspection, faux social, etc. Le type détermine
 *       le niveau par défaut (art. 119 à 235 C. pén. soc. — Livre
 *       2 Titre 2 à 12).</li>
 *   <li><b>Niveau renseigné</b> ({@code niveauPropose}) — niveau
 *       1 à 4 explicitement saisi par l'avocat lorsque le type
 *       {@link CodePenalSocialBeTypeInfraction#AUTRE_QUALIFICATION}
 *       est utilisé ou pour confirmer / forcer un niveau différent
 *       du défaut (ex. ABSENCE_DMFA niveau 2 si non intentionnel
 *       au lieu du niveau 4 par défaut). Si renseigné, prime sur
 *       la table par défaut.</li>
 *   <li><b>Date des faits</b> ({@code dateFaits}) — la date de
 *       commission conditionne les coefficients d'indexation des
 *       amendes pénales (art. 1<sup>er</sup> L. 05/03/1952 — +0,80
 *       décimes additionnels jusqu'au 31/12/2024 ; +0,90 décimes
 *       additionnels depuis le 01/01/2025 par AR 27/12/2024).
 *       Information indicative ; l'outil ne calcule pas le montant
 *       indexé.</li>
 *   <li><b>Nombre de travailleurs concernés</b>
 *       ({@code nombreTravailleursConcernes}) — multiplicateur de
 *       l'amende administrative ou pénale (art. 103 § 2 C. pén.
 *       soc.) lorsque l'infraction est commise « à l'égard d'un
 *       travailleur ». Plafonné à 100 × maximum unitaire pour une
 *       personne morale (art. 105 § 1<sup>er</sup>).</li>
 *   <li><b>Personne morale</b> ({@code personneMorale}) — si
 *       l'employeur est une personne morale, multiplication × 5
 *       des amendes (art. 105 § 1<sup>er</sup> C. pén. soc.).</li>
 *   <li><b>Récidive ≤ 1 an</b> ({@code recidiveDansLAn}) — récidive
 *       légale art. 110 C. pén. soc. : doublement du plafond
 *       d'amende si nouvelle infraction de même niveau dans l'année
 *       de la condamnation pénale ou décision administrative
 *       définitive antérieure.</li>
 *   <li><b>Préposé / mandataire</b> ({@code prevenuPreposeOuMandataire})
 *       — si le prévenu est un préposé / mandataire et non
 *       l'employeur en personne (art. 109 C. pén. soc.).
 *       Information sur la qualité de l'auteur ; n'affecte pas le
 *       niveau d'amende mais conditionne le régime de responsabilité
 *       de l'employeur.</li>
 *   <li><b>Élément moral intentionnel</b>
 *       ({@code elementMoralIntentionnel}) — pour les infractions à
 *       double niveau selon dol (typiquement ABSENCE_DMFA art. 223 :
 *       niveau 4 si frauduleux, niveau 2 sinon ; et FAUX_SOCIAL
 *       art. 232 — toujours niveau 4 car l'intention est constitutive
 *       de la qualification). Indication doctrinale et
 *       jurisprudentielle, l'avocat tranche.</li>
 * </ol>
 */
public record CodePenalSocialBeRequest(

        @NotNull(message = "typeInfraction est requis")
        CodePenalSocialBeTypeInfraction typeInfraction,

        @Min(value = 1, message = "niveauPropose doit être entre 1 et 4")
        @Max(value = 4, message = "niveauPropose doit être entre 1 et 4")
        Integer niveauPropose,

        LocalDate dateFaits,

        @NotNull(message = "nombreTravailleursConcernes est requis")
        @Min(value = 0, message = "nombreTravailleursConcernes doit être positif ou nul")
        @Max(value = 1000000, message = "nombreTravailleursConcernes plafonné à 1 000 000")
        Integer nombreTravailleursConcernes,

        @NotNull(message = "personneMorale est requis")
        Boolean personneMorale,

        @NotNull(message = "recidiveDansLAn est requis")
        Boolean recidiveDansLAn,

        @NotNull(message = "prevenuPreposeOuMandataire est requis")
        Boolean prevenuPreposeOuMandataire,

        @NotNull(message = "elementMoralIntentionnel est requis")
        Boolean elementMoralIntentionnel
) {
}
