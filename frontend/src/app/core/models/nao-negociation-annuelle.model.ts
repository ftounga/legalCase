/**
 * SF-218-30 : modèles miroirs du contrat API (backend SF-218-29) pour l'outil
 * décisionnel « NAO : négociation annuelle obligatoire »
 * (F-DT-66-nao-negociation-annuelle). FRANCE uniquement.
 *
 * Outil de conformité EMPLOYEUR + calculateur de délai de la négociation
 * annuelle obligatoire dans les entreprises pourvues d'un délégué syndical
 * (art. L.2242-1 à L.2242-8 CT) : déclenchement de la négociation sur les blocs
 * obligatoires (rémunération / temps de travail / partage de la valeur ;
 * égalité professionnelle F/H et QVT), respect de la périodicité (annuelle, ou
 * jusqu'à 4 ans par accord de méthode L.2242-11), établissement d'un PV de
 * désaccord, exposition aux sanctions (pénalité égalité F/H, délit d'entrave
 * L.2243-2). Distinct de F-DT-67 (validité de l'accord issu de la NAO).
 */

/**
 * Verdict de conformité de la NAO (aligné EXACTEMENT sur l'enum backend
 * {@code StatutNao}) :
 *  - CONFORME : applicable + tous items obligatoires conformes + échéance non
 *    dépassée.
 *  - NON_CONFORME : applicable + au moins un item obligatoire non conforme, ou
 *    échéance dépassée.
 *  - NON_APPLICABLE : pas de délégué syndical → pas d'obligation de NAO.
 */
export type StatutNao = 'CONFORME' | 'NON_CONFORME' | 'NON_APPLICABLE';

/**
 * Statut d'échéance de la prochaine négociation (aligné EXACTEMENT sur l'enum
 * backend {@code StatutEcheanceNao}) :
 *  - A_JOUR : échéance à plus de 60 jours.
 *  - ECHEANCE_PROCHE : 0 ≤ jours avant échéance ≤ 60.
 *  - DEPASSEE : échéance dépassée (jours < 0).
 */
export type StatutEcheanceNao = 'A_JOUR' | 'ECHEANCE_PROCHE' | 'DEPASSEE';

/**
 * Niveau de risque d'entrave / sanction (aligné EXACTEMENT sur l'enum backend
 * {@code RisqueEntrave}) :
 *  - FAIBLE : CONFORME.
 *  - MODERE : NON_CONFORME (hors blocs non engagés).
 *  - ELEVE : blocs non négociés alors que DS présent (délit d'entrave L.2243-2,
 *    pénalité égalité F/H jusqu'à 1 % de la masse salariale).
 */
export type RisqueEntrave = 'FAIBLE' | 'MODERE' | 'ELEVE';

/**
 * Détail d'un item de la checklist de conformité NAO (aligné EXACTEMENT sur le
 * DTO backend {@code ChecklistItemNao}).
 */
export interface ChecklistItemNao {
  /** Libellé de l'item (obligation légale). */
  item: string;
  /** Item conforme ou non. */
  conforme: boolean;
  /** Item obligatoire (vs. recommandé) dans le contexte du dossier. */
  obligatoire: boolean;
  /** Commentaire explicatif (base légale / preuve). */
  commentaire: string;
}

export interface NaoNegociationAnnuelleRequest {
  /** Effectif de l'entreprise (NAO obligatoire dès qu'un DS est désigné). */
  effectif: number;
  /** Présence d'au moins un délégué syndical (déclencheur de l'obligation). */
  delegueSyndicalPresent: boolean;
  /** Bloc 1 négocié : rémunération, temps de travail, partage de la valeur (L.2242-15). */
  blocRemunerationNegocie: boolean;
  /** Bloc 2 négocié : égalité professionnelle F/H et QVT (L.2242-17). */
  blocEgaliteQvtNegocie: boolean;
  /** Accord de méthode portant la périodicité au-delà de l'année (max 4 ans, L.2242-11). */
  accordMethodePeriodicite: boolean;
  /** Date de la dernière négociation engagée (ISO YYYY-MM-DD, optionnel). */
  dateDerniereNegociation?: string | null;
  /** Périodicité retenue en mois (12 par défaut, 13–48 si accord de méthode). */
  periodiciteMois: number;
  /** PV de désaccord établi en cas d'échec. */
  pvDesaccordEtabli: boolean;
  /** La négociation a abouti à un accord. */
  negociationAboutie: boolean;
}

export interface NaoNegociationAnnuelleResponse {
  caseFileId: string;
  effectif: number;
  delegueSyndicalPresent: boolean;
  blocRemunerationNegocie: boolean;
  blocEgaliteQvtNegocie: boolean;
  accordMethodePeriodicite: boolean;
  dateDerniereNegociation?: string | null;
  periodiciteMois: number;
  pvDesaccordEtabli: boolean;
  negociationAboutie: boolean;
  /** Checklist de conformité (item + conforme + obligatoire + commentaire). */
  checklist: ChecklistItemNao[];
  /** Nombre d'items obligatoires non conformes. */
  itemsObligatoiresManquants: number;
  /** Verdict de conformité. */
  statut: StatutNao;
  /** Date de la prochaine échéance de négociation (ISO YYYY-MM-DD, si date dernière NAO). */
  dateProchaineEcheance?: string | null;
  /** Jours avant l'échéance (peut être négatif si dépassée). */
  joursAvantEcheance?: number | null;
  /** Statut d'échéance de la prochaine négociation. */
  statutEcheance?: StatutEcheanceNao | null;
  /** Niveau de risque d'entrave / sanction. */
  risqueEntrave: RisqueEntrave;
  country: string;
  baseJuridique: string;
}
