/**
 * SF-214-34 : modèles miroirs du contrat API (backend SF-214-33) pour l'outil
 * décisionnel "Appel CAA / cassation CE" (F-IM-41-appel-caa-cassation-ce-fr).
 * FRANCE uniquement — délais d'appel devant la cour administrative d'appel (CAA)
 * contre un jugement de TA en contentieux des étrangers, et pourvoi en cassation
 * devant le Conseil d'État (filtre des pourvois L.821-2 CJA pour l'OQTF).
 *
 * Calculateur de délai : statut de l'appel + date d'échéance de l'appel CAA +
 * jours restants + cour d'appel compétente + motifs d'appel possibles + filtre
 * des pourvois en cassation (CE) + délai du pourvoi en cassation (mois).
 */

export type StatutAppelCaaCassation =
  | 'APPEL_POSSIBLE'
  | 'URGENT'
  | 'PRESCRIT';

export type TypeDecisionTaAppel = 'REJET' | 'ANNULATION';

export type TypeContentieuxAppel =
  | 'OQTF'
  | 'REFUS_TITRE'
  | 'EXPULSION'
  | 'AUTRE';

export interface AppelCaaCassationRequest {
  dateJugementTA: string; // YYYY-MM-DD — requis
  typeDecisionTA: TypeDecisionTaAppel; // REJET | ANNULATION
  typeContentieux: TypeContentieuxAppel; // OQTF | REFUS_TITRE | EXPULSION | AUTRE
  delaiSpecialOQTF: boolean; // délai spécial OQTF (15 j) ?
}

export interface AppelCaaCassationResponse {
  caseFileId: string;
  dateJugementTA: string;
  typeDecisionTA: TypeDecisionTaAppel;
  typeContentieux: TypeContentieuxAppel;
  delaiSpecialOQTF: boolean;
  country: string;
  statut: StatutAppelCaaCassation;
  dateEcheanceAppelCaa: string; // YYYY-MM-DD
  joursRestants: number;
  courAppelCompetente: string; // CAA compétente
  motifsAppelPossibles: string[];
  filtrePourvoisCassation: boolean; // pourvoi CE filtré (L.821-2 CJA) ?
  delaiCassationCeMois: number; // délai du pourvoi en cassation (mois)
}
