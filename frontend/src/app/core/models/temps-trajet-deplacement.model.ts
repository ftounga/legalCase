/**
 * SF-218-52 : modèles miroirs du contrat API (backend SF-218-51) pour l'outil
 * décisionnel « Temps de trajet / déplacement professionnel »
 * (F-DT-81-temps-trajet-deplacement). FRANCE uniquement.
 *
 * Qualifie le temps de trajet professionnel (temps de travail effectif ou non)
 * et détermine si une contrepartie (repos / financière) est due (art. L.3121-4
 * CT ; CJUE 10/09/2015 C-266/14 « Tyco ») : le trajet domicile ↔ lieu habituel
 * de travail n'est pas du temps de travail effectif ; s'il dépasse le temps
 * normal de trajet, il ouvre droit à une contrepartie, sauf si une contrepartie
 * est déjà prévue par accord. Pour un salarié itinérant sans lieu de travail
 * fixe, le déplacement domicile ↔ premier/dernier client est qualifié de temps
 * de travail effectif.
 *
 * DISTINCT du remboursement de frais de déplacement et de l'astreinte —
 * invariant « un outil = une situation ».
 */

/** Type de trajet (aligné sur l'enum backend {@code TypeTrajet}). */
export type TypeTrajet =
  | 'DOMICILE_TRAVAIL_HABITUEL'
  | 'DOMICILE_CLIENT_DEPASSEMENT'
  | 'ITINERANT_SANS_LIEU_FIXE';

/**
 * Verdict de qualification (aligné sur l'enum backend
 * {@code TempsTrajetQualification}).
 *  - TEMPS_TRAVAIL : déplacement qualifié de temps de travail effectif.
 *  - TRAJET_AVEC_CONTREPARTIE : trajet dépassant le temps normal → contrepartie.
 *  - TRAJET_SANS_CONTREPARTIE : pas de dépassement → pas de contrepartie.
 */
export type TempsTrajetQualification =
  | 'TEMPS_TRAVAIL'
  | 'TRAJET_AVEC_CONTREPARTIE'
  | 'TRAJET_SANS_CONTREPARTIE';

export interface TempsTrajetDeplacementRequest {
  /** Type de trajet (requis). */
  typeTrajet: TypeTrajet;
  /** Temps de trajet quotidien constaté en minutes (requis, ≥ 0). */
  tempsTrajetQuotidienMinutes: number;
  /** Temps de trajet « normal » de référence en minutes (requis, ≥ 0). */
  tempsTrajetNormalMinutes: number;
  /** Une contrepartie (repos / financière) est déjà prévue par accord (défaut false). */
  contrepartiePrevueAccord?: boolean | null;
}

export interface TempsTrajetDeplacementResponse {
  caseFileId: string;
  /** Verdict de qualification du temps de trajet. */
  qualification: TempsTrajetQualification;
  typeTrajet: TypeTrajet;
  tempsTrajetQuotidienMinutes: number;
  tempsTrajetNormalMinutes: number;
  contrepartiePrevueAccord: boolean;
  /** Une contrepartie (repos / financière) est due. */
  contrepartieDue: boolean;
  /** Part du trajet excédant le temps normal (minutes). */
  depassementMinutes: number;
  /** Description de la base d'analyse. */
  base: string;
  /** Notes / points de vigilance identifiés. */
  notes: string[];
  country: string;
  baseJuridique: string;
}
