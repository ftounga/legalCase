package fr.ailegalcase.casefile;

/**
 * SF-219-12 : verdict de l'outil <i>flexi-job BE</i> (régime du
 * flexi-job institué par la <b>Loi-programme du 26 décembre 2013</b>
 * (art. 13 à 28), modifiée et élargie par les Lois-programme du
 * 16/11/2015 (extension boulangerie / coiffure), du 30/10/2018
 * (extension commerce de détail / agriculture / soins de santé), et
 * par la Loi-programme du 28/12/2023 (extension secteurs publics
 * limités, hausse plafonds, indexation du flexi-salaire minimum).
 *
 * <h2>Conditions cumulatives V1</h2>
 * <ol>
 *   <li><b>Côté travailleur</b> — soit pensionné (toutes catégories,
 *       sans plafond cumul depuis 2018), soit salarié occupé chez un
 *       <i>autre</i> employeur à concurrence d'au moins 4/5 ETP au
 *       cours du <b>3e trimestre civil précédant</b> l'occupation
 *       flexi (référence T-3, ONSS DmfA).</li>
 *   <li><b>Côté employeur</b> — exerce dans un secteur <b>éligible</b>
 *       (HoReCa CP 302 + extensions successives). Pas de prestations
 *       flexi simultanées chez l'employeur principal du T-3.</li>
 *   <li><b>Contrat-cadre écrit</b> (art. 16 Loi-prog. 26/12/2013) puis
 *       contrat d'occupation flexi-job (oral ou écrit) + déclaration
 *       <b>Dimona spécifique</b> (DmfA flexi).</li>
 *   <li><b>Flexi-salaire minimum</b> indexé (~12,33 € horaire brut +
 *       8 % pécule = 13,31 € au 01/05/2024) + plafonds annuels
 *       (~12 000 € exonérés en 2024, hors pension).</li>
 * </ol>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #ELIGIBLE} — toutes les conditions cumulatives sont
 *       réunies, occupation flexi-job <b>régulière</b>.</li>
 *   <li>{@link #INELIGIBLE_TRAVAILLEUR_HORS_CONDITION} — ni pensionné,
 *       ni 4/5 ETP T-3 — pas de statut flexi possible.</li>
 *   <li>{@link #INELIGIBLE_SECTEUR_NON_ELIGIBLE} — l'employeur n'opère
 *       pas dans un secteur ouvert au flexi-job par la loi.</li>
 *   <li>{@link #INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR} — le
 *       travailleur est déjà occupé chez le <b>même</b> employeur
 *       (interdiction cumul prestations principales + flexi).</li>
 *   <li>{@link #FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} — conditions de
 *       fond OK mais contrat-cadre absent / Dimona FLX non déclarée —
 *       statut formellement irrégulier (risque requalification ONSS
 *       au régime normal + récupération cotisations).</li>
 *   <li>{@link #FRAGILE_PLAFOND_DEPASSE} — flexi-salaire sous le
 *       minimum légal OU plafond annuel dépassé (≥ 12 000 € hors
 *       pension) — partie excédentaire soumise à cotisations
 *       normales + taxation.</li>
 *   <li>{@link #A_ANALYSER} — secteur en zone grise (extension récente
 *       en cours d'AR d'application) — analyse juridique requise.</li>
 * </ul>
 */
public enum FlexiJobBeVerdict {
    /** Occupation flexi-job régulière — toutes conditions cumulatives réunies. */
    ELIGIBLE,

    /** Statut formellement défaillant (contrat-cadre / Dimona) — à régulariser. */
    FRAGILE_CONTRAT_OU_DIMONA_MANQUANT,

    /** Plafond annuel dépassé ou flexi-salaire sous minimum — partie excédentaire requalifiée. */
    FRAGILE_PLAFOND_DEPASSE,

    /** Travailleur ne remplit ni la condition pension ni la condition 4/5 ETP T-3. */
    INELIGIBLE_TRAVAILLEUR_HORS_CONDITION,

    /** Secteur de l'employeur non listé parmi les secteurs flexi éligibles. */
    INELIGIBLE_SECTEUR_NON_ELIGIBLE,

    /** Cumul interdit — le travailleur est déjà occupé chez le même employeur. */
    INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR,

    /** Secteur en zone grise (extension récente sans AR d'application) — avocat BE requis. */
    A_ANALYSER
}
