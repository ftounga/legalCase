/**
 * SF-214-12 : modèles miroirs du contrat API (backend SF-214-11) pour l'outil
 * décisionnel « AES — calcul de présence prouvée » (F-IM-30).
 * FR uniquement (admission exceptionnelle au séjour — circulaire Valls L.435-1).
 *
 * Saisie dynamique de périodes de présence justifiées par pièce, calcul du
 * total d'années prouvées et de l'éligibilité aux 4 voies AES.
 */

export type TypePiece =
  | 'RIB_BANQUE'
  | 'FACTURE_EDF_GAZ'
  | 'QUITTANCE_LOYER'
  | 'BULLETIN_SALAIRE'
  | 'AVIS_IMPOSITION'
  | 'SCOLARITE_ENFANT'
  | 'ATTESTATION_EMPLOYEUR'
  | 'TITRE_SEJOUR'
  | 'AUTRE';

export interface PeriodePresentee {
  /** Date de début (ISO YYYY-MM-DD). */
  debut: string;
  /** Date de fin (ISO YYYY-MM-DD). */
  fin: string;
  /** Type de pièce justificative. */
  typePiece: TypePiece;
}

export interface AesPresenceProuveeRequest {
  periodesPresentees: PeriodePresentee[];
}

/** Éligibilité par voie AES (booléens). */
export interface EligibiliteParVoie {
  aes_famille: boolean;
  aes_humanitaire: boolean;
  aes_etudiant: boolean;
  aes_metiers_tension: boolean;
}

export interface AesPresenceProuveeResponse {
  caseFileId: string;
  country: string;
  periodesPresentees: PeriodePresentee[];
  anneesTotalesProuvees: number;
  eligibiliteParVoie: EligibiliteParVoie;
  gapsPeriodes: string[];
  recommandationsPieces: string[];
}
