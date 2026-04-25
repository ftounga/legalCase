/**
 * SF-FA-25-02 : modèles TypeScript pour l'outil décisionnel
 * "Majeurs protégés — choix du régime" (F-FA-25, FRANCE uniquement,
 * art. 425-494 Cciv + art. 494-1 Cciv habilitation familiale).
 *
 * Contrat importé du backend SF-FA-25-01 (parallèle).
 */

export type RegimeProtection =
  | 'SAUVEGARDE_JUSTICE'
  | 'HABILITATION_FAMILIALE'
  | 'CURATELLE_SIMPLE'
  | 'CURATELLE_RENFORCEE'
  | 'TUTELLE'
  | 'MANDAT_PROTECTION_FUTURE';

export type DemandeurFamilial =
  | 'CONJOINT'
  | 'ENFANT_MAJEUR'
  | 'PARENT'
  | 'FRERE_SOEUR'
  | 'TIERS_PROCHE'
  | 'MINISTERE_PUBLIC';

export type ActeEnvisage =
  | 'GESTION_PATRIMOINE'
  | 'DECISIONS_LOGEMENT'
  | 'DECISIONS_SANTE'
  | 'DECISIONS_FAMILIALES'
  | 'ACTES_ETAT_CIVIL'
  | 'AUTRE';

export type VerdictMajeurs = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface MajeursProtegesRequest {
  regimeProtectionDemande: RegimeProtection;
  altertationFacultesMentales: boolean;
  altertationFacultesPhysiques: boolean;
  certificatMedicalCirconstancie: boolean;
  /** ISO date YYYY-MM-DD. */
  dateCertificatMedical: string;
  consentementPersonneAProteger: boolean;
  demandeurFamilial: DemandeurFamilial;
  actesEnvisages: ActeEnvisage[];
  urgencePatrimoniale: boolean;
  patrimoineSignificatif: boolean;
  isolementSocial: boolean;
}

export interface MajeursProtegesResponse {
  caseFileId: string;
  // Inputs persistés (echoed pour reprise du form en mode édition).
  regimeProtectionDemande: RegimeProtection;
  altertationFacultesMentales: boolean;
  altertationFacultesPhysiques: boolean;
  certificatMedicalCirconstancie: boolean;
  dateCertificatMedical: string;
  consentementPersonneAProteger: boolean;
  demandeurFamilial: DemandeurFamilial;
  actesEnvisages: ActeEnvisage[];
  urgencePatrimoniale: boolean;
  patrimoineSignificatif: boolean;
  isolementSocial: boolean;
  // Sortie décisionnelle.
  scoreEligibilite: number;
  /** Peut différer de `regimeProtectionDemande` (point différenciant). */
  regimeOptimalRecommande: RegimeProtection;
  verdictAcceptabiliteJaf: VerdictMajeurs;
  delaiProcedureMoisPrevisionnel: number;
  auditionPersonneObligatoire: boolean;
  expertisePsyComplementaireRecommandee: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}

// ---------------------------------------------------------------------------
// Options affichables (mat-select)
// ---------------------------------------------------------------------------

export interface RegimeProtectionOption {
  code: RegimeProtection;
  label: string;
}

export const REGIMES_PROTECTION_FR: RegimeProtectionOption[] = [
  { code: 'SAUVEGARDE_JUSTICE', label: 'Sauvegarde de justice (art. 433 Cciv)' },
  { code: 'HABILITATION_FAMILIALE', label: 'Habilitation familiale (art. 494-1 Cciv)' },
  { code: 'CURATELLE_SIMPLE', label: 'Curatelle simple (art. 440 Cciv)' },
  { code: 'CURATELLE_RENFORCEE', label: 'Curatelle renforcée (art. 472 Cciv)' },
  { code: 'TUTELLE', label: 'Tutelle (art. 440 al. 3 Cciv)' },
  { code: 'MANDAT_PROTECTION_FUTURE', label: 'Mandat de protection future (art. 477 Cciv)' },
];

export interface DemandeurFamilialOption {
  code: DemandeurFamilial;
  label: string;
}

export const DEMANDEURS_FAMILIAUX_FR: DemandeurFamilialOption[] = [
  { code: 'CONJOINT', label: 'Conjoint / partenaire de PACS' },
  { code: 'ENFANT_MAJEUR', label: 'Enfant majeur' },
  { code: 'PARENT', label: 'Parent (ascendant)' },
  { code: 'FRERE_SOEUR', label: 'Frère / sœur' },
  { code: 'TIERS_PROCHE', label: 'Tiers proche (relation étroite et stable)' },
  { code: 'MINISTERE_PUBLIC', label: 'Ministère public (procureur)' },
];

export interface ActeEnvisageOption {
  code: ActeEnvisage;
  label: string;
}

export const ACTES_ENVISAGES_FR: ActeEnvisageOption[] = [
  { code: 'GESTION_PATRIMOINE', label: 'Gestion du patrimoine' },
  { code: 'DECISIONS_LOGEMENT', label: 'Décisions relatives au logement' },
  { code: 'DECISIONS_SANTE', label: 'Décisions relatives à la santé' },
  { code: 'DECISIONS_FAMILIALES', label: 'Décisions familiales (mariage, partage)' },
  { code: 'ACTES_ETAT_CIVIL', label: 'Actes d\'état civil' },
  { code: 'AUTRE', label: 'Autre' },
];

// ---------------------------------------------------------------------------
// Helpers d'affichage
// ---------------------------------------------------------------------------

export function regimeProtectionLabel(code: RegimeProtection): string {
  return REGIMES_PROTECTION_FR.find((r) => r.code === code)?.label ?? code;
}

export function demandeurFamilialLabel(code: DemandeurFamilial): string {
  return DEMANDEURS_FAMILIAUX_FR.find((d) => d.code === code)?.label ?? code;
}

export function acteEnvisageLabel(code: ActeEnvisage): string {
  return ACTES_ENVISAGES_FR.find((a) => a.code === code)?.label ?? code;
}

export function verdictMajeursLabel(v: VerdictMajeurs): string {
  switch (v) {
    case 'ELEVEE': return 'Acceptabilité élevée';
    case 'MOYENNE': return 'Acceptabilité moyenne';
    case 'FAIBLE': return 'Acceptabilité faible';
  }
}
