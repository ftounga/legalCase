/**
 * SF-218-22 : modèles miroirs du contrat API (backend SF-218-21) pour l'outil
 * décisionnel « Stagiaire : gratification minimale et requalification en CDI »
 * (F-DT-109-stagiaire-gratification-requalification). FRANCE uniquement.
 *
 * Stage en milieu professionnel (art. L.124-1 et s. Code de l'éducation) :
 * vérification du seuil de déclenchement et calcul de la gratification minimale
 * obligatoire (au-delà de 2 mois / 44 jours de présence — art. L.124-6 +
 * D.124-6), et appréciation du risque de requalification du stage en contrat de
 * travail (CDI) en présence d'indices d'irrégularité (poste de travail
 * permanent, missions hors projet pédagogique, dépassement de la durée maximale
 * de 6 mois — art. L.124-5 et L.124-8).
 */

/**
 * Niveau de risque de requalification du stage en contrat de travail (CDI),
 * aligné EXACTEMENT sur l'enum backend {@code RisqueRequalification} :
 *  - FAIBLE : aucun indice d'irrégularité notable.
 *  - MODERE : un indice présent.
 *  - ELEVE : ≥ 2 indices présents ou durée > 6 mois.
 */
export type RisqueRequalification = 'FAIBLE' | 'MODERE' | 'ELEVE';

/**
 * Verdict global de l'analyse (aligné EXACTEMENT sur l'enum backend
 * {@code VerdictGlobal}) :
 *  - STAGE_CONFORME : gratification versée suffisante + risque de requalification
 *    non élevé.
 *  - RAPPEL_GRATIFICATION : gratification obligatoire et insuffisamment versée.
 *  - REQUALIFICATION_PROBABLE : risque de requalification élevé.
 */
export type VerdictGlobal =
  | 'STAGE_CONFORME'
  | 'RAPPEL_GRATIFICATION'
  | 'REQUALIFICATION_PROBABLE';

export interface StagiaireGratificationRequest {
  /** Date de début du stage (ISO YYYY-MM-DD, requis). */
  dateDebutStage: string;
  /** Date de fin du stage (ISO YYYY-MM-DD, requis). */
  dateFinStage: string;
  /** Jours de présence effective (1 jour = 7 h ; règle des 22 jours/mois). */
  nombreJoursPresence: number;
  /** Gratification réellement perçue (défaut 0). */
  gratificationMensuelleVersee?: number | null;
  /** Taux horaire conventionnel si CCN plus favorable (optionnel). */
  tauxHoraireConventionnel?: number | null;
  /** Exécution de tâches sans lien avec le projet pédagogique (défaut false). */
  missionsHorsProjetPedagogique?: boolean | null;
  /** Occupation d'un poste correspondant à une tâche régulière (défaut false). */
  posteTravailPermanent?: boolean | null;
}

export interface StagiaireGratificationResponse {
  caseFileId: string;
  dateDebutStage: string;
  dateFinStage: string;
  nombreJoursPresence: number;
  gratificationMensuelleVersee?: number | null;
  tauxHoraireConventionnel?: number | null;
  missionsHorsProjetPedagogique: boolean;
  posteTravailPermanent: boolean;
  /** Gratification obligatoire (seuil > 2 mois / 44 jours atteint). */
  gratificationObligatoire: boolean;
  /** Le seuil de déclenchement de la gratification est-il atteint. */
  seuilAtteint: boolean;
  /** Gratification minimale due (€). */
  gratificationMinimaleDue: number;
  /** Rappel de gratification = max(0, due − versée) (€). */
  rappelGratification: number;
  /** Niveau de risque de requalification du stage en CDI. */
  risqueRequalification: RisqueRequalification;
  /** Motifs du risque de requalification. */
  motifs: string[];
  /** Verdict global de l'analyse. */
  verdictGlobal: VerdictGlobal;
  country: string;
  baseJuridique: string;
}
