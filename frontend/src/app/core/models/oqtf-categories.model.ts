/**
 * SF-214-10 : modèles miroirs du contrat API (backend SF-214-09) pour l'outil
 * décisionnel « OQTF catégories L. 611-1 » (F-IM-29). FR uniquement.
 *
 * 7 catégories CESEDA L. 611-1 (1° à 7°) : pour chaque catégorie, moyens de
 * défense spécifiques + base juridique + délai de recours + éventuelle procédure
 * parallèle (renvoi F-IM-22 Dublin si CAT_7).
 */

export type CategorieL611 =
  | 'CAT_1'
  | 'CAT_2'
  | 'CAT_3'
  | 'CAT_4'
  | 'CAT_5'
  | 'CAT_6'
  | 'CAT_7';

export interface OqtfCategoriesRequest {
  categorieL611: CategorieL611;
  dateNotificationOqtf: string; // ISO yyyy-MM-dd
  motifOqtf?: string | null; // ≤ 300 caractères, optionnel
}

export interface OqtfCategoriesResponse {
  caseFileId: string;
  categorieL611: CategorieL611;
  dateNotificationOqtf: string;
  motifOqtf: string | null;
  country: string;
  categorieLibelle: string;
  moyensDefense: string[];
  baseJuridique: string;
  delaiRecours: string;
  procedureParallele: string | null; // lien vers F-IM-22 si CAT_7
}
