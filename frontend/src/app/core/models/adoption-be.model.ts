/**
 * SF-223-02 : modèles TypeScript de l'outil décisionnel "Recevabilité de
 * l'adoption en Belgique" (`adoption-be`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-02 (endpoints
 * POST/GET — loi du 24/04/2003 réformant l'adoption ; CC art. 343-1 et s. — à
 * vérifier par avocat belge, renumérotation CC post-réformes 2017-2019). ≠
 * adoption française. L'adoption internationale (Convention de La Haye) est
 * différée (P4 F-224).
 */

/** Type d'adoption analysé (branche d'adoptant / d'effet — outil multi-vues unique). */
export type TypeAdoptionBe = 'PLENIERE' | 'SIMPLE' | 'CO_PARENTALE';

/** Verdict de l'analyse de recevabilité (4 niveaux). */
export type AdoptionBeVerdict =
  | 'RECEVABLE'
  | 'RECEVABLE_SOUS_CONDITIONS'
  | 'IRRECEVABLE'
  | 'QUALIFICATION_INCOMPLETE';

/** État du consentement des parents biologiques d'origine. */
export type ConsentementParentsBiologiques = 'OBTENU' | 'REFUSE' | 'SANS_OBJET';

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/adoption-be-analysis`.
 *
 * `typeAdoption` et `consentementParentsBiologiques` sont requis (validation
 * côté backend → 400). `ecartAgeAnnees` nullable (dérivé de la différence des
 * âges s'il n'est pas saisi). `adoptantEnCoupleAvecParentBiologique` /
 * `lienCohabitationLegaleOuMariage` pertinents surtout pour CO_PARENTALE.
 */
export interface AdoptionBeRequest {
  typeAdoption: TypeAdoptionBe;
  ageAdoptant: number | null;
  ageAdopte: number | null;
  ecartAgeAnnees: number | null;
  adoptantEnCoupleAvecParentBiologique: boolean | null;
  lienCohabitationLegaleOuMariage: boolean | null;
  agrementObtenu: boolean | null;
  consentementAdopteOuRepresentants: boolean | null;
  consentementParentsBiologiques: ConsentementParentsBiologiques;
  commentaire: string | null;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, motifs/conditions, actes à
 * produire, bases juridiques, messages).
 */
export interface AdoptionBeResponse extends AdoptionBeRequest {
  caseFileId: string;
  verdict: AdoptionBeVerdict;
  motifsConditions: string[];
  actesAProduire: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
