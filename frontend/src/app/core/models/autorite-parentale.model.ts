/**
 * SF-FA-19-02 : modèles TypeScript pour l'outil décisionnel
 * "Autorité parentale — exercice" (F-FA-19, FRANCE uniquement,
 * art. 372-373 / 373-2-10 Cciv).
 *
 * Contrat importé du backend SF-FA-19-01.
 */

export type RegimeExercice =
  | 'CONJOINT'
  | 'EXCLUSIF_MERE'
  | 'EXCLUSIF_PERE'
  | 'DELEGATION_TIERS';

export type MotifChangementAutorite =
  | 'MISE_EN_DANGER'
  | 'DESINTERET_PROLONGE'
  | 'IMPOSSIBILITE_FAIT'
  | 'CONDAMNATION_PENALE'
  | 'ALIENATION_PARENTALE'
  | 'AUTRE';

export type PreuveAutorite =
  | 'JUGEMENT_PENAL'
  | 'MAINS_COURANTES'
  | 'CERTIFICAT_MEDICAL'
  | 'TEMOIGNAGES_ECOLE'
  | 'TEMOIGNAGES_PROCHES'
  | 'RAPPORT_AED'
  | 'EXPERTISE_PSYCHIATRIQUE'
  | 'AUTRE';

export type AutoriteParentaleVerdict = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface AutoriteParentaleRequest {
  regimeExerciceActuel: RegimeExercice;
  regimeExerciceDemande: RegimeExercice;
  motifChangement: MotifChangementAutorite;
  dangerCaracterise: boolean;
  preuvesProduites: PreuveAutorite[];
  ageEnfants: number[];
  consentementAutreParent: boolean;
  interferenceVieEnfant: boolean;
  /** ISO date YYYY-MM-DD. */
  dateRequete: string;
}

export interface AutoriteParentaleResponse {
  caseFileId: string;
  regimeExerciceActuel: RegimeExercice;
  regimeExerciceDemande: RegimeExercice;
  motifChangement: MotifChangementAutorite;
  dangerCaracterise: boolean;
  preuvesProduites: PreuveAutorite[];
  ageEnfants: number[];
  consentementAutreParent: boolean;
  interferenceVieEnfant: boolean;
  dateRequete: string;
  scoreEligibilite: number;
  verdictProbabiliteAcceptation: AutoriteParentaleVerdict;
  expertiseRecommandee: boolean;
  auditionEnfantsRecommandee: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

export interface RegimeExerciceOption {
  code: RegimeExercice;
  label: string;
}

export const REGIMES_EXERCICE_FR: RegimeExerciceOption[] = [
  { code: 'CONJOINT', label: 'Conjoint (parents en commun)' },
  { code: 'EXCLUSIF_MERE', label: 'Exclusif — mère' },
  { code: 'EXCLUSIF_PERE', label: 'Exclusif — père' },
  { code: 'DELEGATION_TIERS', label: 'Délégation à un tiers (art. 377 Cciv)' },
];

export interface MotifChangementOption {
  code: MotifChangementAutorite;
  label: string;
}

export const MOTIFS_CHANGEMENT_FR: MotifChangementOption[] = [
  { code: 'MISE_EN_DANGER', label: 'Mise en danger de l\'enfant' },
  { code: 'DESINTERET_PROLONGE', label: 'Désintérêt prolongé' },
  { code: 'IMPOSSIBILITE_FAIT', label: 'Impossibilité de fait' },
  { code: 'CONDAMNATION_PENALE', label: 'Condamnation pénale' },
  { code: 'ALIENATION_PARENTALE', label: 'Aliénation parentale' },
  { code: 'AUTRE', label: 'Autre' },
];

export interface PreuveAutoriteOption {
  code: PreuveAutorite;
  label: string;
}

export const PREUVES_AUTORITE_FR: PreuveAutoriteOption[] = [
  { code: 'JUGEMENT_PENAL', label: 'Jugement pénal' },
  { code: 'MAINS_COURANTES', label: 'Mains courantes' },
  { code: 'CERTIFICAT_MEDICAL', label: 'Certificat médical' },
  { code: 'TEMOIGNAGES_ECOLE', label: 'Témoignages école' },
  { code: 'TEMOIGNAGES_PROCHES', label: 'Témoignages proches' },
  { code: 'RAPPORT_AED', label: 'Rapport AED / ASE' },
  { code: 'EXPERTISE_PSYCHIATRIQUE', label: 'Expertise psychiatrique' },
  { code: 'AUTRE', label: 'Autre' },
];

export function regimeExerciceLabel(code: RegimeExercice): string {
  return REGIMES_EXERCICE_FR.find((r) => r.code === code)?.label ?? code;
}

export function motifChangementLabel(code: MotifChangementAutorite): string {
  return MOTIFS_CHANGEMENT_FR.find((m) => m.code === code)?.label ?? code;
}

export function preuveAutoriteLabel(code: PreuveAutorite): string {
  return PREUVES_AUTORITE_FR.find((p) => p.code === code)?.label ?? code;
}

export function verdictAutoriteParentaleLabel(v: AutoriteParentaleVerdict): string {
  switch (v) {
    case 'ELEVEE': return 'Probabilité élevée';
    case 'MOYENNE': return 'Probabilité moyenne';
    case 'FAIBLE': return 'Probabilité faible';
  }
}
