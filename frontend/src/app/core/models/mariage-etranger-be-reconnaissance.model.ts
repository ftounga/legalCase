/**
 * F-217 SF-217-17 : modèles TypeScript de l'outil décisionnel
 * "Reconnaissance d'un mariage / divorce étranger en Belgique"
 * (`mariage-etranger-be-reconnaissance`).
 *
 * BELGIQUE uniquement. Contrat API importé de SF-217-16 (backend, endpoints
 * POST/GET figés — CDIP art. 21+ / 22+ / 25 / 27 / 46 — à vérifier).
 * Incluant le talaq (répudiation, post-Moudawana 2004), les mariages
 * religieux non précédés du civil et les mariages polygames.
 */

/** Verdict de l'analyse de reconnaissance. */
export type MariageEtrangerBeReconnaissanceVerdict =
  | 'RECONNAISSANCE_DE_PLEIN_DROIT'
  | 'RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS'
  | 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC'
  | 'EXEQUATUR_REQUIS'
  | 'QUALIFICATION_INCOMPLETE';

/** Nature de l'acte étranger à reconnaître. */
export type NatureActeEtrangerBe =
  | 'MARIAGE_CIVIL_ETRANGER'
  | 'MARIAGE_RELIGIEUX_NON_CIVIL'
  | 'MARIAGE_POLYGAME'
  | 'DIVORCE_JUDICIAIRE_ETRANGER'
  | 'TALAQ_REPUDIATION';

/** Localisation de la résidence habituelle d'au moins une des parties. */
export type ResidenceHabituelleBe = 'BELGIQUE' | 'ETRANGER' | 'INCONNU';

/** Nationalité d'au moins une des parties au moment de l'acte. */
export type NationalitePartiesBe = 'BELGIQUE' | 'UE' | 'HORS_UE' | 'INCONNU';

/** Code structuré d'un motif (refus ou réserve). */
export type MotifReconnaissanceEtrangerBeCode =
  | 'POLYGAMIE_ORDRE_PUBLIC'
  | 'MARIAGE_RELIGIEUX_NON_CIVIL'
  | 'TALAQ_CONSENTEMENT_EPOUSE_ABSENT'
  | 'TALAQ_PROCEDURE_NON_CONTRADICTOIRE'
  | 'TALAQ_EPOUSE_NON_PRESENTE_NI_REPRESENTEE'
  | 'FRAUDE_LOI_RECONNAISSABLE'
  | 'MARIAGE_FORCE_DETECTE'
  | 'MARIAGE_ENFANT'
  | 'FOND_DROIT_PERSONNEL_NON_CONFORME'
  | 'FORME_LOCUS_REGIT_ACTUM_NON_CONFORME'
  | 'DECISION_NON_OFFICIELLE';

/** Sévérité d'un motif. */
export type SeveriteMotifBe = 'LOW' | 'MEDIUM' | 'HIGH';

/** Motif structuré — exposé dans la réponse. */
export interface MotifReconnaissanceEtrangerBe {
  code: MotifReconnaissanceEtrangerBeCode;
  libelle: string;
  fondement: string;
  severite: SeveriteMotifBe;
}

/**
 * Requête POST `/api/v1/case-files/{caseFileId}/mariage-etranger-be-reconnaissance`.
 *
 * `paysOrigine` doit être un code ISO 3166-1 alpha-2 (2 lettres majuscules).
 * `dateActe` au format ISO `YYYY-MM-DD`, non future.
 * Les 4 booleans talaq sont nullables sauf si
 * `natureActe = TALAQ_REPUDIATION` (validation côté backend).
 */
export interface MariageEtrangerBeReconnaissanceRequest {
  natureActe: NatureActeEtrangerBe;
  paysOrigine: string;
  dateActe: string;
  residenceHabituelleAuMoinsUnePartie: ResidenceHabituelleBe;
  nationaliteAuMoinsUnePartie: NationalitePartiesBe;
  conformiteDroitFondPersonnel: boolean;
  conformiteFormeLocusRegitActum: boolean;
  consentementEpouse: boolean | null;
  epousePresente: boolean | null;
  procedureContradictoire: boolean | null;
  decisionEcriteOfficielle: boolean | null;
  conventionBilateraleApplicable: boolean;
  commentaire: string | null;
}

/**
 * Réponse de l'endpoint POST / GET.
 *
 * Ré-expose l'intégralité du snapshot des inputs (hérité de
 * `MariageEtrangerBeReconnaissanceRequest`) pour permettre la ré-édition
 * du formulaire après rechargement, **plus** les champs calculés (verdict,
 * motifs de refus, motifs de réserve, actes à produire, bases juridiques,
 * messages d'aide).
 */
export interface MariageEtrangerBeReconnaissanceResponse
  extends MariageEtrangerBeReconnaissanceRequest {
  caseFileId: string;
  verdict: MariageEtrangerBeReconnaissanceVerdict;
  motifsRefus: MotifReconnaissanceEtrangerBe[];
  motifsReserve: MotifReconnaissanceEtrangerBe[];
  actesAProduire: string[];
  basesJuridiques: string[];
  messages: string[];
  country: string;
  calculatedAt: string;
}
