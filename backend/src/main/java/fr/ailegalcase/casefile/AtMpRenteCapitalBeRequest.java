package fr.ailegalcase.casefile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-29 : payload de creation / mise a jour de l'analyse <i>Rente
 * AT/MP vs capitalisation BE</i> (Loi du 10/04/1971 art. 24 ; Lois
 * coordonnees du 03/06/1970 art. 35 ; AR du 21/12/1971 + AR
 * 24/02/2005 - bareme).
 *
 * <h2>Logique metier - elements cumulatifs</h2>
 * <ol>
 *   <li><b>Origine</b> ({@code origine}) - voir
 *       {@link AtMpRenteCapitalBeOrigine}. Determine l'intitule du
 *       verdict et les voies de recours. Pas d'effet sur le calcul du
 *       montant : MP renvoient au regime AT art. 35 lois coordonnees
 *       03/06/1970.</li>
 *   <li><b>Statut de reconnaissance</b> ({@code statutReconnaissance}) -
 *       voir {@link AtMpRenteCapitalBeStatutReconnaissance}. RECONNU
 *       permet le calcul effectif ; CONTESTE / EN_COURS produit un
 *       verdict indicatif ou INELIGIBLE.</li>
 *   <li><b>Date de consolidation</b> ({@code dateConsolidation}) -
 *       date d'arret de l'incapacite temporaire et de fixation du taux
 *       IPP par l'expertise medicale Fedris / assureur-loi. Point de
 *       depart du regime de rente / capital art. 24 al. 3 Loi
 *       10/04/1971. Optionnelle si statut EN_COURS.</li>
 *   <li><b>Taux IPP</b> ({@code tauxIpp}) - taux d'incapacite
 *       permanente partielle en pourcentage, fixe par l'expertise
 *       medicale Fedris (MP) ou assureur-loi (AT). Determine le mode
 *       de versement : &lt; 19% capitalisation, &gt;= 19% rente. Valeur
 *       entre 0 et 100 inclus. Si null, statut doit etre EN_COURS ou
 *       CONTESTE.</li>
 *   <li><b>Remuneration de base annuelle</b>
 *       ({@code remunerationBaseAnnuelle}) - remuneration annuelle de
 *       reference pour le calcul de l'indemnite : derniere remuneration
 *       brute annuelle de la victime art. 34 Loi 10/04/1971,
 *       plafonnee art. 39 (plafond annuel ONSS - 49 853,86 EUR au
 *       01/01/2024, indexe annuellement). Valeur strictement positive
 *       en EUR.</li>
 *   <li><b>Date de naissance</b> ({@code dateNaissance}) - date de
 *       naissance de la victime - permet le calcul du coefficient
 *       d'age applicable au bareme de capitalisation AR 24/02/2005
 *       (Table I-bis - coefficient decroissant avec l'age, du
 *       quasi-13 a 1 an au quasi-0 a 75 ans).</li>
 *   <li><b>Demande de conversion partielle</b>
 *       ({@code demandeConversionPartielle}) - drapeau indiquant que
 *       la victime souhaite convertir 1/3 maximum de la rente en
 *       capital art. 45ter Loi 10/04/1971 + AR 10/12/1987. Possible
 *       uniquement apres consolidation depuis 3 ans (delai d'epreuve
 *       art. 24 § 3 AR 03/07/1996) et si verdict = RENTE_ANNUELLE_GE_19.</li>
 *   <li><b>Date de demande de conversion</b>
 *       ({@code dateDemandeConversion}) - date envisagee de la demande
 *       de conversion (mode prospectif). Si null, le calculateur
 *       utilise la date du jour Europe/Brussels.</li>
 * </ol>
 */
public record AtMpRenteCapitalBeRequest(

        @NotNull(message = "origine est requis")
        AtMpRenteCapitalBeOrigine origine,

        @NotNull(message = "statutReconnaissance est requis")
        AtMpRenteCapitalBeStatutReconnaissance statutReconnaissance,

        LocalDate dateConsolidation,

        @DecimalMin(value = "0.0", message = "tauxIpp >= 0")
        @DecimalMax(value = "100.0", message = "tauxIpp <= 100")
        BigDecimal tauxIpp,

        @NotNull(message = "remunerationBaseAnnuelle est requis")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "remunerationBaseAnnuelle > 0")
        BigDecimal remunerationBaseAnnuelle,

        @NotNull(message = "dateNaissance est requis")
        LocalDate dateNaissance,

        boolean demandeConversionPartielle,

        LocalDate dateDemandeConversion,

        @Min(value = 0, message = "ageConsolidation >= 0")
        @Max(value = 120, message = "ageConsolidation <= 120")
        Integer ageConsolidationOverride
) {
}
