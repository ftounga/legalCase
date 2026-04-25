/**
 * SF-DT-35-02 : modèles TypeScript pour l'outil décisionnel
 * "Contestation ARE / France Travail (ex-Pôle emploi)" (F-DT-35).
 * FR uniquement. Contrat figé par SF-DT-35-01 (backend).
 */

export type TypeDecisionContestee =
  | 'REFUS_OUVERTURE_DROITS'
  | 'MONTANT_INDEMNITE'
  | 'DUREE_INDEMNITE'
  | 'RADIATION'
  | 'TROP_PERCU'
  | 'AUTRE';

export type MotifContestationAre =
  | 'ERREUR_CALCUL_REMUNERATION_REFERENCE'
  | 'MAUVAISE_QUALIFICATION_RUPTURE'
  | 'OMISSION_PERIODES_TRAVAIL'
  | 'REFUS_INJUSTIFIE'
  | 'AUTRE';

export type PreuveAre =
  | 'BULLETIN_PAIE'
  | 'ATTESTATION_EMPLOYEUR'
  | 'CERTIFICAT_TRAVAIL'
  | 'LETTRE_LICENCIEMENT'
  | 'JUGEMENT_PRUDHOMAL'
  | 'AUTRE';

export type VerdictAre =
  | 'RECOURS_HIERARCHIQUE_PRIORITAIRE'
  | 'CONTENTIEUX_TA_DIRECT_POSSIBLE'
  | 'INSUFFISAMMENT_FONDE'
  | 'AUTRE_VOIE';

export interface ContestationAreRequest {
  typeDecisionContestee: TypeDecisionContestee;
  motifContestation: MotifContestationAre;
  dateNotificationDecision: string;       // YYYY-MM-DD
  dateRecoursHierarchiquePropose: string; // YYYY-MM-DD
  preuvesProduites: PreuveAre[];
  montantContesteEur: number;
  demandeurDejaSaisiTribunal: boolean;
  delaiContestationRespecte: boolean;
}

export interface ContestationAreResponse {
  caseFileId: string;
  // Inputs reflétés (pour mode résultat → édition).
  typeDecisionContestee: TypeDecisionContestee;
  motifContestation: MotifContestationAre;
  dateNotificationDecision: string;
  dateRecoursHierarchiquePropose: string;
  preuvesProduites: PreuveAre[];
  montantContesteEur: number;
  demandeurDejaSaisiTribunal: boolean;
  delaiContestationRespecte: boolean;
  // Outputs
  delaiRecoursHierarchiqueJoursOk: boolean;
  delaiRecoursContentieuxTaJoursOk: boolean;
  scoreSuccessProbable: number; // 0..100
  verdictRecommandation: VerdictAre;
  delaiInstructionMoisPrevisionnel: number;
  expertiseTraitementSalaireRecommandee: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface TypeDecisionContesteeOption {
  code: TypeDecisionContestee;
  label: string;
}

export interface MotifContestationAreOption {
  code: MotifContestationAre;
  label: string;
}

export interface PreuveAreOption {
  code: PreuveAre;
  label: string;
}

export const TYPES_DECISION_CONTESTEE: TypeDecisionContesteeOption[] = [
  { code: 'REFUS_OUVERTURE_DROITS', label: 'Refus d\'ouverture des droits' },
  { code: 'MONTANT_INDEMNITE',      label: 'Montant de l\'indemnité' },
  { code: 'DUREE_INDEMNITE',        label: 'Durée d\'indemnisation' },
  { code: 'RADIATION',              label: 'Radiation' },
  { code: 'TROP_PERCU',             label: 'Trop-perçu / récupération d\'indu' },
  { code: 'AUTRE',                  label: 'Autre décision' },
];

export const MOTIFS_CONTESTATION_ARE: MotifContestationAreOption[] = [
  { code: 'ERREUR_CALCUL_REMUNERATION_REFERENCE',
    label: 'Erreur de calcul de la rémunération de référence' },
  { code: 'MAUVAISE_QUALIFICATION_RUPTURE',
    label: 'Mauvaise qualification de la rupture' },
  { code: 'OMISSION_PERIODES_TRAVAIL',
    label: 'Omission de périodes de travail' },
  { code: 'REFUS_INJUSTIFIE',
    label: 'Refus injustifié' },
  { code: 'AUTRE',
    label: 'Autre motif' },
];

export const PREUVES_ARE: PreuveAreOption[] = [
  { code: 'BULLETIN_PAIE',         label: 'Bulletin(s) de paie' },
  { code: 'ATTESTATION_EMPLOYEUR', label: 'Attestation employeur (France Travail)' },
  { code: 'CERTIFICAT_TRAVAIL',    label: 'Certificat de travail' },
  { code: 'LETTRE_LICENCIEMENT',   label: 'Lettre de licenciement' },
  { code: 'JUGEMENT_PRUDHOMAL',    label: 'Jugement prud\'homal' },
  { code: 'AUTRE',                 label: 'Autre pièce' },
];

/** Verdict labels (pour bannière). */
export const VERDICT_ARE_LABELS: Record<VerdictAre, string> = {
  RECOURS_HIERARCHIQUE_PRIORITAIRE: 'Recours hiérarchique prioritaire',
  CONTENTIEUX_TA_DIRECT_POSSIBLE:   'Contentieux TA direct possible',
  INSUFFISAMMENT_FONDE:             'Contestation insuffisamment fondée',
  AUTRE_VOIE:                       'Autre voie de recours',
};
