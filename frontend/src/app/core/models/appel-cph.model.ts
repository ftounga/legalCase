/**
 * SF-218-02 : modèles TypeScript de l'outil "Appel CPH devant la Cour d'appel"
 * (F-DT-86-appel-cph-cour-appel, FRANCE uniquement — R. 1461-1 et s. CPC ;
 * art. 538 CPC — délai d'appel 1 mois ; art. 901 CPC — déclaration d'appel et
 * chefs de jugement critiqués ; art. 946 CPC — procédure orale en appel social ;
 * R. 1461-2 CPC — représentation obligatoire avocat / défenseur syndical ;
 * R. 1462-1 CPC — taux de compétence en dernier ressort).
 *
 * <p>F-218a — Procédure CPH avancée (P3 Travail FR).</p>
 *
 * <p>Contrat API importé de SF-218-01 (backend, endpoints POST/GET figés
 * sur `/api/v1/case-files/{caseFileId}/appel-cph-analysis`).</p>
 */

/** Partie qui interjette appel du jugement CPH. */
export type PartieAppelante = 'SALARIE' | 'EMPLOYEUR';

/** Mode de notification du jugement CPH (point de départ du délai d'appel). */
export type ModeNotification = 'SIGNIFICATION' | 'LRAR';

/** Représentation constituée pour l'appel social (R. 1461-2 CPC). */
export type RepresentationConstituee = 'AVOCAT' | 'DEFENSEUR_SYNDICAL' | 'AUCUNE';

/**
 * Verdict de recevabilité de l'appel.
 *
 *  - `DELAI_OUVERT` — délai > 7 j (vert)
 *  - `DELAI_URGENT` — délai 1–7 j (or)
 *  - `DELAI_EXPIRE` — délai dépassé (rouge)
 *  - `VOIE_FERMEE` — jugement en dernier ressort, appel impossible → pourvoi
 *    en cassation F-DT-87 (navy)
 */
export type AppelCphStatut =
  | 'DELAI_OUVERT'
  | 'DELAI_URGENT'
  | 'DELAI_EXPIRE'
  | 'VOIE_FERMEE';

/** Item de la checklist des formalités d'appel social. */
export interface AppelCphChecklistItem {
  libelle: string;
  obligatoire: boolean;
  /** Marque un item bloquant mis en avant (ex. représentation absente). */
  bloquant?: boolean;
  baseJuridique: string;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/appel-cph-analysis`.
 *
 * 5 champs : date de notification du jugement, partie appelante, mode de
 * notification, représentation constituée, jugement en dernier ressort.
 */
export interface AppelCphRequest {
  /** Date de notification du jugement CPH (ISO `YYYY-MM-DD`). */
  dateNotificationJugement: string | null;
  partieAppelante: PartieAppelante | null;
  modeNotification: ModeNotification | null;
  representationConstituee: RepresentationConstituee | null;
  jugementEnDernierRessort: boolean | null;
}

/**
 * Réponse de l'endpoint POST / GET — inclut le snapshot des inputs (pour
 * ré-édition du formulaire UI) ET les sorties calculées (date limite d'appel,
 * jours restants, statut, checklist formalités, base juridique, éventuel
 * renvoi pourvoi F-DT-87).
 */
export interface AppelCphResponse extends AppelCphRequest {
  caseFileId: string;
  /** Verdict de recevabilité. */
  statut: AppelCphStatut;
  /** Date limite d'appel = notification + 1 mois (ISO `YYYY-MM-DD`). */
  dateEcheanceAppel: string | null;
  /** Jours restants jusqu'à l'échéance (négatif si expiré). */
  joursRestants: number;
  /** Checklist des formalités d'appel social. */
  checklistFormalites: AppelCphChecklistItem[];
  /** Renvoi vers le pourvoi en cassation F-DT-87 si voie d'appel fermée. */
  renvoiPourvoiCassation?: boolean;
  baseJuridique: string;
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
