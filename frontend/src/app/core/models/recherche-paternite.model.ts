/**
 * SF-FA-18-06 : modèles TypeScript pour l'outil décisionnel
 * "Action en recherche de paternité" (F-FA-18 — FR uniquement,
 * art. 327 + 340 + 16-11 + 321 Cciv).
 *
 * Contrat figé dans SF-FA-18-05 (backend, mergé PR #664).
 */

/** Qualité du demandeur (art. 327 al. 2 Cciv). */
export type QualiteDuDemandeurRecherche =
  | 'ENFANT_MAJEUR'
  | 'REPRESENTANT_LEGAL_MINEUR'
  | 'MERE';

/** Verdict de recevabilité (scoring niveau 5). */
export type VerdictRecevabiliteRecherche = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

/** Libellés humains pour mat-radio qualité du demandeur. */
export const QUALITE_DEMANDEUR_RECHERCHE_LABELS:
  ReadonlyArray<{ code: QualiteDuDemandeurRecherche; label: string; sub: string }> = [
  {
    code: 'ENFANT_MAJEUR',
    label: 'Enfant majeur (art. 327 al. 2 + 321)',
    sub: 'L\'enfant majeur agit lui-même — délai 10 ans à compter de la majorité',
  },
  {
    code: 'REPRESENTANT_LEGAL_MINEUR',
    label: 'Représentant légal du mineur (art. 327 al. 2)',
    sub: 'Tuteur ou parent agissant au nom du mineur — pas de prescription tant que minorité',
  },
  {
    code: 'MERE',
    label: 'Mère (art. 327 al. 2 — représentation légale)',
    sub: 'Mère agissant en représentation légale de son enfant mineur',
  },
];

export interface RecherchePaterniteRequest {
  qualiteDuDemandeur: QualiteDuDemandeurRecherche;
  /** ISO YYYY-MM-DD — obligatoire. */
  dateNaissanceEnfant: string;
  presomptionPossessionEtat: boolean;
  expertiseAdnDemandee: boolean;
  pereDesigneRefuseADN: boolean;
  motifsSerieux: boolean;
}

export interface RecherchePaterniteResponse {
  caseFileId: string;
  qualiteDuDemandeur: QualiteDuDemandeurRecherche;
  verdictRecevabilite: VerdictRecevabiliteRecherche;
  scoreRecevabilite: number;
  delaiPrescriptionAns: number;
  delaiPrescriptionRestantMois: number;
  expertiseAdnRecommandee: boolean;
  presomptionRefusADN: boolean;
  risquesRefus: string[];
  documentsRequis: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
