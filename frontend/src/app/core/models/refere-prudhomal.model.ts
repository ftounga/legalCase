/**
 * SF-DT-34-02 : modèles TypeScript pour l'outil décisionnel
 * "Référé prud'homal — France" (F-DT-34, art. R.1454-1 Code du travail).
 * FR uniquement. Contrat figé par SF-DT-34-01 (backend, parallèle).
 */

export type TypeRefere =
  | 'PROVISION_SALAIRES'
  | 'EXPERTISE_MEDICALE'
  | 'EXPERTISE_TECHNIQUE'
  | 'MESURES_CONSERVATOIRES'
  | 'REINTEGRATION_URGENCE'
  | 'AUTRE';

export type NatureCreance =
  | 'SALAIRES_NON_VERSES'
  | 'INDEMNITE_RUPTURE'
  | 'HEURES_SUPPLEMENTAIRES'
  | 'PRIMES'
  | 'CONGES_PAYES'
  | 'AUTRE';

export type PreuveUrgence =
  | 'BULLETIN_PAIE'
  | 'RELANCE_EMPLOYEUR'
  | 'MISE_EN_DEMEURE'
  | 'CONSTAT_HUISSIER'
  | 'CERTIFICAT_MEDICAL'
  | 'CONTRAT'
  | 'AUTRE';

export type VerdictRefereDt =
  | 'PROVISION_PROBABLE'
  | 'EXPERTISE_RECOMMANDEE'
  | 'INSUFFISAMMENT_FONDE'
  | 'AUTRE_VOIE_RECOMMANDEE';

export interface ReferePrudhomalRequest {
  typeRefere: TypeRefere;
  natureCreance: NatureCreance;
  montantProvisionDemandeeEur: number;
  absenceContestationSerieuse: boolean;
  preuvesUrgenceProduites: PreuveUrgence[];
  dommageImmediatCarac: boolean;
  trésorerieEmployeurDouteuse: boolean;
  dateMiseEnDemeure: string; // YYYY-MM-DD
  ancienneteContratMois: number;
}

export interface ReferePrudhomalResponse {
  caseFileId: string;
  // Snapshot inputs reflétés (pour mode résultat → édition).
  typeRefere: TypeRefere;
  natureCreance: NatureCreance;
  montantProvisionDemandeeEur: number;
  absenceContestationSerieuse: boolean;
  preuvesUrgenceProduites: PreuveUrgence[];
  dommageImmediatCarac: boolean;
  trésorerieEmployeurDouteuse: boolean;
  dateMiseEnDemeure: string;
  ancienneteContratMois: number;
  // Outputs scoring.
  scoreSuccess: number; // 0..100
  verdictRecommandation: VerdictRefereDt;
  delaiAudienceJoursPrevisionnel: number;
  delaiOrdonnanceJoursPrevisionnel: number;
  montantProvisionRecommandeEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface TypeRefereOption {
  code: TypeRefere;
  label: string;
}

export interface NatureCreanceOption {
  code: NatureCreance;
  label: string;
}

export interface PreuveUrgenceOption {
  code: PreuveUrgence;
  label: string;
}

export const TYPES_REFERE: TypeRefereOption[] = [
  { code: 'PROVISION_SALAIRES', label: 'Provision sur salaires / indemnités' },
  { code: 'EXPERTISE_MEDICALE', label: 'Expertise médicale (inaptitude / AT-MP)' },
  { code: 'EXPERTISE_TECHNIQUE', label: 'Expertise technique (heures sup, classification)' },
  { code: 'MESURES_CONSERVATOIRES', label: 'Mesures conservatoires (saisies, séquestre)' },
  { code: 'REINTEGRATION_URGENCE', label: 'Réintégration en urgence' },
  { code: 'AUTRE', label: 'Autre demande de référé' },
];

export const NATURES_CREANCE: NatureCreanceOption[] = [
  { code: 'SALAIRES_NON_VERSES', label: 'Salaires non versés' },
  { code: 'INDEMNITE_RUPTURE', label: 'Indemnité de rupture' },
  { code: 'HEURES_SUPPLEMENTAIRES', label: 'Heures supplémentaires' },
  { code: 'PRIMES', label: 'Primes' },
  { code: 'CONGES_PAYES', label: 'Congés payés' },
  { code: 'AUTRE', label: 'Autre créance salariale' },
];

export const PREUVES_URGENCE: PreuveUrgenceOption[] = [
  { code: 'BULLETIN_PAIE', label: 'Bulletins de paie' },
  { code: 'RELANCE_EMPLOYEUR', label: 'Relance écrite de l\'employeur' },
  { code: 'MISE_EN_DEMEURE', label: 'Mise en demeure (LRAR)' },
  { code: 'CONSTAT_HUISSIER', label: 'Constat d\'huissier' },
  { code: 'CERTIFICAT_MEDICAL', label: 'Certificat médical' },
  { code: 'CONTRAT', label: 'Contrat de travail' },
  { code: 'AUTRE', label: 'Autre preuve d\'urgence' },
];

/** Verdict labels (pour bannière). */
export const VERDICT_LABELS: Record<VerdictRefereDt, string> = {
  PROVISION_PROBABLE: 'Provision probable — référé prud\'homal recommandé',
  EXPERTISE_RECOMMANDEE: 'Expertise recommandée — référé pertinent',
  INSUFFISAMMENT_FONDE: 'Conditions insuffisamment caractérisées',
  AUTRE_VOIE_RECOMMANDEE: 'Une autre voie procédurale est recommandée',
};
