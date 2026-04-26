/**
 * SF-FA-24-02 : modèles TypeScript pour l'outil décisionnel "Dévolution
 * légale successorale" (F-FA-24). FR uniquement — art. 731 et s. Cciv
 * (4 ordres + spécial conjoint survivant + représentation + fente).
 *
 * Contrat figé dans SF-FA-24-01 (backend, mergé PR #651).
 */

/** Option du conjoint survivant (présence de descendants tous communs). */
export type OptionConjoint = 'USUFRUIT' | 'QUART';

/** Ordre d'héritiers actif dans la dévolution. */
export type OrdreActif =
  | 'DESCENDANTS'
  | 'PRIVILEGIES'
  | 'ASCENDANTS_ORDINAIRES'
  | 'COLLATERAUX_ORDINAIRES'
  | 'CONJOINT_SEUL'
  | 'DESHERENCE';

/** Qualité de l'héritier désigné. */
export type QualiteHeritier =
  | 'CONJOINT'
  | 'DESCENDANT'
  | 'PERE'
  | 'MERE'
  | 'FRERE_SOEUR'
  | 'ASCENDANT_ORDINAIRE_PATERNEL'
  | 'ASCENDANT_ORDINAIRE_MATERNEL'
  | 'COLLATERAL_ORDINAIRE'
  | 'REPRESENTANT';

/** Modalité de propriété (pleine, usufruit, nue-propriété). */
export type ModaliteHeritier = 'PLEINE_PROPRIETE' | 'USUFRUIT' | 'NUE_PROPRIETE';

/** Héritier désigné par la dévolution avec sa quote-part. */
export interface HeritierDesigne {
  qualite: QualiteHeritier;
  ordre: number;
  quotePartPct: number;
  modalite: ModaliteHeritier;
}

/** Libellés humains des ordres d'héritiers (chip indicateur graphique). */
export const ORDRE_ACTIF_LABELS: ReadonlyArray<{ code: OrdreActif; label: string; numero: string }> = [
  { code: 'DESCENDANTS', label: 'Ordre 1 — Descendants', numero: '1' },
  { code: 'PRIVILEGIES', label: 'Ordre 2 — Privilégiés (parents + frères/sœurs)', numero: '2' },
  { code: 'ASCENDANTS_ORDINAIRES', label: 'Ordre 3 — Ascendants ordinaires', numero: '3' },
  { code: 'COLLATERAUX_ORDINAIRES', label: 'Ordre 4 — Collatéraux ordinaires', numero: '4' },
  { code: 'CONJOINT_SEUL', label: 'Conjoint seul (art. 757-2)', numero: 'C' },
  { code: 'DESHERENCE', label: 'Succession en déshérence (art. 768)', numero: '∅' },
];

/** Libellés humains des qualités d'héritiers (tableau résultat). */
export const QUALITE_HERITIER_LABELS: Readonly<Record<QualiteHeritier, string>> = {
  CONJOINT: 'Conjoint',
  DESCENDANT: 'Descendant',
  PERE: 'Père',
  MERE: 'Mère',
  FRERE_SOEUR: 'Frère/Sœur',
  ASCENDANT_ORDINAIRE_PATERNEL: 'Ascendant paternel',
  ASCENDANT_ORDINAIRE_MATERNEL: 'Ascendant maternel',
  COLLATERAL_ORDINAIRE: 'Collatéral ordinaire',
  REPRESENTANT: 'Représentant',
};

/** Libellés humains des modalités. */
export const MODALITE_HERITIER_LABELS: Readonly<Record<ModaliteHeritier, string>> = {
  PLEINE_PROPRIETE: 'Pleine propriété',
  USUFRUIT: 'Usufruit',
  NUE_PROPRIETE: 'Nue-propriété',
};

export interface DevolutionLegaleRequest {
  conjointSurvivant: boolean;
  nbDescendants: number;
  tousDescendantsCommunsAvecConjoint: boolean | null;
  nbDescendantsPredecedes: number;
  nbPetitsEnfantsParRepresentation: number;
  pereVivant: boolean;
  mereVivant: boolean;
  nbFreresSoeurs: number;
  nbFreresSoeursPredecedes: number;
  ascendantsOrdinaires: boolean;
  collateralOrdinaires: boolean;
  optionConjoint: OptionConjoint | null;
}

export interface DevolutionLegaleResponse {
  caseFileId: string;
  ordreActif: OrdreActif;
  heritiersDesignes: HeritierDesigne[];
  quotePartConjoint: number;
  modaliteConjoint: ModaliteHeritier | null;
  representationActive: boolean;
  fenteApplicable: boolean;
  scoreEligibilite: number;
  risquesContentieux: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: string;
}
