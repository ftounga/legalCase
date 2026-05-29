/**
 * SF-214-44 : modèles miroirs du contrat API (backend SF-214-43) pour l'outil
 * décisionnel "Autorisation travail employeur"
 * (F-IM-46-autorisation-travail-employeur-fr). FRANCE uniquement — obligations de
 * l'employeur qui souhaite embaucher un ressortissant étranger (procédure de
 * demande d'autorisation de travail auprès de l'OFII / plateforme dédiée).
 *
 * Outil côté employeur, complémentaire à F-IM-07 (côté salarié) :
 *  - statut : autorisation requise / non requise (UE-EEE-Suisse, dispenses) /
 *    recours possible (en cas de refus) / recours prescrit ;
 *  - obligations de la demande (checklist procédure employeur) ;
 *  - délai d'instruction OFII + taxe OFII ;
 *  - délai du recours devant le TA si refus d'autorisation.
 */

export type StatutAutorisationTravailEmployeur =
  | 'AUTORISATION_REQUISE'
  | 'AUTORISATION_NON_REQUISE'
  | 'RECOURS_POSSIBLE'
  | 'RECOURS_PRESCRIT';

export type TypeContratAutorisationTravail = 'CDI' | 'CDD' | 'INTERIM';

export interface AutorisationTravailEmployeurRequest {
  typeContrat: TypeContratAutorisationTravail; // CDI | CDD | INTERIM
  posteProposes: string; // intitulé du poste proposé (≤ 200)
  nationaliteCandidat: string; // nationalité du candidat (texte libre)
  dureeContratMois?: number | null; // durée du contrat en mois (optionnel)
  refusAutorisation: boolean; // l'autorisation a-t-elle été refusée ?
  dateRefusAutorisation?: string | null; // YYYY-MM-DD — optionnel
}

export interface AutorisationTravailEmployeurResponse {
  caseFileId: string;
  typeContrat: TypeContratAutorisationTravail;
  posteProposes: string;
  nationaliteCandidat: string;
  dureeContratMois?: number | null;
  refusAutorisation: boolean;
  dateRefusAutorisation?: string | null;
  country: string;
  statut: StatutAutorisationTravailEmployeur;
  obligationsDemande: string[]; // checklist des obligations de la demande
  delaiInstructionOFII: string; // délai d'instruction OFII (ex. "2 mois")
  recoursPossible: boolean; // un recours est-il encore ouvert ?
  delaiRecoursTa?: string | null; // YYYY-MM-DD — date limite du recours TA
  taxeOFII: string; // taxe OFII applicable (ex. "55 % du SMIC mensuel")
}
