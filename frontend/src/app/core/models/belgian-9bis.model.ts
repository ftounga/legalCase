/**
 * SF-IM-14-05 : modèle TypeScript de l'outil décisionnel "9bis humanitaire BE"
 * (Loi 15/12/1980 art. 9bis + AR 17/05/2007). Contrat figé dans SF-IM-14-01
 * (backend Belgian9bisCalculator/Controller).
 */

export type Verdict9bis = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';

export interface Belgian9bisRequest {
  /** YYYY-MM-DD — date d'entrée sur le territoire belge. */
  dateEntreeBelgique: string;
  /** Durée de présence en Belgique en mois (entier ≥ 0). */
  dureePresenceMois: number;
  /** Circonstances exceptionnelles empêchant le retour au pays d'origine. */
  circonstancesExceptionnelles: boolean;
  /** Liens familiaux constitués en Belgique. */
  liensFamiliauxBe: boolean;
  /** Liens professionnels (contrat, emploi). */
  liensProfessionnels: boolean;
  /** Enfants scolarisés en Belgique. */
  scolariteEnfantsBe: boolean;
  /** Menace réelle pour l'ordre public. */
  menaceOrdrePublic: boolean;
  /** YYYY-MM-DD — date de dépôt de la demande (optionnelle). */
  dateDepotDemande?: string | null;
}

export interface Belgian9bisResponse {
  caseFileId: string;
  dateEntreeBelgique: string;
  dureePresenceMois: number;
  circonstancesExceptionnelles: boolean;
  liensFamiliauxBe: boolean;
  liensProfessionnels: boolean;
  scolariteEnfantsBe: boolean;
  menaceOrdrePublic: boolean;
  dateDepotDemande: string | null;
  country: 'BELGIQUE';
  presence3AnsOk: boolean;
  liensConstitutifsOk: boolean;
  pasMenace: boolean;
  /** Score global 0-100 (présence +25, liens +40, pas menace +35). */
  scoreGlobal: number;
  /** ELEVEE (≥75) / MOYENNE (40-74) / FAIBLE (<40). */
  verdictProbabilite: Verdict9bis;
  criteresNonRemplis: string[];
  dateExpirationInstructionPrevisionnelle: string | null;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
