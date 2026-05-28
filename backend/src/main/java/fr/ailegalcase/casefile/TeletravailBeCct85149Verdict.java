package fr.ailegalcase.casefile;

/**
 * SF-219-16 : verdict de l'outil <i>télétravail BE — CCT n° 85 (structurel)
 * / CCT n° 149 (occasionnel)</i>.
 *
 * <h2>Conditions cumulatives V1 (télétravail structurel CCT n° 85)</h2>
 * <ol>
 *   <li><b>Volontariat réciproque</b> — art. 5 CCT n° 85 : le télétravail
 *       est mis en place sur base d'un accord volontaire entre l'employeur
 *       et le travailleur (refus / fin sans préjudice juridique).</li>
 *   <li><b>Convention individuelle écrite</b> — art. 6 CCT n° 85 :
 *       écrit obligatoire par télétravailleur reprenant fréquence,
 *       jours, lieu d'exécution, équipement, indemnité, période d'essai
 *       éventuelle, modalités de réversibilité.</li>
 *   <li><b>Équipement fourni par l'employeur</b> — art. 9 CCT n° 85 :
 *       l'employeur fournit, installe et entretient les équipements
 *       nécessaires (ordinateur, accès, support technique) ou prend en
 *       charge les coûts si le travailleur utilise son propre matériel.</li>
 *   <li><b>Indemnité forfaitaire frais</b> — usage admis (ONSS / fisc) :
 *       remboursement forfaitaire des frais d'internet, électricité,
 *       chauffage, ergonomie (plafonds ONSS/SPF Finances révisés
 *       annuellement, voir avocat BE pour la date d'occupation).</li>
 *   <li><b>Droits sociaux maintenus</b> — art. 4 CCT n° 85 : le
 *       télétravailleur bénéficie des mêmes droits que les travailleurs
 *       exerçant dans les locaux de l'employeur (formation, carrière,
 *       protection sociale, droit collectif, ...).</li>
 *   <li><b>Droit à la déconnexion</b> — Loi du 26/03/2018 (art. 16-18)
 *       + Loi du 03/10/2022 (entreprises ≥ 20 travailleurs) : modalités
 *       prévues collectivement (CCT d'entreprise ou règlement de travail).</li>
 * </ol>
 *
 * <h2>Verdicts hiérarchisés</h2>
 * <ul>
 *   <li>{@link #CONFORME_CCT_85_STRUCTUREL} — télétravail structurel
 *       régulier conforme aux conditions cumulatives CCT n° 85.</li>
 *   <li>{@link #CONFORME_CCT_149_OCCASIONNEL} — télétravail occasionnel
 *       relevant de la CCT n° 149 (force majeure / raison personnelle /
 *       COVID), cadre minimal respecté.</li>
 *   <li>{@link #NON_CONFORME_CONVENTION_ECRITE_MANQUANTE} — télétravail
 *       structurel pratiqué sans convention écrite individuelle
 *       (art. 6 CCT n° 85) → présomption d'irrégularité, exposition à
 *       requalification + sanctions du règlement de travail.</li>
 *   <li>{@link #NON_CONFORME_EQUIPEMENT_NON_FOURNI} — l'employeur ne
 *       fournit pas / n'indemnise pas l'équipement nécessaire (art. 9
 *       CCT n° 85) → manquement contractuel, rappel de frais
 *       réclamable.</li>
 *   <li>{@link #NON_CONFORME_DROITS_REDUITS} — le télétravailleur ne
 *       bénéficie pas des mêmes droits (formation, carrière, accès
 *       collectif) que les présentiels — discrimination art. 4 CCT
 *       n° 85.</li>
 *   <li>{@link #FRAGILE_DECONNEXION_NON_DEFINIE} — entreprise &gt;= 20
 *       travailleurs sans accord collectif / règlement de travail
 *       fixant les modalités de déconnexion (Loi 03/10/2022) → sanction
 *       SPF ETCS possible.</li>
 *   <li>{@link #A_ANALYSER} — type de télétravail indéterminé ou
 *       configuration mixte (combinaison structurel/occasionnel) à
 *       analyser au cas par cas.</li>
 * </ul>
 */
public enum TeletravailBeCct85149Verdict {
    /** Télétravail structurel conforme à la CCT n° 85 — toutes conditions cumulatives réunies. */
    CONFORME_CCT_85_STRUCTUREL,

    /** Télétravail occasionnel relevant de la CCT n° 149 — cadre minimal OK. */
    CONFORME_CCT_149_OCCASIONNEL,

    /** Télétravail structurel sans convention écrite individuelle — non-conformité art. 6 CCT n° 85. */
    NON_CONFORME_CONVENTION_ECRITE_MANQUANTE,

    /** Équipement non fourni / non indemnisé par l'employeur — non-conformité art. 9 CCT n° 85. */
    NON_CONFORME_EQUIPEMENT_NON_FOURNI,

    /** Droits réduits par rapport aux travailleurs présentiels — non-conformité art. 4 CCT n° 85. */
    NON_CONFORME_DROITS_REDUITS,

    /** Modalités de déconnexion non définies (entreprise ≥ 20 travailleurs) — manquement Loi 03/10/2022. */
    FRAGILE_DECONNEXION_NON_DEFINIE,

    /** Type de télétravail indéterminé / configuration mixte — analyse cas par cas requise. */
    A_ANALYSER
}
