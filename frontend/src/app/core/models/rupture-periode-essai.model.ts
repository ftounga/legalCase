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

/** Motif de discrimination (subset L.1132-1). */
export type DiscriminationMotif =
  | 'RACE_ORIGINE'
  | 'SEXE'
  | 'GROSSESSE'
  | 'SANTE'
  | 'SYNDICAL'
  | 'AUTRE';

/** Codes des 12 anomalies. */
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
  | 'CONVENTION_COLLECTIVE_NON_RESPECTEE';

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
