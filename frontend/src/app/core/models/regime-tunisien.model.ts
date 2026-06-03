/**
 * SF-220-01 : modèles miroirs du contrat API (backend SF-220-01) pour l'outil
 * décisionnel "Régime franco-tunisien (accord du 17/03/1988)"
 * (F-IM-47-regime-tunisien-fr). FR uniquement.
 *
 * Aiguillage : droit commun CESEDA SAUF particularités dérogatoires de l'accord
 * franco-tunisien (étudiant, commerçant, salarié). Distinct de F-IM-17 (régime
 * algérien fermé) — la Tunisie reste largement renvoyée au CESEDA.
 */

export type CategorieRegimeTunisien =
  | 'ETUDIANT'
  | 'COMMERCANT'
  | 'SALARIE'
  | 'FAMILIAL'
  | 'AUTRE';

export type RegimeTunisien =
  | 'ACCORD_1988_DEROGATOIRE'
  | 'DROIT_COMMUN_CESEDA'
  | 'MIXTE';

export interface RegimeTunisienRequest {
  categorie: CategorieRegimeTunisien;
  dureeSejourEnvisageeMois: number | null;
  titreEnCours: boolean;
  dejaResident: boolean;
}

export interface RegimeTunisienResponse {
  caseFileId: string;
  categorie: CategorieRegimeTunisien;
  dureeSejourEnvisageeMois: number | null;
  titreEnCours: boolean;
  dejaResident: boolean;
  country: string;
  regime: RegimeTunisien;
  particularitesApplicables: string[];
  basesJuridiques: string[];
  renvoiDroitCommun: boolean;
  messages: string[];
}
