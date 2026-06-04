/**
 * SF-223-09 : modèles TypeScript de l'outil décisionnel "Modification de l'état
 * civil (Belgique)" (`etat-civil-be-modification`).
 *
 * BELGIQUE uniquement. Contrat API figé côté backend SF-223-09 (endpoints
 * POST/GET). Qualifie une modification de l'état civil : changement de nom
 * (SPF Justice) / de prénom (officier de l'état civil) — loi 18/06/2018 ; de
 * sexe (auto-déclaration administrative — loi 25/06/2017 — à vérifier par avocat
 * belge). 1 outil = 1 situation (modification de l'état civil). DISTINCT de la
 * rectification d'état civil (erreur matérielle d'acte — P4 différé F-224) et du
 * changement d'état civil FR. BE-only pur.
 */

/** Type de modification de l'état civil sollicitée. */
export type TypeModificationEtatCivil =
  | 'CHANGEMENT_NOM'
  | 'CHANGEMENT_PRENOM'
  | 'CHANGEMENT_SEXE';

/** Verdict de l'analyse (4 niveaux). */
export type EtatCivilBeModificationVerdict =
  | 'MODIFICATION_RECEVABLE'
  | 'MODIFICATION_RECEVABLE_SOUS_CONDITIONS'
  | 'MODIFICATION_IRRECEVABLE'
  | 'QUALIFICATION_INCOMPLETE';

/**
 * Requête POST
 * `/api/v1/case-files/{caseFileId}/etat-civil-be-modification-analysis`.
 *
 * `typeModification` est requis (validation côté backend → 400).
 * `personneMajeure` et `nationaliteBelgeOuResident` sont des booleans. Les
 * booleans de fond propres à chaque branche sont nullables :
 *  - `motifLegitime` (branche NOM) ;
 *  - `secondeDemandePrenom` (branche PRÉNOM — gratuité / tarif réduit de la 1re
 *    demande) ;
 *  - `declarationSexeReiteree` (branche SEXE — seconde déclaration confirmative) ;
 *  - `consentementRepresentantsSiMineur` (mineur).
 */
export interface EtatCivilBeModificationRequest {
  typeModification: TypeModificationEtatCivil;
  personneMajeure: boolean;
  nationaliteBelgeOuResident: boolean;
  motifLegitime: boolean | null;
  secondeDemandePrenom: boolean | null;
  declarationSexeReiteree: boolean | null;
  consentementRepresentantsSiMineur: boolean | null;
}

/**
 * Réponse POST / GET. Ré-expose le snapshot des inputs (ré-édition du
 * formulaire) + les champs calculés (verdict, autorité compétente, motifs,
 * conseils, démarches, bases juridiques, messages).
 */
export interface EtatCivilBeModificationResponse
  extends EtatCivilBeModificationRequest {
  caseFileId: string;
  verdict: EtatCivilBeModificationVerdict;
  autoriteCompetente: string;
  motifs: string[];
  conseils: string[];
  demarches: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
