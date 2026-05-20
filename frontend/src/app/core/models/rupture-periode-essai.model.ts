/**
 * SF-DT-38-02 : modèles TypeScript de l'outil "Rupture de période d'essai
 * (qualification régulière / abusive / nulle / illégale)" (F-DT-38).
 * FRANCE uniquement.
 *
 * Contrat API importé de SF-DT-38-01 (backend, endpoints POST/GET figés).
 */

/** Verdict 4 niveaux. */
export type RupturePeriodeEssaiVerdict =
  | 'REGULIERE'
  | 'RISQUE_ABUSIVE'
  | 'NULLE'
  | 'ILLEGALE_REQUALIF_LICENCIEMENT';

/** Gravité d'une anomalie. */
export type RupturePeriodeEssaiGravite = 'AVERE' | 'PROBABLE';

/** Catégorie socio-professionnelle (durée légale L.1221-19). */
export type CategorieSocioProfessionnelle =
  | 'OUVRIER_EMPLOYE'
  | 'AGENT_MAITRISE_TECHNICIEN'
  | 'CADRE';

/** Type de contrat. */
export type TypeContratEssai = 'CDI' | 'CDD' | 'INTERIM';

/** Auteur de la rupture. */
export type AuteurRupture = 'EMPLOYEUR' | 'SALARIE';

/**
 * Motif de discrimination L.1132-1 (liste exhaustive 2026).
 *
 * SF-DT-38-02 : 6 valeurs legacy conservées pour rétrocompat.
 * SF-252-01 (audit 2026-05-20) : +17 motifs L.1132-1 manquants.
 */
export type DiscriminationMotif =
  // SF-DT-38-02 (legacy)
  | 'RACE_ORIGINE'
  | 'SEXE'
  | 'GROSSESSE'
  | 'SANTE'
  | 'SYNDICAL'
  | 'AUTRE'
  // SF-252-01 — L.1132-1 motifs exhaustifs
  | 'MOEURS'
  | 'ORIENTATION_SEXUELLE'
  | 'IDENTITE_GENRE'
  | 'AGE'
  | 'SITUATION_FAMILLE'
  | 'CARACTERISTIQUES_GENETIQUES'
  | 'VULNERABILITE_ECONOMIQUE'
  | 'OPINIONS_POLITIQUES'
  | 'CONVICTIONS_RELIGIEUSES'
  | 'APPARENCE_PHYSIQUE'
  | 'NOM_DE_FAMILLE'
  | 'LIEU_DE_RESIDENCE'
  | 'DOMICILIATION_BANCAIRE'
  | 'PERTE_AUTONOMIE'
  | 'HANDICAP'
  | 'CAPACITE_LANGUE_FRANCAISE'
  | 'FONCTIONS_JURIDICTIONNELLES';

/** Codes des anomalies (12 SF-DT-38-01 + 5 SF-252-01). */
export type RupturePeriodeEssaiCodeAnomalie =
  | 'PERIODE_ESSAI_ABSENTE'
  | 'DUREE_ESSAI_DEPASSEE'
  | 'RENOUVELLEMENT_IRREGULIER'
  | 'RUPTURE_HORS_PERIODE_ESSAI'
  | 'DELAI_PREVENANCE_INSUFFISANT'
  | 'MOTIF_NON_PROFESSIONNEL'
  | 'MOTIF_ETRANGER_A_ESSAI'
  | 'DISCRIMINATION_AVEREE'
  | 'GROSSESSE_PROTECTION_VIOLEE'
  | 'AT_MP_PROTECTION_VIOLEE'
  | 'ATTEINTE_LIBERTE_FONDAMENTALE'
  | 'CONVENTION_COLLECTIVE_NON_RESPECTEE'
  // SF-252-01 — 5 protections nullité additionnelles (audit 2026-05-20)
  | 'SALARIE_PROTEGE_SANS_AUTORISATION'
  | 'LANCEUR_ALERTE_PROTECTION_VIOLEE'
  | 'TEMOIN_HARCELEMENT_PROTECTION_VIOLEE'
  | 'DROIT_RETRAIT_PROTECTION_VIOLEE'
  | 'GROSSESSE_NOTIFIEE_POST_RUPTURE';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/rupture-periode-essai`.
 * Dates au format ISO `YYYY-MM-DD`. Commentaires nullable.
 */
export interface RupturePeriodeEssaiRequest {
  categorieSocioProfessionnelle: CategorieSocioProfessionnelle;
  typeContrat: TypeContratEssai;
  dureeCddMois: number | null;
  dateDebutContrat: string;
  dateRupture: string;
  dureePeriodeEssaiContractuelleMois: number;
  renouvellementInvoque: boolean | null;
  accordBrancheRenouvellement: boolean | null;
  accordEcritSalarieRenouvellement: boolean | null;
  auteurRupture: AuteurRupture;
  delaiPrevenanceJoursAppliques: number | null;
  motifInvoque: string | null;
  motifLieAuxCompetencesProfessionnelles: boolean | null;
  motifEconomiqueOuOrganisationnel: boolean | null;
  discriminationInvoquee: DiscriminationMotif | null;
  grossesseAuMomentRupture: boolean | null;
  arretAccidentTravailEnCours: boolean | null;
  atteinteLiberteFondamentale: string | null;
  lettreRuptureMotivee: boolean | null;
  motifsAveresParPieces: boolean | null;
  conventionCollectiveApplicable: boolean | null;
  conventionCollectivePlusFavorableRespectee: boolean | null;
  salaireMensuelBrut: number | null;
  // SF-252-01 — 7 nouveaux champs (audit F-DT-38 2026-05-20)
  salarieProtege: boolean | null;
  autorisationInspectionTravailObtenue: boolean | null;
  lanceurAlerte: boolean | null;
  temoinOuVictimeHarcelement: boolean | null;
  droitDeRetraitExerce: boolean | null;
  grossesseDeclareePostRupture: boolean | null;
  dateNotificationGrossesse: string | null;
}

/** Anomalie détectée (élément de `anomaliesDetectees`). */
export interface RupturePeriodeEssaiAnomalie {
  code: RupturePeriodeEssaiCodeAnomalie;
  libelle: string;
  fondement: string;
  gravite: RupturePeriodeEssaiGravite;
  explication: string;
}

/** Indemnité estimée (verdict RISQUE_ABUSIVE — fourchette 1 à 6 mois). */
export interface RupturePeriodeEssaiIndemnite {
  montantMinEuros: number | null;
  montantMaxEuros: number | null;
  baseCalcul: string;
  fondement: string;
}

/** Réponse de l'endpoint POST / GET. */
export interface RupturePeriodeEssaiResponse extends RupturePeriodeEssaiRequest {
  caseFileId: string;
  verdict: RupturePeriodeEssaiVerdict;
  scoreIrregularite: number;
  ancienneteJoursAuMomentRupture: number;
  dureeLegaleMaximaleMois: number;
  delaiPrevenanceLegalJours: number;
  delaiPrevenanceRespecte: boolean;
  anomaliesDetectees: RupturePeriodeEssaiAnomalie[];
  indemniteEstimee: RupturePeriodeEssaiIndemnite | null;
  remedeReintegration: boolean;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
