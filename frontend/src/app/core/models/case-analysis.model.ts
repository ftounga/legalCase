import { DocumentPieceType } from './document.model';
import { FamilleExtractedData } from './divorce-accepte.model';

export interface TimelineEntry {
  date: string;
  evenement: string;
}

/**
 * F-146 SF-146-01 : référence précise d'une citation — document + pièce + pages.
 * Tous les champs sauf `documentName` sont nullable (pièce non identifiable ou
 * analyse pré-F-146).
 */
export interface SourceRef {
  documentName: string | null;
  pieceType: DocumentPieceType | null;
  pieceLabel: string | null;
  pageStart: number | null;
  pageEnd: number | null;
}

export interface AnalysisItem {
  texte: string;
  source: string | null;
  extrait: string | null;
  /** F-146 SF-146-01 : référence précise (null pour les analyses legacy). */
  sourceRef?: SourceRef | null;
}

export interface CompensationEstimate {
  indemnite: number;
  salaireReference: number;
  ancienneteAnnees: number;
  ancienneteMois: number;
  typeRupture: string;
  plafondMinMois: number;
  plafondMaxMois: number;
  donneesPartielles: boolean;
}

export type ModeGardeDetaille =
  | 'ALTERNEE_FR' | 'DVH_CLASSIQUE_FR' | 'DVH_ELARGI_FR'
  | 'ALTERNEE_BE' | 'SECONDAIRE_BE' | 'SECONDAIRE_ELARGI_BE';

export interface PensionAlimentaireEstimate {
  montantMin: number;
  montantMax: number;
  revenus: number;
  nbEnfants: number;
  modeGarde: 'EXCLUSIVE' | 'ALTERNEE';
  pays: 'FRANCE' | 'BELGIQUE';
  donneesPartielles: boolean;
  modeGardeDetaille?: ModeGardeDetaille | null;
  /** F-153 SF-153-01 : fourchette jurisprudentielle JAF (null si données partielles). */
  jurisprudenceRange?: JurisprudenceRange | null;
}

export interface PrestationCompensatoireEstimate {
  montantMin: number;
  montantMax: number;
  ecartRevenus: number;
  dureeMarriage: number;
  pays: 'FRANCE' | 'BELGIQUE';
  donneesPartielles: boolean;
  /** F-153 SF-153-01 : fourchette jurisprudentielle JAF (null si données partielles). */
  jurisprudenceRange?: JurisprudenceRange | null;
}

/** F-153 SF-153-01 : fourchette p25/p50/p75 observée en jurisprudence. */
export interface JurisprudenceRange {
  p25: number;
  p50: number;
  p75: number;
  label: string;
  sourceRef: string;
}

export interface BienItem {
  libelle: string;
  valeur: number | null;
}

export interface LiquidationCommunaute {
  regimeMatrimonial: string | null;
  actifCommun: BienItem[];
  biensPropresEpouxA: BienItem[];
  biensPropresEpouxB: BienItem[];
  passifCommun: BienItem[];
}

export interface BelgianCompensationEstimate {
  preavisSemaines: number;
  indemniteCompensatoire: number;
  salaireHebdomadaire: number;
  salaireReference: number;
  ancienneteAnnees: number;
  ancienneteMois: number;
  cct109MinSemaines: number;
  cct109MaxSemaines: number;
  cct109MinEuros: number;
  cct109MaxEuros: number;
  donneesPartielles: boolean;
}

export interface CaseAnalysisResult {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  status: string;
  timeline: TimelineEntry[];
  faits: AnalysisItem[];
  pointsJuridiques: AnalysisItem[];
  risques: AnalysisItem[];
  questionsOuvertes: string[];
  piecesManquantes: string[];
  riskLevel: string | null;
  riskScore: number | null;
  modelUsed: string | null;
  updatedAt: string | null;
  analysisDocuments?: { index: number; name: string }[];
  compensationEstimate?: CompensationEstimate | null;
  belgianCompensationEstimate?: BelgianCompensationEstimate | null;
  pensionAlimentaireEstimate?: PensionAlimentaireEstimate | null;
  prestationCompensatoireEstimate?: PrestationCompensatoireEstimate | null;
  liquidationCommunaute?: LiquidationCommunaute | null;
  travailExtractedData?: TravailExtractedData | null;
  immigrationExtractedData?: ImmigrationExtractedData | null;
  /**
   * F-IA-04 : extraction Famille agrégée (ne contient que les champs
   * effectivement utilisés par les outils décisionnels Famille — voir
   * `FamilleExtractedData` pour la liste complète). Renseigné pour les
   * dossiers `DROIT_FAMILLE` (FR + BE), null sinon.
   *
   * Branchement TypeScript ajouté pour exposer au panel décisionnel
   * `ctx.synthesis?.familleExtractedData` (cf. decisional-tools-panel
   * lignes 710-1450). Backend produit ce champ via `CaseAnalysisResponse`
   * Spring depuis avril 2026 (SF-FA-10-02 et suivantes).
   */
  familleExtractedData?: FamilleExtractedData | null;
  licenciementValidityDetection?: LicenciementValidityDetection | null;
  ruptureConvValidityDetection?: RuptureConvValidityDetection | null;
  piecesManquantesDetails?: PieceManquanteEntry[] | null;
  /** F-150 : événements factuels détectés qui ouvrent un nouveau droit de séjour (liste vide hors immigration). */
  immigrationTriggerEvents?: ImmigrationTriggerEvent[] | null;
  /** F-151 : scenarii stratégiques immigration comparés (liste vide si aucun choix stratégique ouvert). */
  immigrationStrategyScenarios?: ImmigrationStrategyScenario[] | null;
  /** F-152 : détection validité divorce par consentement mutuel (famille, null hors domaine famille). */
  divorceConsentementValidityDetection?: DivorceConsentementValidityDetection | null;
  /** F-152 : scoring calculé (null si détection absente). */
  divorceConsentementScoring?: DivorceConsentementScoring | null;
  /**
   * F-197 SF-197-02 : type de litige (Travail FR) ou type de procédure
   * (Immigration) tel que détecté par l'IA, projeté top-level pour permettre
   * l'affichage badge "Type litige" dans la grille F-162 et le rappel dans
   * le dialog override. Renseigné par le backend depuis le JSON brut de
   * l'analyse (`type_litige_detecte` Travail FR / `type_procedure_detectee`
   * Immigration). Null hors Travail FR / Immigration ou si non détecté.
   */
  typeLitigeDetecte?: string | null;
}

/**
 * F-185 SF-185-01 — état partiel d'une synthèse en cours de streaming Sonnet.
 * `sections` est l'objet JSON top-level reconstruit à partir des sections déjà closes
 * (clés = `timeline`, `faits`, `pointsJuridiques`, etc. — sous-ensemble de
 * {@link CaseAnalysisResult}). Null si l'analyse vient juste de démarrer.
 *
 * Note : les clés sont en `snake_case` côté backend (telles que produites par Sonnet :
 * `points_juridiques`, `questions_ouvertes`, `pieces_manquantes`). Le frontend doit
 * mapper vers son camelCase au moment d'afficher.
 */
export interface CaseAnalysisPartialResponse {
  analysisId: string;
  version: number;
  /** F-190 SF-190-02 — type de l'analyse en cours de streaming. STANDARD = première synthèse, ENRICHED = re-analyse. */
  analysisType?: 'STANDARD' | 'ENRICHED';
  status: 'PROCESSING' | 'PARTIAL';
  sections: Partial<CaseAnalysisPartialSections> | null;
  updatedAt: string;
}

/**
 * F-185 SF-185-01 — clés JSON exactement telles que produites par Sonnet (snake_case).
 * Toutes optionnelles : seules celles déjà arrivées dans le stream sont présentes.
 */
export interface CaseAnalysisPartialSections {
  timeline: TimelineEntry[];
  faits: AnalysisItem[];
  points_juridiques: AnalysisItem[];
  risques: AnalysisItem[];
  questions_ouvertes: string[];
  pieces_manquantes: string[];
  pieces_manquantes_details: PieceManquanteEntry[];
  risk_level: string;
  risk_score: number;
  travail_extracted_data: unknown;
  immigration_extracted_data: unknown;
  famille_extracted_data: unknown;
}

/** F-152 SF-152-01 : détection validité divorce consentement mutuel. */
export interface DivorceConsentementValidityDetection {
  detections: { [critereCode: string]: DetectedAnswer };
}

/** F-152 SF-152-01 : scoring 0-100 + verdict. */
export type DivorceConsentementVerdict = 'VALIDE' | 'RISQUE_MOYEN' | 'RISQUE_ELEVE_NULLITE';

export interface DivorceConsentementScoring {
  score: number;
  verdict: DivorceConsentementVerdict;
  criteresValides: string[];
  criteresNonValides: string[];
  criteresInconnus: string[];
}

/** F-150 SF-150-01 : événement déclencheur immigration détecté dans le dossier. */
export interface ImmigrationTriggerEvent {
  eventCode: string;
  eventLabel: string;
  eventDate: string | null;
  sourceDocument: string | null;
  justification: string | null;
  baseLegale: string;
  suggestedTitleCode: string;
  suggestedTitleLabel: string;
}

/** F-151 SF-151-01 : scénario stratégique immigration. */
export type StrategyRiskLevel = 'FAIBLE' | 'MOYEN' | 'ELEVE';

export interface ImmigrationStrategyScenario {
  scenarioLabel: string;
  scenarioDescription: string;
  baseLegale: string | null;
  targetTitleCode: string | null;
  targetTitleLabel: string | null;
  delayDaysEstimate: string | null;
  riskLevel: StrategyRiskLevel | null;
  riskJustification: string | null;
  requiredAdditionalPieces: string[];
  advantages: string[];
  drawbacks: string[];
}

export interface PieceManquanteEntry {
  texte: string;
  critereCode?: string | null;
}

export interface DetectedAnswer {
  reponse: 'OUI' | 'NON' | 'INCONNU';
  justification?: string | null;
}

export interface LicenciementValidityDetection {
  detections: { [critereCode: string]: DetectedAnswer };
}

export interface RuptureConvValidityDetection {
  detections: { [critereCode: string]: DetectedAnswer };
}

export interface TravailExtractedData {
  conventionCollective?: string | null;
  dateEntree?: string | null;
  salaireBrutMensuel?: number | null;
  typeContrat?: string | null;
  poste?: string | null;
  motifLicenciement?: string | null;
  dateLicenciement?: string | null;
  /**
   * SF-218-14 : catégorie d'employé du particulier employeur (CESU) détectée
   * par l'IA, pour pré-fill F-DT-108 (FR uniquement). Aligné sur l'enum backend
   * `CategorieEmploye`.
   */
  cesuCategorieEmploye?: 'SALARIE_PARTICULIER_EMPLOYEUR' | 'ASSISTANT_MATERNEL' | null;
  /**
   * SF-218-14 : flag de visibilité CONTEXTUAL — true si l'IA détecte un
   * employeur particulier (mentions « CESU », « garde d'enfants », « assistant
   * maternel », « employé de maison », « PAJEMPLOI », « particulier employeur »).
   * FR-only. Déclenche l'apparition de F-DT-108 (n'est PAS un champ de formulaire).
   */
  particulierEmployeurDetecte?: boolean | null;
  /**
   * SF-218-16 : détention de la carte d'identité de journaliste professionnel
   * (CCIJP) détectée par l'IA, pour pré-fill F-DT-105-journaliste-statut (FR
   * uniquement). Présomption de la qualité de journaliste professionnel.
   */
  journalisteCartePresse?: boolean | null;
  /**
   * SF-218-16 : flag de visibilité CONTEXTUAL — true si l'IA détecte un statut
   * de journaliste professionnel (mentions « journaliste », « carte de presse »,
   * « clause de cession », « clause de conscience », « rédaction », « organe de
   * presse », « pigiste »). FR-only. Déclenche l'apparition de F-DT-105
   * (n'est PAS un champ de formulaire).
   */
  statutJournalisteDetecte?: boolean | null;
  /**
   * SF-218-18 : annexe du régime intermittent du spectacle détectée par l'IA,
   * pour pré-fill F-DT-106-intermittent-spectacle-are (FR uniquement) —
   * ANNEXE_8_TECHNICIENS (techniciens) ou ANNEXE_10_ARTISTES (artistes).
   */
  intermittentAnnexe?: 'ANNEXE_8_TECHNICIENS' | 'ANNEXE_10_ARTISTES' | null;
  /**
   * SF-218-18 : flag de visibilité CONTEXTUAL — true si l'IA détecte un statut
   * d'intermittent du spectacle (mentions « intermittent du spectacle »,
   * « annexe 8 », « annexe 10 », « cachet », « 507 heures », « artiste du
   * spectacle », « technicien audiovisuel », « France Travail spectacle »).
   * FR-only. Déclenche l'apparition de F-DT-106 (n'est PAS un champ de
   * formulaire).
   */
  statutIntermittentDetecte?: boolean | null;
  /**
   * SF-218-20 : participation effective à la direction de l'entreprise détectée
   * par l'IA, pour pré-fill F-DT-107-cadre-dirigeant-statut (FR uniquement) —
   * indice complémentaire exigé par la jurisprudence post-2012 (Cass. soc.)
   * pour confirmer la qualification de cadre dirigeant (art. L.3111-2 CT).
   */
  cadreParticipationDirection?: boolean | null;
  /**
   * SF-218-20 : flag de visibilité CONTEXTUAL — true si l'IA détecte des signaux
   * de cadre dirigeant ou un litige sur la qualification (mentions « cadre
   * dirigeant », « forfait sans référence horaire », « comité de direction »,
   * « COMEX », « membre du directoire », « rappel d'heures supplémentaires »
   * contre un cadre de haut niveau). FR-only. Déclenche l'apparition de
   * F-DT-107 (n'est PAS un champ de formulaire).
   */
  statutCadreDirigeantDetecte?: boolean | null;
  /**
   * SF-218-28 : un signalement interne de harcèlement a été reçu, détecté par
   * l'IA dans les pièces du dossier. Pré-fill du champ `signalementRecu` de
   * F-DT-59-harcelement-procedure-interne (FR uniquement).
   */
  harcelementSignalementInterne?: boolean | null;
  /**
   * SF-218-28 : flag de visibilité CONTEXTUAL — true si l'IA détecte des signaux
   * de procédure interne de traitement d'un signalement de harcèlement (mentions
   * « signalement de harcèlement », « référent CSE harcèlement », « enquête
   * interne », « alerte agissements sexistes », « obligation de prévention
   * employeur »), distincts d'un litige de nullité de licenciement (F-DT-11).
   * FR-only. Déclenche l'apparition de F-DT-59 (n'est PAS un champ de formulaire).
   */
  harcelementProcedureInterneDetectee?: boolean | null;
  /**
   * SF-218-30 : présence d'au moins un délégué syndical désigné dans
   * l'entreprise, détectée par l'IA dans les pièces du dossier. Pré-fill du champ
   * `delegueSyndicalPresent` de F-DT-66-nao-negociation-annuelle (déclencheur de
   * l'obligation de négociation annuelle obligatoire, FR uniquement).
   */
  delegueSyndicalPresent?: boolean | null;
  /**
   * SF-218-30 : flag de visibilité CONTEXTUAL — true si l'IA détecte des signaux
   * de négociation annuelle obligatoire (mentions « négociation annuelle
   * obligatoire », « NAO », « PV de désaccord », « réunion de négociation
   * salariale », « accord de méthode », « délégué syndical » dans un contexte de
   * négociation collective). FR-only. Déclenche l'apparition de
   * F-DT-66-nao-negociation-annuelle (n'est PAS un champ de formulaire).
   */
  naoDetectee?: boolean | null;
  /**
   * SF-218-36 : flag de visibilité CONTEXTUAL — true si l'IA détecte des signaux
   * de règlement intérieur (mentions « règlement intérieur », « échelle des
   * sanctions », « clause du règlement intérieur », « dépôt au greffe »,
   * « consultation CSE règlement »). FR-only. Déclenche l'apparition de
   * F-DT-100-reglement-interieur-validite (n'est PAS un champ de formulaire).
   */
  reglementInterieurDetecte?: boolean | null;
  /**
   * SF-218-36 : true si un règlement intérieur existe effectivement dans
   * l'entreprise, détecté par l'IA dans les pièces du dossier. Pré-fill du champ
   * `reglementExiste` de F-DT-100-reglement-interieur-validite (FR uniquement).
   */
  reglementInterieurPresent?: boolean | null;
  /**
   * SF-218-38 : flag de visibilité CONTEXTUAL — true si l'IA détecte des signaux
   * de monétisation de jours de RTT (mentions « rachat de RTT », « monétisation
   * des RTT », « renonciation à des jours de RTT », « jours de RTT payés »,
   * « rémunération majorée des RTT »). FR-only. Déclenche l'apparition de
   * F-DT-51-rtt-monetisation (n'est PAS un champ de formulaire).
   *
   * NB : le sous-record backend consolidé `Sf218dDetail` est sérialisé
   * `@JsonUnwrapped` avec des clés JSON SNAKE_CASE explicites (`@JsonProperty`)
   * — d'où le nom de propriété snake_case ci-dessous (contrairement aux champs
   * camelCase du record parent et du sous-record Sf218cIrpDetail). Ne PAS
   * renommer en camelCase sous peine de pré-fill vide.
   */
  rtt_monetisation_detectee?: boolean | null;
  /**
   * SF-218-38 : nombre de jours de RTT auxquels le salarié renonce, détecté par
   * l'IA dans les pièces du dossier (demande de monétisation, avenant). Pré-fill
   * du champ `nombreJoursRttRenonces` de F-DT-51-rtt-monetisation (FR
   * uniquement). Clé JSON SNAKE_CASE (Sf218dDetail `@JsonProperty`).
   */
  nombre_jours_rtt_renonces?: number | null;
  /**
   * SF-218-38 : salaire journalier brut de référence détecté par l'IA dans les
   * pièces du dossier (bulletins, contrat). Pré-fill du champ
   * `salaireJournalierBrut` de F-DT-51-rtt-monetisation (FR uniquement). Clé JSON
   * SNAKE_CASE (Sf218dDetail `@JsonProperty`).
   */
  salaire_journalier_brut?: number | null;
  /**
   * SF-218-40 : flag pivot CONTEXTUAL — true quand l'IA détecte des signaux de
   * prime de partage de la valeur (« PPV », « prime de partage de la valeur »,
   * « prime Macron », « prime exceptionnelle de pouvoir d'achat », « prime
   * PEPA »). Déclenche l'apparition de F-DT-52-ppv-exoneration (FR uniquement).
   * Clé JSON SNAKE_CASE (Sf218dDetail `@JsonProperty`).
   */
  ppv_detectee?: boolean | null;
  /**
   * SF-218-40 : montant de la PPV versée au bénéficiaire sur l'année civile,
   * détecté par l'IA dans les pièces du dossier (bulletins, accord). Pré-fill du
   * champ `montantPrime` de F-DT-52-ppv-exoneration (FR uniquement). Clé JSON
   * SNAKE_CASE (Sf218dDetail `@JsonProperty`).
   */
  montant_ppv?: number | null;
  /**
   * SF-218-40 : présence d'un accord d'intéressement dans l'entreprise détectée
   * par l'IA (porte le plafond social PPV à 6 000 €). Pré-fill du champ
   * `accordInteressementPresent` de F-DT-52-ppv-exoneration (FR uniquement). Clé
   * JSON SNAKE_CASE (Sf218dDetail `@JsonProperty`). Champ partagé avec F-DT-53.
   */
  accord_interessement_present?: boolean | null;
  /**
   * SF-218-42 : flag de visibilité CONTEXTUAL — true si l'IA détecte un
   * dispositif d'ÉPARGNE SALARIALE dans les pièces (intéressement art. L.3312-1
   * et s. CT, participation art. L.3322-1 et s. CT, PEE/PERCO, « abondement
   * employeur », « réserve spéciale de participation »). Déclenche l'apparition
   * de F-DT-53-epargne-salariale-conformite (FR uniquement). Clé JSON
   * SNAKE_CASE (Sf218dDetail `@JsonProperty`).
   */
  epargne_salariale_detectee?: boolean | null;
  /**
   * SF-218-42 : présence d'un accord (ou régime) de participation dans
   * l'entreprise, détectée par l'IA. Pré-fill du champ
   * `accordParticipationPresent` de F-DT-53-epargne-salariale-conformite (FR
   * uniquement). Clé JSON SNAKE_CASE (Sf218dDetail `@JsonProperty`).
   */
  accord_participation_present?: boolean | null;
  /**
   * SF-218-44 : flag pivot CONTEXTUAL — l'IA a détecté des signaux de congé pour
   * évènement familial (mentions « congé mariage », « congé naissance », « congé
   * décès », « congé pour évènement familial », « jours pour décès d'un proche »,
   * « annonce du handicap de l'enfant »). Déclenche l'apparition de
   * F-DT-76-conges-evenements-familiaux (FR uniquement). Clé JSON SNAKE_CASE
   * (Sf218dDetail `@JsonProperty`).
   */
  conge_evt_familial_detecte?: boolean | null;
  /**
   * SF-218-44 : type d'évènement familial identifié par l'IA dans les pièces du
   * dossier, pour pré-fill du champ `typeEvenement` de
   * F-DT-76-conges-evenements-familiaux (FR uniquement). Clé JSON SNAKE_CASE
   * (Sf218dDetail `@JsonProperty`).
   */
  type_evenement_familial?: string | null;
  /**
   * SF-218-46 : flag pivot CONTEXTUAL — l'IA a détecté des signaux de congé
   * parental d'éducation (mentions « congé parental », « congé parental
   * d'éducation », « PreParE », « réintégration après congé parental », « temps
   * partiel pour élever un enfant »). Déclenche l'apparition de
   * F-DT-78-conge-parental-education (FR uniquement). Clé JSON SNAKE_CASE
   * (Sf218dDetail `@JsonProperty`).
   */
  conge_parental_detecte?: boolean | null;
  /**
   * SF-218-46 : date de naissance ou d'arrivée de l'enfant au foyer identifiée
   * par l'IA dans les pièces du dossier, pour pré-fill du champ
   * `dateNaissanceOuAdoption` de F-DT-78-conge-parental-education (FR
   * uniquement). Format ISO yyyy-MM-dd. Clé JSON SNAKE_CASE (Sf218dDetail
   * `@JsonProperty`).
   */
  date_naissance_ou_adoption?: string | null;
  /**
   * SF-218-48 : flag pivot CONTEXTUAL — l'IA a détecté des signaux de congé de
   * proche aidant (mentions « congé de proche aidant », « proche aidant »,
   * « AJPA », « allocation journalière du proche aidant », « aider un parent
   * dépendant », « perte d'autonomie d'un proche »). Déclenche l'apparition de
   * F-DT-79-conge-proche-aidant (FR uniquement). Clé JSON SNAKE_CASE
   * (Sf218dDetail `@JsonProperty`).
   */
  conge_proche_aidant_detecte?: boolean | null;
  /**
   * SF-218-48 : lien avec la personne aidée identifié par l'IA dans les pièces
   * du dossier, pour pré-fill du champ `lienPersonneAidee` de
   * F-DT-79-conge-proche-aidant (FR uniquement). Valeur alignée sur l'enum
   * backend {@code CongeProcheAidantLien} (CONJOINT / ASCENDANT / DESCENDANT /
   * COLLATERAL / SANS_LIEN_RESIDENCE_COMMUNE). Clé JSON SNAKE_CASE (Sf218dDetail
   * `@JsonProperty`).
   */
  lien_personne_aidee?: string | null;
  /**
   * SF-218-50 : flag pivot CONTEXTUAL — l'IA a détecté des signaux d'acquisition
   * de JRTT (mentions « jours de RTT », « JRTT », « accord d'aménagement du temps
   * de travail », « horaire collectif 37 heures / 39 heures », « jours de repos
   * compensateurs de réduction du temps de travail »). Déclenche l'apparition de
   * F-DT-80-rtt-acquisition (FR uniquement). Clé JSON SNAKE_CASE (Sf218dDetail
   * `@JsonProperty`).
   */
  rtt_acquisition_detectee?: boolean | null;
  /**
   * SF-218-50 : horaire hebdomadaire collectif (heures) extrait par l'IA, pour
   * pré-fill du champ `horaireHebdomadaireCollectif` de F-DT-80-rtt-acquisition
   * (FR uniquement ; ex. 37, 39). Clé JSON SNAKE_CASE (Sf218dDetail
   * `@JsonProperty`).
   */
  horaire_hebdomadaire_collectif?: number | null;
  /**
   * SF-218-52 : flag pivot CONTEXTUAL — l'IA a détecté des signaux de temps de
   * trajet / déplacement professionnel (mentions « temps de trajet », « temps de
   * déplacement professionnel », « contrepartie au temps de trajet », « salarié
   * itinérant », « déplacement domicile-client », « dépassement du temps normal
   * de trajet »). Déclenche l'apparition de F-DT-81-temps-trajet-deplacement
   * (FR uniquement). Clé JSON SNAKE_CASE (Sf218dDetail `@JsonProperty`).
   */
  temps_trajet_detecte?: boolean | null;
  /**
   * SF-218-52 : type de trajet professionnel détecté par l'IA, pour pré-fill du
   * champ `typeTrajet` de F-DT-81-temps-trajet-deplacement (FR uniquement).
   * Valeur alignée sur l'enum backend {@code TypeTrajet}
   * (DOMICILE_TRAVAIL_HABITUEL / DOMICILE_CLIENT_DEPASSEMENT /
   * ITINERANT_SANS_LIEU_FIXE). Clé JSON SNAKE_CASE (Sf218dDetail `@JsonProperty`).
   */
  type_trajet?: string | null;
  /**
   * SF-218-52 : temps de trajet quotidien (minutes) extrait par l'IA, pour
   * pré-fill du champ `tempsTrajetQuotidienMinutes` de
   * F-DT-81-temps-trajet-deplacement (FR uniquement). Clé JSON SNAKE_CASE
   * (Sf218dDetail `@JsonProperty`).
   */
  temps_trajet_quotidien_minutes?: number | null;
  /**
   * SF-218-54 : flag pivot CONTEXTUAL — l'IA a détecté des signaux de droit à la
   * déconnexion (mentions « droit à la déconnexion », « charte de déconnexion »,
   * « plages de déconnexion », « usage des outils numériques », « sollicitations
   * hors temps de travail », « emails en dehors des heures de travail »).
   * Déclenche l'apparition de F-DT-83-droit-deconnexion-conformite (FR
   * uniquement). Clé JSON SNAKE_CASE (Sf218dDetail `@JsonProperty`). N'est PAS un
   * champ de formulaire.
   */
  droit_deconnexion_detecte?: boolean | null;
  /**
   * SF-218-54 : présence d'un accord ou d'une charte sur le droit à la
   * déconnexion détectée par l'IA, pour pré-fill du champ `accordOuChartePresent`
   * de F-DT-83-droit-deconnexion-conformite (FR uniquement). Clé JSON SNAKE_CASE
   * (Sf218dDetail `@JsonProperty`).
   */
  accord_deconnexion_present?: boolean | null;
  /**
   * SF-218-34 : type de mandat syndical (DELEGUE_SYNDICAL / RSS) détecté par
   * l'IA dans les pièces du dossier (lettre de désignation, procès-verbal CSE),
   * pour pré-fill du champ `typeMandat` de
   * F-DT-69-delegation-syndicale-protection (FR uniquement).
   */
  mandatSyndicalType?: 'DELEGUE_SYNDICAL' | 'RSS' | null;
  /**
   * SF-218-34 : flag de visibilité CONTEXTUAL — true si l'IA détecte des signaux
   * de désignation d'un délégué syndical (DS) ou d'un représentant de section
   * syndicale (RSS) (mentions « délégué syndical », « RSS », « représentant de
   * section syndicale », « désignation syndicale », « section syndicale »).
   * FR-only. Déclenche l'apparition de F-DT-69-delegation-syndicale-protection
   * (n'est PAS un champ de formulaire).
   */
  delegationSyndicaleDetectee?: boolean | null;
  /**
   * SF-218-32 : % des suffrages exprimés au 1er tour des dernières élections
   * recueilli par les syndicats signataires d'un accord d'entreprise, détecté par
   * l'IA (∈ [0 ; 100]). Pré-fill du champ `pourcentageSuffragesSignataires` de
   * F-DT-67-accord-entreprise-validite (FR uniquement). Mappe la clé JSON backend
   * `accord_pourcentage_signataires`.
   */
  accordPourcentageSignataires?: number | null;
  /**
   * SF-218-32 : type d'opération portant sur l'accord d'entreprise détecté par
   * l'IA (CONCLUSION / REVISION / DENONCIATION). Pré-fill du champ `typeOperation`
   * de F-DT-67-accord-entreprise-validite (FR uniquement). Mappe la clé JSON
   * backend `accord_type_operation`.
   */
  accordTypeOperation?: string | null;
  /**
   * SF-218-32 : flag de visibilité CONTEXTUAL — true si l'IA détecte des signaux
   * de validité d'un accord d'entreprise (mentions « accord d'entreprise », «
   * avenant de révision », « dénonciation de l'accord », « conditions de majorité
   * L.2232-12 », « référendum de validation »). FR-only. Déclenche l'apparition de
   * F-DT-67-accord-entreprise-validite (n'est PAS un champ de formulaire). Mappe la
   * clé JSON backend `accord_entreprise_detecte`.
   */
  accordEntrepriseDetecte?: boolean | null;
  /**
   * SF-218-22 : date de début du stage détectée par l'IA dans la convention de
   * stage (ISO YYYY-MM-DD), pour pré-fill F-DT-109-stagiaire-gratification-
   * requalification (FR uniquement).
   */
  dateDebutStage?: string | null;
  /**
   * SF-218-22 : date de fin du stage détectée par l'IA (ISO YYYY-MM-DD), pour
   * pré-fill F-DT-109-stagiaire-gratification-requalification (FR uniquement).
   */
  dateFinStage?: string | null;
  /**
   * SF-218-22 : flag de visibilité CONTEXTUAL — true si l'IA détecte un stage
   * en milieu professionnel (mentions « convention de stage », « stagiaire »,
   * « gratification », « établissement d'enseignement », « tuteur de stage »,
   * « PFMP », « école »). FR-only. Déclenche l'apparition de F-DT-109 (n'est PAS
   * un champ de formulaire).
   */
  stageDetecte?: boolean | null;
  /**
   * SF-218-24 : motif de rupture du contrat d'apprentissage détecté par l'IA,
   * pour pré-fill F-DT-110-apprentissage-rupture (FR uniquement). Aligné sur
   * l'enum backend {@code MotifRupture}.
   */
  apprentissageMotifRupture?:
    | 'ACCORD_PARTIES'
    | 'FAUTE_GRAVE'
    | 'FORCE_MAJEURE'
    | 'INAPTITUDE'
    | 'EXCLUSION_DEFINITIVE_CFA'
    | 'SANS_MOTIF'
    | null;
  /**
   * SF-218-24 : flag de visibilité CONTEXTUAL — true si l'IA détecte une rupture
   * de contrat d'apprentissage (mentions « contrat d'apprentissage », « apprenti »,
   * « CFA », « maître d'apprentissage », « rupture apprentissage », « 45 jours »).
   * FR-only. Déclenche l'apparition de F-DT-110 (n'est PAS un champ de formulaire).
   */
  apprentissageRuptureDetectee?: boolean | null;
  /**
   * SF-218-26 : secteur du CDI de chantier / d'opération détecté par l'IA, pour
   * pré-fill F-DT-37-licenciement-cdi-chantier (FR uniquement). Aligné sur l'enum
   * backend {@code SecteurChantier} (BTP / INGENIERIE / AUTRE).
   */
  cdiChantierSecteur?: 'BTP' | 'INGENIERIE' | 'AUTRE' | null;
  /**
   * SF-218-26 : flag de visibilité CONTEXTUAL — true si l'IA détecte un CDI de
   * chantier / d'opération (mentions « CDI de chantier », « contrat de chantier »,
   * « contrat d'opération », « fin de chantier », « BTP », « ingénierie »,
   * « licenciement pour fin de chantier »). FR-only. Déclenche l'apparition de
   * F-DT-37 (n'est PAS un champ de formulaire).
   */
  cdiChantierDetecte?: boolean | null;
  congesContractuels?: number | null;
  primeAncienneteContractuelle?: number | null;
  /** SF-130-01 : true si salaireBrutMensuel a été déduit d'un net via × 1,30 */
  salaireEstDeduit?: boolean | null;
  /** SF-155-04 : motif de nullité pressenti pour pré-fill F-DT-11 harcèlement / discrimination (FR uniquement). */
  motifNullitePressenti?: 'DISCRIMINATION' | 'HARCELEMENT_MORAL' | 'HARCELEMENT_SEXUEL'
    | 'RETORSION' | 'SYNDICAL' | 'MATERNITE_PATERNITE' | 'ACCIDENT_MP' | null;
  /** SF-155-04 : origine d'inaptitude pressentie pour pré-fill F-DT-15 (FR uniquement). */
  origineInaptitudePressentie?: 'ACCIDENT_TRAVAIL' | 'MALADIE_PROFESSIONNELLE' | 'MALADIE_ORDINAIRE' | null;
  /** SF-155-04 : date de l'avis d'inaptitude (YYYY-MM-DD) pour pré-fill F-DT-15. */
  avisMedecinTravailDate?: string | null;
  /** SF-155-04 : détection recherche de reclassement documentée pour pré-fill F-DT-15. */
  reclassementRespecteDetected?: DetectedAnswer | null;
  /** SF-155-04 : heures sup mentionnées dans le dossier pour pré-fill F-DT-19 (FR uniquement). */
  heuresSupMentionneesDansDossier?: HeuresSupMentionnees | null;
  // SF-246-22 : suppression du vestige `fautesDetectees` — ce champ a été
  // déplacé vers `FamilleExtractedData` par SF-246-03. Il n'a jamais été
  // alimenté par le pipeline IA côté Travail (stub aspirationnel).
  // Le composant `divorce-faute-section` utilise `FamilleExtractedData` — aucun impact.
  /**
   * SF-DT-29-02 : âge du demandeur en années entières pour pré-fill F-DT-29
   * (crédit-temps BE, AR 29/10/1997 fin de carrière nécessite âge ≥ 55/60).
   * SF-246-05 : champ branché sur le pipeline IA — extrait par
   * `extractTravailData()` depuis la clé `age_demandeur_annees` (Travail BE
   * uniquement, borné [0, 100]). `null` si non déterminable ou dossier FR.
   */
  ageDemandeurAnnees?: number | null;
  /**
   * F-197 SF-197-02 : type de litige tel qu'il a été détecté par le pipeline
   * IA (Travail FR uniquement). Présent pour permettre aux outils
   * décisionnels de raisonner sur le type IA brut, indépendamment de
   * l'override avocat ({@link #typeLitigeAvocatOverride}). Renseigné
   * post-pipeline IA via projection JSON `type_litige_detecte`.
   */
  typeLitigeDetecte?: string | null;
  /**
   * F-197 SF-197-02 : override avocat single-value du type de litige (Travail
   * FR). Si présent, prend précédence sur {@link #typeLitigeDetecte} pour le
   * pré-remplissage des outils décisionnels au prochain run de Synthèse
   * enrichie (F-DT-08/09/10/11/12/13). Null tant qu'aucun override n'a été
   * posé. Persisté côté backend dans la table `case_file_type_litige_override`
   * (cf. SF-197-01).
   */
  typeLitigeAvocatOverride?: string | null;
  /**
   * SF-246-01 : flags procéduraux du licenciement pour pré-fill F-DT-36
   * (nullité de procédure, FR uniquement). Tous nullables — restent `null` pour
   * un dossier travail belge (concept procédural FR sans équivalent BE direct).
   */
  convocationEntretienDetectee?: boolean | null;
  dateConvocationEntretienDetectee?: string | null;
  dateEntretienPrealableDetectee?: string | null;
  entretienPrealableTenuDetected?: DetectedAnswer | null;
  lettreLicenciementEcriteDetectee?: boolean | null;
  lettreLicenciementMotiveeDetected?: DetectedAnswer | null;
  motivationLettreSuffisanteDetected?: DetectedAnswer | null;
  /**
   * SF-246-02 : flag de visibilité F-166 — `true` si une clause de non-concurrence
   * est textuellement présente au contrat de travail produit aux pièces. Réutilisé
   * par F-DT-24 comme pré-fill du booléen `clausePresenteContrat`. Travail FR.
   */
  clauseNonConcurrenceDetectee?: boolean | null;
  /**
   * SF-246-02 : détail de la clause de non-concurrence pour pré-fill F-DT-24
   * (FR uniquement). Durée en mois (`[0, 600]`), zone géographique en texte libre
   * (≤ 500 car.), contrepartie en euros bruts mensuels (`> 0`). Tous nullables —
   * restent `null` pour un dossier travail belge (régime CCT 1bis distinct).
   */
  nonConcurrenceDureeMois?: number | null;
  nonConcurrenceZoneGeographique?: string | null;
  nonConcurrenceContrepartieMontantEur?: number | null;
  /**
   * SF-246-13 : date de prise d'effet + secteur d'activité de la clause de
   * non-concurrence pour pré-fill F-DT-24 (FR uniquement). Tous deux nullables.
   * `nonConcurrenceDatePriseEffet` : date ISO YYYY-MM-DD (= date de fin/rupture contrat).
   * `nonConcurrenceSecteurActivite` : code parmi INFORMATIQUE / COMMERCE / INDUSTRIE / SERVICES / AUTRE.
   */
  nonConcurrenceDatePriseEffet?: string | null;
  nonConcurrenceSecteurActivite?: string | null;
  /**
   * SF-246-15 : identités salarié/employeur pour pré-fill des fiches de procédure
   * `prudhome-fiche` (FR) et `tribunal-travail-fiche` (BE). Champs présents dans le
   * record backend depuis F-DT-04/F-DT-06 — dette DTO frontend réglée ici.
   * `siretEmployeur` : FR uniquement (14 chiffres) ; `bceEmployeur` : BE uniquement.
   */
  nomSalarie?: string | null;
  prenomSalarie?: string | null;
  adresseSalarie?: string | null;
  nomEmployeur?: string | null;
  adresseEmployeur?: string | null;
  siretEmployeur?: string | null;
  bceEmployeur?: string | null;
  /**
   * SF-207-01 : 2 champs IA Travail BE pour pré-fill F-207
   * (prescription Travail BE). Tous deux nullables.
   * `dateRuptureContrat` : date de rupture du contrat au format ISO YYYY-MM-DD
   * (point de départ du délai de 1 an post-rupture, Loi 03/07/1978 art. 15
   * al. 1 + CCT 109 art. 11). Distincte de `dateLicenciement` (date de
   * notification) — la date de rupture intègre le préavis presté éventuel.
   * `motifRupture` : motif de rupture détecté en texte libre court
   * (`licenciement`, `démission`, `faute grave`, `RCC`, `rupture amiable`,
   * `fin de CDD`...). Utilisé pour pré-fill `typeCreance`.
   * Restent `null` pour un dossier Travail FR.
   */
  dateRuptureContrat?: string | null;
  motifRupture?: string | null;
  /**
   * SF-213-02b : 3 champs IA Travail BE pour pré-fill `rappel-salaire-be`
   * (calculateur d'arriérés de salaire BE — Loi 12/04/1965 art. 10 +
   * Loi 03/07/1978 art. 15). Tous trois nullables, branche BE uniquement.
   *
   * - `montantArrieresSalaireBrut` : montant brut total des arriérés réclamés
   *   en euros (number > 0). Inclut salaire de base, primes dues,
   *   ancienneté oubliée, indexation manquante, sursalaire conventionnel.
   * - `dateDebutArrieresSalaire` : date ISO YYYY-MM-DD du début de la
   *   période d'arriéré (= date de la 1ère échéance non payée).
   * - `dateFinArrieresSalaire` : date ISO YYYY-MM-DD de la fin de la
   *   période d'arriéré (= date d'exigibilité principale, pivot du calcul
   *   des intérêts moratoires et du délai de prescription).
   *
   * Restent `null` pour un dossier Travail FR (régime distinct :
   * intérêts légaux variables semestriels art. 1153 C.civ. + prescription
   * 3 ans C. trav. L. 3245-1) — l'outil n'a pas d'équivalent FR direct.
   */
  montantArrieresSalaireBrut?: number | null;
  dateDebutArrieresSalaire?: string | null;
  dateFinArrieresSalaire?: string | null;
  /**
   * SF-246-22 : type de procédure travail identifié par le pipeline IA pour pré-fill
   * F-136 `travail-procedure` (calendrier procédural FR + BE).
   * Codes admis (6 exacts — 3 FR + 3 BE) :
   * `PRUDHOMMES_FR`, `APPEL_CA_SOCIALE_FR`, `CASSATION_SOCIALE_FR` (France),
   * `TRIBUNAL_TRAVAIL_BE`, `COUR_TRAVAIL_BE`, `CASSATION_BE` (Belgique).
   * Code hors whitelist → null. Gating pays appliqué par `TravailProcedurePrefillRules`
   * (`_FR` pour workspace France, `_BE` pour workspace Belgique).
   * Source backend réelle : `procedure_travail_detection.procedure_detectee`.
   * Remplace le stub aspirationnel de l'ancien type d'intersection `TravailProcedureAiData`.
   */
  procedureTravailDetectee?: string | null;
  /**
   * SF-246-22 : date déclencheur de la procédure travail (date de saisine prud'homale,
   * citation à comparaître, dépôt de requête) au format ISO YYYY-MM-DD.
   * Null si non détectable ou si le sous-objet `procedure_travail_detection` est absent.
   * Source backend réelle : `procedure_travail_detection.date_declencheur`.
   * Remplace le stub aspirationnel de l'ancien type d'intersection `TravailProcedureAiData`.
   */
  dateDeclencheurProcedure?: string | null;
  /**
   * SF-207-02b : 6 champs IA Travail BE pour pré-fill F-207 (checklist C4 ONEM).
   * Tous nullables — restent `null` pour un dossier Travail FR (régime distinct
   * de l'attestation France Travail R.1234-9).
   * `raisonSocialeEmployeur` : raison sociale (dénomination juridique) de l'employeur.
   * `numeroBce` : numéro BCE (Banque-Carrefour des Entreprises) — 10 chiffres.
   * `categorieOnem` : code catégorie ONEM (ex. "9" pour faute grave).
   * `motifExplicite` : motif explicite de la fin de contrat (texte libre du C4).
   * `preavisPresteJours` : durée de préavis presté en jours (Integer côté backend).
   * `dernierSalaireMensuelBrut` : dernier salaire mensuel brut (BigDecimal backend
   * → number frontend).
   */
  raisonSocialeEmployeur?: string | null;
  numeroBce?: string | null;
  categorieOnem?: string | null;
  motifExplicite?: string | null;
  preavisPresteJours?: number | null;
  dernierSalaireMensuelBrut?: number | null;
  /**
   * SF-207-03 / SF-207-03b : 3 champs IA Travail BE pour pré-fill F-207
   * (contestation décision C4 ONEM). BE-only — restent null pour FR.
   * `dateNotificationDecisionOnem` : date de notification de la décision
   * ONEM contestée (ISO YYYY-MM-DD).
   * `dateDecisionDirecteur` : date de notification de la décision du
   * Directeur du Bureau du chômage sur le recours administratif (ISO).
   * `recoursAdminDejaForme` : true si le recours administratif au
   * Directeur a déjà été formé (Boolean nullable côté backend).
   */
  dateNotificationDecisionOnem?: string | null;
  dateDecisionDirecteur?: string | null;
  recoursAdminDejaForme?: boolean | null;
  /**
   * SF-207-04 / SF-207-04b : 2 champs IA Travail BE pour pré-fill F-207
   * (déclaration AT Fedris). BE-only — restent null pour FR.
   * `dateAccident` : date de survenance de l'accident du travail (ISO YYYY-MM-DD).
   * `dateConnaissanceAccidentEmployeur` : date à laquelle l'employeur a eu
   * connaissance de l'accident (point de départ du délai 8 j Fedris, ISO).
   */
  dateAccident?: string | null;
  dateConnaissanceAccidentEmployeur?: string | null;
  /**
   * SF-207-05 / SF-207-05b : 3 champs IA Travail BE pour pré-fill F-207
   * (référé tribunal du travail BE — CJ art. 584). BE-only — restent null
   * pour FR (régime distinct du référé prud'homal R.1454-1 CT).
   * `motifUrgenceDetecte` : code parmi la whitelist
   *   {HARCELEMENT, SALAIRE_IMPAYE, MODIFICATION_UNILATERALE, AUTRE} détecté
   *   par le pipeline IA sur le motif d'urgence (texte libre du dossier).
   *   Toute valeur hors whitelist → null.
   * `dateFaitGenerateurUrgence` : date du fait générateur de l'urgence (ISO
   *   YYYY-MM-DD) — point de départ du raisonnement « urgence qualifiable ».
   * `perilImmediatPresume` : flag présumant un péril en demeure caractérisé
   *   (préjudice imminent / irréversible) — pré-fill du booléen `perilEnDemeure`.
   */
  motifUrgenceDetecte?: string | null;
  dateFaitGenerateurUrgence?: string | null;
  perilImmediatPresume?: boolean | null;
  /**
   * SF-207-06 / SF-207-06b : 4 champs IA Travail BE pour pré-fill F-207
   * (RCC BE — conditions d'éligibilité, ex-prépension). BE-only — restent
   * null pour FR (régime distinct, sans équivalent direct).
   * `dateNaissanceSalarie` : date de naissance du salarié (ISO YYYY-MM-DD).
   *   Sert au calcul de l'âge à la date de licenciement envisagée
   *   (seuils CCT 17 / CCT 17/13 / AR 03/05/2007).
   * `anneesCarriereSalarie` : nombre d'années de carrière professionnelle
   *   salariée cumulées (entier borné [0, 60]).
   * `metierLourdDetecte` : flag détecté de reconnaissance de métier lourd
   *   (CCT 17/13). True uniquement → pré-fill ; false / autre → laissé décoché.
   * `entrepriseEnDifficulteDetectee` : flag détecté de reconnaissance
   *   d'entreprise en difficulté par arrêté ministériel (AR 03/05/2007 art. 8).
   *   True uniquement → pré-fill ; false / autre → laissé décoché.
   */
  dateNaissanceSalarie?: string | null;
  anneesCarriereSalarie?: number | null;
  metierLourdDetecte?: boolean | null;
  entrepriseEnDifficulteDetectee?: boolean | null;
  /**
   * SF-207-07 / SF-207-07b : 3 champs IA Travail BE pour pré-fill F-207
   * (RCC BE — indemnité complémentaire, calculateur CCT 17 art. 5). BE-only —
   * restent null pour FR (pas d'équivalent direct du RCC en droit français).
   * `remunerationNetteReferenceRccDetectee` : rémunération nette mensuelle de
   *   référence à la rupture (€). Base de calcul de la formule CCT 17 art. 5
   *   (indemnité = (remunNette − allocOnem) / 2, plancher 0).
   * `allocationOnemMensuelleEstimee` : allocation ONEM mensuelle estimée (€).
   *   Fournie par l'avocat (formule complexe ONEM hors scope IA — estimation
   *   conservative seulement).
   * `dateDebutRccEnvisagee` : date de début effective du RCC (ISO YYYY-MM-DD).
   *   Base de comptage du nombre de mensualités jusqu'à l'âge légal de la pension.
   */
  remunerationNetteReferenceRccDetectee?: number | null;
  allocationOnemMensuelleEstimee?: number | null;
  dateDebutRccEnvisagee?: string | null;
  /**
   * SF-207-08 / SF-207-08b : 3 champs IA Travail BE pour pré-fill F-207
   * (outplacement obligatoire 45+ — CCT n°82, CCT n°82 bis, Loi 05/09/2001
   * art. 13, AR 30/05/2018, AR 25/11/1991 art. 154). BE-only — restent null
   * pour FR (pas d'équivalent direct du régime BE d'outplacement).
   * `ancienneteSalarie` : ancienneté du salarié à la date du licenciement
   *   en années avec décimales (Double côté backend, borne soft [0, 60]).
   * `motifLicenciementDetecte` : motif de rupture détecté — whitelist
   *   stricte {LICENCIEMENT_ECONOMIQUE | LICENCIEMENT_AUTRE | FAUTE_GRAVE |
   *   DEMISSION}. Toute valeur hors whitelist → null.
   * `offreOutplacementMentionnee` : flag détecté de mention d'une offre
   *   d'outplacement formelle dans le dossier (Boolean nullable côté backend).
   *   Pré-fill true uniquement — false ou null laissent la case décochée.
   */
  ancienneteSalarie?: number | null;
  motifLicenciementDetecte?: string | null;
  offreOutplacementMentionnee?: boolean | null;
  /**
   * SF-213-01 / SF-213-01b : 3 champs IA Travail BE pour pré-fill F-213
   * (clause de non-concurrence BE — Loi 03/07/1978 art. 65 + CCT n°13).
   * BE-only — restent null pour FR (régime distinct, F-DT-24 FR est à montant libre).
   * `salaireBrutAnnuel` : rémunération annuelle brute (€ > 0). Base seuil
   *   légal CCT n°13 (73 571 € en 2024, indexé annuellement).
   * `clauseNonConcurrenceDureeMois` : durée de la clause en mois (entier 1-12).
   *   Borne maximale légale CCT n°13.
   * `clauseNonConcurrenceZone` : zone géographique de la clause
   *   {BELGIQUE_UNIQUEMENT | BELGIQUE_ET_ETRANGER | NON_SPECIFIEE}. Toute
   *   autre valeur → null (l'avocat tranche).
   */
  salaireBrutAnnuel?: number | null;
  clauseNonConcurrenceDureeMois?: number | null;
  clauseNonConcurrenceZone?: string | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `requalification_detection` (CDD + intérim)
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Durée du dernier CDD en mois [0, 120]. Source : contrat CDD. */
  cddDureeMois?: number | null;
  /** Date de fin du dernier CDD (ISO YYYY-MM-DD). */
  cddDateFinDernierContrat?: string | null;
  /** Date de début du CDD suivant — succession de CDD (ISO). */
  cddNouveauDateDebut?: string | null;
  /** Date de fin du CDD suivant (ISO). */
  cddNouveauDateFin?: string | null;
  /** Total des salaires bruts sur la durée du CDD (€ > 0). */
  cddTotalSalairesBruts?: number | null;
  /** Durée totale cumulée des missions d'intérim en mois [0, 120]. */
  interimDureeTotaleMois?: number | null;
  /** Date de fin de la dernière mission d'intérim (ISO YYYY-MM-DD). */
  interimDateFinDerniereMission?: string | null;
  /** Date de début d'une nouvelle mission d'intérim (ISO). */
  interimNouvellesMissionDateDebut?: string | null;
  /** Date de fin d'une nouvelle mission d'intérim (ISO). */
  interimNouvellesMissionDateFin?: string | null;
  /** Nom ou SIRET de l'entreprise utilisatrice (≤ 200 car.). */
  interimEntrepriseUtilisatrice?: string | null;
  /** Total des rémunérations brutes sur toutes missions (€ > 0). */
  interimTotalRemunerationsBrutes?: number | null;
  /** Durée de la mission d'intérim en jours calendaires [0, 3650]. */
  interimDureeMissionJours?: number | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `paie_detection`
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Jours de congés payés acquis [0, 50]. Source : bulletins / STC. */
  congesJoursAcquis?: number | null;
  /** Jours de congés payés pris [0, 50]. */
  congesJoursPris?: number | null;
  /** Salaire effectivement versé par mois (€ > 0). Distinct du montant dû. */
  rappelSalaireMontantPerverseMensuel?: number | null;
  /** Date de début de la période de rappel — premier mois impayé (ISO). */
  rappelSalairePeriodeDebut?: string | null;
  /** Date de fin de la période de rappel — dernier mois impayé (ISO). */
  rappelSalairePeriodeFin?: string | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `rupture_collective_detection`
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Âge du salarié en années [16, 80] — extractible si pièce d'identité aux pièces. */
  salarieAgeAnnees?: number | null;
  /** Effectif de l'entreprise (PSE) en nb salariés [0, 100000]. */
  pseNombreSalaries?: number | null;
  /** Nombre de licenciements envisagés dans le PSE [0, 100000]. */
  pseNombreLicenciements?: number | null;
  /** Date de signature du protocole transactionnel (ISO YYYY-MM-DD). */
  transactionDateSignature?: string | null;
  /** Montant de l'indemnité transactionnelle (€ > 0). */
  transactionIndemniteMontantEur?: number | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `sante_discrimination_detection`
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Date de l'accident du travail (ISO YYYY-MM-DD) — distincte de dateLicenciement. */
  atDateAccident?: string | null;
  /** Date de première exposition au risque MP (ISO). Distincte de atDateAccident. */
  atDateExposition?: string | null;
  /**
   * Type de décision France Travail contestée — whitelist :
   * REFUS_INSCRIPTION | RADIATION | SUPPRESSION_ARE | REDUCTION_ARE | EXCLUSION_TEMPORAIRE | AUTRE
   */
  areTypeDecision?: string | null;
  /** Montant contesté dans la décision France Travail (€ > 0). */
  areMontantConteste?: number | null;
  /**
   * Motif de discrimination — whitelist :
   * SEXE | AGE | ORIGINE | HANDICAP | RELIGION | ORIENTATION_SEXUELLE | GROSSESSE | ACTIVITES_SYNDICALES | AUTRE
   */
  discriminationMotif?: string | null;
  /**
   * Contexte de l'acte discriminatoire — whitelist :
   * REFUS_EMBAUCHE | LICENCIEMENT | MUTATION | SANCTION_DISCIPLINAIRE | PROMOTION_REFUSEE | REMUNERATION_INFERIEURE | HARCELEMENT | AUTRE
   */
  discriminationContexte?: string | null;
  // -------------------------------------------------------------------------
  // SF-246-21 — sous-objet `procedure_details_detection`
  // FR uniquement — null pour dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Montant de la provision demandée en référé prud'homal (€ > 0). */
  refereMontantProvision?: number | null;
  /** Date du certificat de travail (ISO YYYY-MM-DD). */
  documentsDateCertificatTravail?: string | null;
  /** Date de l'attestation France Travail (ISO YYYY-MM-DD). */
  documentsDateAttestationFranceTravail?: string | null;
  /** Date du solde de tout compte signé (ISO YYYY-MM-DD). */
  documentsDateSoldeToutCompte?: string | null;
  // -------------------------------------------------------------------------
  // SF-246-23 — sous-objet `travail_be_detection` (BELGIQUE uniquement)
  // null pour tout dossier Travail FRANCE.
  // -------------------------------------------------------------------------
  /**
   * BELGIQUE — Date à laquelle l'employeur a eu connaissance du fait constituant
   * le motif grave (ISO YYYY-MM-DD). Point de départ du délai de 3 j ouvrables
   * art. 35 Loi 03/07/1978. Strictement antérieure à dateLicenciement.
   * Pré-fill F-DT-27 motif-grave-be.
   */
  dateConnaissanceFait?: string | null;
  /**
   * BELGIQUE — Date à laquelle l'employeur a notifié les motifs de la rupture
   * au travailleur par lettre recommandée (ISO YYYY-MM-DD). Point d'arrivée du
   * 2e délai de 3 j ouvrables. Strictement postérieure à dateLicenciement.
   * Pré-fill F-DT-27 motif-grave-be.
   */
  dateNotificationMotifs?: string | null;
  /**
   * BELGIQUE — Numéro ou libellé de la commission paritaire applicable (≤ 20 car.,
   * ex. "CP 200", "SCP 200.01"). Concept distinct de conventionCollective (IDCC FR).
   * Pré-fill F-DT-28 avantages-conventionnels-be.
   */
  commissionParitaireBe?: string | null;
  /**
   * BELGIQUE — Jours de travail effectif (ou assimilés) au cours de l'année
   * précédente [0, 365]. Base pécule de vacances simple (Loi 28/06/1971).
   * Pré-fill F-DT-28 avantages-conventionnels-be.
   */
  joursTravaillesAnneePrecedenteBe?: number | null;
  /**
   * BELGIQUE — Jours effectivement prestés depuis le 1er avril de l'exercice
   * courant [0, 365]. Distinct de joursTravaillesAnneePrecedenteBe (année précédente).
   * Pré-fill F-DT-28 avantages-conventionnels-be.
   */
  joursPrestesBe?: number | null;
  /**
   * BELGIQUE — Date à laquelle le travailleur a formellement introduit sa demande
   * de crédit-temps auprès de l'employeur (ISO YYYY-MM-DD). Distincte de dateEntree
   * et de la date d'entrée en vigueur du crédit-temps.
   * Pré-fill F-DT-29 credit-temps-be.
   */
  dateDemandeCreditTemps?: string | null;
  /**
   * F-DT-38 (rupture période d'essai) — flag accident du travail / maladie pro
   * détecté dans le dossier (déclencheur protection rupture pendant arrêt AT/MP).
   * Référencé par `RupturePeriodeEssaiSectionPrefillRules.computeArretAccidentTravail`.
   * Dette frontend résorbée ici (le helper consommait un champ encore non typé).
   */
  atMpDetecte?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-206-01 — sous-objet `abandon_poste_detail` (FRANCE uniquement)
  // Pré-fill F-DT-42 (abandon de poste / présomption de démission).
  // Tous nullables — restent `null` pour un dossier Travail BE (mécanisme
  // franco-français de la loi 21/12/2022).
  // -------------------------------------------------------------------------
  /** Date de présentation de la mise en demeure de reprendre le poste (ISO YYYY-MM-DD). */
  abandonPosteDateMiseEnDemeure?: string | null;
  /** Mode de notification de la MED (LRAR | REMISE_MAIN_PROPRE | AUTRE). */
  abandonPosteModeNotification?: string | null;
  /** Délai accordé par l'employeur au salarié pour reprendre / justifier (en jours). */
  abandonPosteDelaiAccordeJours?: number | null;
  /** Motif d'absence invoqué (AUCUN | MEDICAL | DROIT_RETRAIT | DROIT_GREVE | MODIFICATION_CONTRAT_REFUSEE | DEFAUT_PAIEMENT_SALAIRE | AUTRE). */
  abandonPosteMotifAbsence?: string | null;
  /** Date de reprise du poste ou de justification de l'absence (ISO YYYY-MM-DD). */
  abandonPosteDateReprise?: string | null;
  /** True si la MED mentionne le délai imparti (D.1237-2-1). */
  abandonPosteMedMentionneDelai?: boolean | null;
  /** True si la MED mentionne les conséquences (présomption de démission). */
  abandonPosteMedMentionneConsequences?: boolean | null;
  /** True si reprise ou justification effectivement intervenue dans le délai. */
  abandonPosteRepriseDansDelai?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-218-12 — régime VRP statutaire (FRANCE uniquement)
  // Pré-fill F-DT-104 (VRP : préavis et indemnité de clientèle) + flag pivot
  // CONTEXTUAL. Nullables — restent `null` pour un dossier Travail BE (statut
  // VRP franco-français, art. L.7311-1 et s. CT).
  // -------------------------------------------------------------------------
  /** Moyenne annuelle des commissions des 3 dernières années (assiette indemnité de clientèle, L.7313-13). */
  vrpCommissionsAnnuelles?: number | null;
  /** Flag pivot CONTEXTUAL : true quand l'IA détecte un statut VRP (mentions « VRP », « représentant de commerce », commissions, carte de représentant). */
  vrpStatutDetecte?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-206-03 — sous-objet `conges_payes_arret_maladie_detail` (FRANCE only)
  // Pré-fill F-DT-75 (congés payés acquis pendant arrêt maladie).
  // Tous nullables — restent `null` pour un dossier Travail BE (régime
  // L.3141-5 / L.3141-5-1 CT purement français — loi 22/04/2024 art. 37).
  // -------------------------------------------------------------------------
  /** Type d'arrêt (MALADIE_NON_PROFESSIONNELLE | ACCIDENT_TRAVAIL_MALADIE_PRO). */
  cpArretMaladieType?: string | null;
  /** Nombre de mois cumulés d'arrêt maladie / AT-MP non décomptés par l'employeur. */
  cpArretMaladieNombreMois?: number | null;
  /** True si le salarié est encore en poste (vs sorti, contrat rompu). */
  cpArretMaladieSalarieEnPoste?: boolean | null;
  /** Date de rupture du contrat — requis si salarié sorti pour calculer le délai d'action (ISO YYYY-MM-DD). */
  cpArretMaladieDateRupture?: string | null;
  /** Jours de CP déjà accordés / décomptés par l'employeur (pour calcul du rappel net). */
  cpArretMaladieJoursDejaAccordes?: number | null;
  // -------------------------------------------------------------------------
  // SF-206-05 — sous-objet `prise_acte_detail` (FRANCE only)
  // Pré-fill F-DT-39 (prise d'acte de la rupture aux torts de l'employeur).
  // Tous nullables — restent `null` pour un dossier Travail BE (la prise
  // d'acte CPH avec effets licenciement / démission est un mécanisme
  // franco-français — Cass. soc. 25/06/2003 n°01-42.679).
  // -------------------------------------------------------------------------
  /** Défaut ou retard de paiement du salaire (Cass. soc. 20/03/2013 n°11-26.770). */
  priseActeDefautPaiementSalaire?: boolean | null;
  /** Montant cumulé d'impayés en € (seuil significatif backend : 1500 €). */
  priseActeMontantImpayes?: number | null;
  /** Harcèlement moral ou sexuel — bascule LICENCIEMENT_NUL (L.1152-3 / L.1153-4 CT). */
  priseActeHarcelement?: boolean | null;
  /** Manquement à l'obligation de sécurité (L.4121-1 CT). */
  priseActeManquementSecurite?: boolean | null;
  /** Modification unilatérale d'un élément essentiel du contrat. */
  priseActeModificationContrat?: boolean | null;
  /** Déclassement professionnel / mise à l'écart. */
  priseActeDeclassement?: boolean | null;
  /** Discrimination (L.1132-1 CT) — bascule LICENCIEMENT_NUL (L.1132-4 CT). */
  priseActeDiscrimination?: boolean | null;
  /** Non-paiement des heures supplémentaires (L.3171-4 CT). */
  priseActeHeuresSupNonPayees?: boolean | null;
  /** Non-respect des durées maximales / temps de repos (L.3121-18 s. CT). */
  priseActeNonRespectRepos?: boolean | null;
  /** Griefs actuels et persistants (vs anciens régularisés — Cass. soc. 26/03/2014 n°12-23.634). */
  priseActeGriefsPersistants?: boolean | null;
  /** Grief rendant impossible la poursuite du contrat (critère central Cass. soc. 26/03/2014 n°12-21.372). */
  priseActeGriefImpossiblePoursuite?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-02 — sous-objet `faute_grave_detail` (FRANCE only)
  // Pré-fill F-DT-36 (licenciement pour faute grave / faute lourde — L.1234-1
  // s. CT, Cass. soc. 18/06/2013 n°11-14.393). Tous nullables — restent `null`
  // pour un dossier Travail BE (distinction faute grave / faute lourde
  // strictement française).
  // -------------------------------------------------------------------------
  /** Résumé factuel des faits reprochés (≤ 500 caractères). */
  fauteGraveFaitsReproches?: string | null;
  /** Dates des faits reprochés au format ISO YYYY-MM-DD (tableau, potentiellement vide). */
  fauteGraveDatesFaits?: string[] | null;
  /** Qualification retenue par l'employeur — `FAUTE_SIMPLE` | `FAUTE_GRAVE` | `FAUTE_LOURDE`. */
  fauteGraveQualificationEmployeur?: string | null;
  /** Intention de nuire alléguée par l'employeur (critère distinctif faute lourde — L.1234-9 CT). */
  fauteGraveIntentionNuireAlleeguee?: boolean | null;
  /** Ancienneté du salarié en mois entiers (0–600). */
  fauteGraveAncienneteMois?: number | null;
  /** Salaire brut mensuel de référence en euros (> 0). */
  fauteGraveSalaireMensuelBrut?: number | null;
  // -------------------------------------------------------------------------
  // SF-212-03 — sous-objet `forfait_jours_detail` (FRANCE only)
  // Pré-fill F-DT-50 (forfait jours — validité et rappel HS).
  // Tous nullables — restent `null` pour un dossier Travail BE (le régime
  // forfait jours art. L.3121-58+ CT avec exigences post-Cass. soc. 29/06/2011
  // est strictement français).
  // -------------------------------------------------------------------------
  /** Existence d'un accord collectif autorisant le forfait (L. 3121-63 CT). */
  forfaitJoursAccordCollectifExiste?: boolean | null;
  /** Entretien annuel charge de travail formalisé (L. 3121-65 CT). */
  forfaitJoursEntretienAnnuelRealise?: boolean | null;
  /** Document de contrôle mensuel des jours travaillés (L. 3121-66 CT). */
  forfaitJoursDocumentControle?: boolean | null;
  /** Catégorie cadre autonome / ETAM maîtrisant son temps (L. 3121-58 CT). */
  forfaitJoursCategorieAutonome?: boolean | null;
  /** Nombre de jours du forfait annuel (entier 0–235). */
  forfaitJoursNbJours?: number | null;
  // SF-212-06 — sous-objet `transfert_entreprise_detail` (FRANCE only)
  // Pré-fill F-DT-72 (transfert d'entreprise — maintien des contrats
  // L. 1224-1 CT, Cass. soc. 18/07/2000 n°98-46.071, Directive 2001/23/CE).
  // Tous nullables — restent `null` pour un dossier Travail BE (maintien
  // des contrats lors d'un transfert en BE relève de la CCT 32bis distincte).
  // -------------------------------------------------------------------------
  /** Type de transfert détecté — `CESSION` | `FUSION` | `APPORT_PARTIEL_ACTIF` | `EXTERNALISATION` | `REPRISE_ACTIVITE` | `AUTRE`. */
  transfertTypeTransfert?: string | null;
  /** EEA identifiée avant le transfert (Cass. soc. 18/07/2000 — ensemble organisé de personnes et d'éléments). */
  transfertEeaIdentifiee?: boolean | null;
  /** Activité économique poursuivie après le transfert (continuité d'exploitation). */
  transfertActivitePreservee?: boolean | null;
  /** Licenciements prononcés par le cédant peu avant le transfert. */
  transfertLicenciementsPreTransfert?: boolean | null;
  /** Date du transfert effectif au format ISO YYYY-MM-DD. */
  transfertDateTransfert?: string | null;
  // -------------------------------------------------------------------------
  // SF-212-07 — sous-objet `csp_detail` (FRANCE only)
  // Pré-fill F-DT-44 (CSP/CRP — conformité de la proposition L. 1233-65 à
  // L. 1233-70 CT ; ANI CSP 19/07/2011 ; DARES).
  // Tous nullables — restent `null` pour un dossier Travail BE (le régime
  // équivalent BE est l'outplacement obligatoire CCT 82, mécanisme distinct).
  // -------------------------------------------------------------------------
  /** Effectif total de l'entreprise au moment du licenciement économique (entier 0–100 000). */
  cspEffectifEntreprise?: number | null;
  /** Proposition de CSP effectivement remise/notifiée au salarié. */
  cspProposeDetail?: boolean | null;
  /** Document d'information CSP remis au salarié (ANI CSP 19/07/2011). */
  cspDocumentRemis?: boolean | null;
  /** Date de remise du document d'information au format ISO YYYY-MM-DD. */
  cspDateRemise?: string | null;
  /** Adhésion du salarié au CSP — `true` = accepte, `false` = refuse, `null` = inconnu. */
  cspAdhesion?: boolean | null;
  /** Salaire mensuel brut de référence pour le calcul ASP (€ > 0). */
  cspSalaireMensuelBrut?: number | null;
  // -------------------------------------------------------------------------
  // SF-212-09 — sous-objet `faute_inexcusable_detail` (FRANCE only)
  // Pré-fill F-DT-91 (faute inexcusable de l'employeur — L. 452-1 à L. 452-5
  // CSS ; Cass. ass. plén. 24/06/2005 ; L. 4121-1 CT).
  // Tous nullables — restent `null` pour un dossier Travail BE (régimes
  // faute grave / intentionnelle BE distincts).
  // -------------------------------------------------------------------------
  /** Conscience du danger établie chez l'employeur (1re condition Cass. ass. plén. 24/06/2005). */
  fauteInexcusableConscienceDanger?: boolean | null;
  /** Signalement antérieur du danger (salarié, CSE, médecin du travail, inspection). */
  fauteInexcusableSignalementPrior?: boolean | null;
  /** Mesures de prévention prises (L. 4121-1 CT). */
  fauteInexcusableMesuresPrevention?: boolean | null;
  /** Taux d'IPP reconnu par la CPAM en % (entier 0–100). */
  fauteInexcusableTauxIpp?: number | null;
  // -------------------------------------------------------------------------
  // SF-212-25 — sous-objet `lanceur_alerte_detail` (FRANCE only)
  // Pré-fill F-DT-61 (protection du lanceur d'alerte — L. 1132-3-3 CT ;
  // loi Sapin II n° 2016-1691 ; loi Waserman n° 2022-401 du 21/03/2022).
  // Tous nullables — restent `null` pour un dossier Travail BE (régime
  // belge distinct via loi du 28/11/2022).
  // -------------------------------------------------------------------------
  /** Nature du signalement (CRIME_DELIT, VIOLATION_DROIT_UE, MENACE_INTERET_GENERAL, AUTRE). */
  lanceurAlerteNatureSignalement?: string | null;
  /** Procédure utilisée (INTERNE, EXTERNE, DIVULGATION_PUBLIQUE). */
  lanceurAlerteProcedure?: string | null;
  /** Mesure de représailles détectée (L. 1132-3-3 CT). */
  lanceurAlerteMesureRepresaille?: boolean | null;
  /** Nature de la mesure (LICENCIEMENT, SANCTION, MESURE_DISCRIMINATOIRE, AUTRE, AUCUNE). */
  lanceurAlerteNatureMesure?: string | null;
  // -------------------------------------------------------------------------
  // SF-212-11 — sous-objet `modification_contrat_detail` (FRANCE only)
  // Pré-fill F-DT-70 (modification du contrat — refus du salarié — Cass. soc.
  // distinction modification / changement des conditions de travail ;
  // L. 1222-6 CT — modification pour motif économique).
  // Tous nullables — restent `null` pour un dossier Travail BE (régime de
  // l'acte équipollent à rupture distinct).
  // -------------------------------------------------------------------------
  /** Élément du contrat modifié (REMUNERATION, QUALIFICATION, DUREE_TRAVAIL, LIEU_TRAVAIL, HORAIRES, TACHES, AUTRE). */
  modifContratElementModifie?: string | null;
  /** Élément modifié explicitement contractualisé (clause écrite). */
  modifContratContractualise?: boolean | null;
  /** Modification proposée pour un motif économique (L. 1222-6 CT). */
  modifContratMotifEco?: boolean | null;
  /** Notification écrite L. 1222-6 (LRAR) envoyée au salarié. */
  modifContratNotifEcrite?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-13 — sous-objet `mutation_mobilite_detail` (FRANCE only)
  // Pré-fill F-DT-71 (mutation — validité de la clause de mobilité — Cass.
  // soc. constante : zone précise, intérêt légitime, délai de prévenance,
  // motif professionnel, situation familiale).
  // Tous nullables — restent `null` pour un dossier Travail BE (régime de
  // modification d'un élément essentiel du contrat, Loi du 03/07/1978).
  // -------------------------------------------------------------------------
  /** Clause de mobilité contractuelle présente dans le contrat (ou avenant signé). */
  mutationClausePresente?: boolean | null;
  /** Zone géographique de la clause définie avec précision (Cass. soc. 07/06/2006). */
  mutationZoneGeographiquePrecise?: boolean | null;
  /** Intérêt légitime de l'employeur démontré (Cass. soc. 23/02/2005). */
  mutationInteretLegitimeEmployeur?: boolean | null;
  /** Délai de prévenance accordé en semaines (Cass. soc. 03/03/2010). */
  mutationDelaiPrevenanceSemaines?: number | null;
  /** Situation familiale du salarié contraignante (Cass. soc. 14/10/2008). */
  mutationSituationFamilialeContraingnante?: boolean | null;
  /** Motif de la mutation rattaché à un besoin professionnel objectif. */
  mutationMotifProfessionnel?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-15 — sous-objet `teletravail_detail` (FRANCE only)
  // Pré-fill F-DT-82 (télétravail — conformité et litige — L. 1222-9 à
  // L. 1222-11 CT ; ANI télétravail du 26/11/2020).
  // Tous nullables — restent `null` pour un dossier Travail BE (CCT n°85 et
  // Loi du 05/03/2017 — régime distinct).
  // -------------------------------------------------------------------------
  /** Cadre juridique du télétravail (ACCORD_COLLECTIF, CHARTE_UNILATERALE, ACCORD_INDIVIDUEL, AUCUN). */
  teletravailCadre?: string | null;
  /** Double volontariat employeur + salarié documenté (L. 1222-9 al. 1). */
  teletravailDoubleVolontariat?: boolean | null;
  /** Indemnité d'occupation / remboursement de frais versée (ANI 2020 art. 6.2). */
  teletravailIndemniteVersee?: boolean | null;
  /** Montant journalier de l'indemnité d'occupation en euros. */
  teletravailMontantIndemniteJournalier?: number | null;
  /** Accident survenu à domicile pendant la plage de télétravail (L. 1222-9 al. 4). */
  teletravailAccidentDomicile?: boolean | null;
  /** Retour au bureau imposé unilatéralement sans accord ni délai de prévenance. */
  teletravailRetourBureauImpose?: boolean | null;
  /** Refus de télétravailler invoqué comme cause de licenciement (L. 1222-9 al. 6 interdit). */
  teletravailRefusCauseIncrimination?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-19 — sous-objet `mise_a_pied_detail` (FRANCE only)
  // Pré-fill F-DT-48 (mise à pied disciplinaire — régularité — L. 1331-1 CT ;
  // L. 1332-1 à L. 1332-4 CT ; Cass. soc. 26/10/2010 n°09-42.740 ;
  // jurisprudence constante Cass. soc. — interdiction de la double sanction).
  // Tous nullables — restent `null` pour un dossier Travail BE (régime
  // disciplinaire distinct, Loi du 03/07/1978 et CCT applicables).
  // -------------------------------------------------------------------------
  /** Nature de la mise à pied — DISCIPLINAIRE / CONSERVATOIRE / INCONNUE. */
  mapDisciplinaireNature?: string | null;
  /** Procédure d'entretien préalable suivie (L. 1332-1 à L. 1332-3 CT). */
  mapDisciplinaireProcedureSuivie?: boolean | null;
  /** Prescription des faits respectée — sanction < 2 mois (L. 1332-4 CT). */
  mapDisciplinairePrescriptionFaute?: boolean | null;
  /** Durée prévue par le règlement intérieur ou l'accord collectif (L. 1311-2 CT). */
  mapDisciplinaireDureeRi?: boolean | null;
  /** Durée de la mise à pied en jours calendaires. */
  mapDisciplinaireDureeJours?: number | null;
  /** Salaire effectivement suspendu pendant la période. */
  mapDisciplinaireSalaireSuspendu?: boolean | null;
  /** Sanction antérieure pour les mêmes faits — double sanction interdite. */
  mapDisciplinaireSanctionsAnterieures?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-23 — sous-objet `egalite_salariale_detail` (FRANCE only)
  // Pré-fill F-DT-56 (égalité salariale femmes/hommes — L. 1142-7 à L. 1142-10 CT ;
  // L. 1144-1 CT charge de la preuve aménagée ; L. 3221-2 CT à travail égal
  // salaire égal ; loi 05/09/2018 « avenir professionnel »). Regroupé en sous-objet
  // imbriqué pour rester sous la limite JVM 255 slots du constructeur canonical
  // du record TravailExtractedData côté backend (saturé par les vagues F-212).
  // Tous nullables — reste `null` pour un dossier Travail BE.
  // -------------------------------------------------------------------------
  /** Sous-objet IA pour pré-fill F-DT-56 (FRANCE only) — null si non documenté. */
  egaliteSalarialeDetail?: EgaliteSalarialeDetail | null;
  // -------------------------------------------------------------------------
  // F-256 SF-212-35 — sous-objet `pdv_rcc_detail` (FRANCE only)
  // Pré-fill F-DT-46 (PDV / RCC conformité — L. 1237-17 à L. 1237-19-14 CT ;
  // ord. n°2017-1387 du 22/09/2017 ; décret 2017-1718 du 20/12/2017).
  // Désormais projeté côté backend par F-256 (refactor TravailExtractedData
  // en sous-records, slot libéré).
  // -------------------------------------------------------------------------
  /** Sous-objet IA pour pré-fill F-DT-46 (FRANCE only). */
  pdvRccDetail?: PdvRccDetail | null;
  // -------------------------------------------------------------------------
  // F-256 SF-212-17 — sous-objet `rupture_anticipee_cdd_detail` (FRANCE only)
  // Pré-fill F-DT-43 (rupture anticipée du CDD — L. 1243-1 à L. 1243-4 CT).
  // Réactivé par F-256 (slot libéré sur le constructeur canonical de
  // TravailExtractedData). Le flag top-level `ruptureAnticipeeCddDetectee`
  // reste exposé pour le déclenchement F-IA-04.
  // -------------------------------------------------------------------------
  /** Sous-objet IA pour pré-fill F-DT-43 (FRANCE only). */
  ruptureAnticipeeCddDetail?: RuptureAnticipeeCddDetail | null;
  // -------------------------------------------------------------------------
  // F-256 SF-212-21 — sous-objet `demission_equivoque_detail` (FRANCE only)
  // Pré-fill F-DT-41 (démission validité équivoque — Cass. soc. 09/05/2007).
  // Réactivé par F-256 (slot libéré). Le flag top-level `demissionEquivoquePressentie`
  // est désormais aussi projeté sur le record Java (auparavant lu depuis raw JsonNode).
  // -------------------------------------------------------------------------
  /** Sous-objet IA pour pré-fill F-DT-41 (FRANCE only). */
  demissionEquivoqueDetail?: DemissionEquivoqueDetail | null;
  /** F-256 SF-212-21 — flag F-205 déclenchant F-DT-41 démission validité équivoque (FRANCE only). */
  demissionEquivoquePressentie?: boolean | null;
  /** F-256 SF-212-35 — flag F-205 déclenchant F-DT-46 PDV/RCC conformité (FRANCE only). */
  pdvRccEnvisage?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-29 — sous-objet `conge_maternite_paternite_detail` (FRANCE only)
  // Pré-fill F-DT-77 (congé maternité / paternité — L. 1225-1 à L. 1225-40
  // CT ; L. 331-3 CSS ; loi du 16/03/2021 sur le congé paternité 25 jours).
  // -------------------------------------------------------------------------
  /** Sous-objet IA pour pré-fill F-DT-77 (FRANCE only). */
  congeMaternitePaterniteDetail?: CongeMaternitePaterniteDetail | null;
  /** SF-212-29 — flag F-205 déclenchant F-DT-77 congé maternité / paternité (FRANCE only). */
  congeMaternitePaterniteDetecte?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-27 — sous-objet `burnout_detail` (FRANCE only)
  // Pré-fill F-DT-64 (burn-out — reconnaissance MP hors tableau — L. 461-1
  // al. 4 et 5 CSS ; CRRMP ; circulaire DGT 2016/01). Tableau 57 n'inclut
  // pas le burn-out, d'où la voie hors tableau obligatoire (taux IPP ≥ 25 %).
  // @JsonUnwrapped côté backend — JSON HTTP plat. Tous nullables — restent
  // `null` pour un dossier Travail BE (régime Fedris distinct).
  // -------------------------------------------------------------------------
  /** Diagnostic de burn-out posé (certificat / expertise médicale). */
  burnoutDiagnostic?: boolean | null;
  /** Taux d'IPP estimé (entier %, doit être ≥ 25 pour ouvrir CRRMP). */
  burnoutTauxIpp?: number | null;
  /** Surcharge / manquements employeur obligation sécurité L. 4121-1 documentés. */
  burnoutSurchargeDocumentee?: boolean | null;
  /** Arrêts maladie multiples ou prolongés en lien avec le travail. */
  burnoutArretsMaladie?: boolean | null;
  /** SF-212-27 — flag F-205 déclenchant F-DT-64 burn-out reconnaissance MP (FRANCE only). */
  burnoutDetecte?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-31 — sous-objet `elections_cse_detail` (FRANCE only)
  // Pré-fill F-DT-65 (élections CSE — conformité procédure — L. 2314-1 à
  // L. 2314-37 CT ; R. 2314-1+ CT ; ordonnance n°2017-1386 du 22/09/2017
  // instituant le CSE ; L. 2314-32 CT — délai contestation 15 jours).
  // @JsonUnwrapped côté backend — JSON HTTP plat. Tous nullables — restent
  // `null` pour un dossier Travail BE (régime conseil d'entreprise distinct).
  // -------------------------------------------------------------------------
  /** Date d'élection ISO YYYY-MM-DD — base du calcul du délai de contestation. */
  electionCseDateElection?: string | null;
  /** PAP négocié avec les organisations syndicales (L. 2314-6 CT). */
  electionCsePapNegocie?: boolean | null;
  /** Collèges électoraux conformes (au moins 2 — L. 2314-11 CT). */
  electionCseCollegesConformes?: boolean | null;
  /** Résultats contestés devant le tribunal judiciaire (L. 2314-32 CT). */
  electionCseResultatsContestes?: boolean | null;
  /** SF-212-31 — flag F-205 déclenchant F-DT-65 élections CSE conformité (FRANCE only). */
  electionCseDetectee?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-33 — sous-objet `temps_partiel_requalification_detail` (FRANCE only)
  // Pré-fill F-DT-49 (temps partiel — requalification en temps plein —
  // L. 3123-1 à L. 3123-20 CT ; L. 3123-6 mentions obligatoires ; L. 3123-9
  // plafond heures complémentaires ; L. 3245-1 prescription rappel salaire ;
  // Cass. soc. 22/01/1992 présomption de temps complet réfragable).
  // @JsonUnwrapped côté backend — JSON HTTP plat. Tous nullables — restent
  // `null` pour un dossier Travail BE (régime Loi 03/07/1978 + CCT n°35 distinct).
  // -------------------------------------------------------------------------
  /** Durée hebdomadaire contractuelle en heures (ex. 24, 28). */
  tempsPartielDureeContractuelle?: number | null;
  /** True si le contrat écrit mentionne la durée (L. 3123-6 CT). */
  tempsPartielMentionsDuree?: boolean | null;
  /** True si le contrat écrit mentionne la répartition jours/semaines (L. 3123-6 CT). */
  tempsPartielMentionsRepartition?: boolean | null;
  /** Heures complémentaires effectuées en moyenne par semaine. */
  tempsPartielHCMoyenne?: number | null;
  /** SF-212-33 — flag F-205 déclenchant F-DT-49 temps partiel — requalification (FRANCE only). */
  tempsPartielRequalificationEnvisagee?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-212-37 — sous-objet `conciliation_cph_detail` (FRANCE only)
  // Pré-fill F-DT-84 (conciliation CPH — Bureau de Conciliation et
  // d'Orientation, R. 1454-7 à R. 1454-12 CT ; L. 1235-1 al. 3 CT — barème
  // transactions BCA ; L. 1411-1 CT compétence CPH ; R. 1454-12 CT
  // homologation PV exécutoire). F-212 19/19 — dernier outil livré.
  // @JsonUnwrapped côté backend — JSON HTTP plat. Tous nullables — restent
  // `null` pour un dossier Travail BE (régime tribunal du travail + chambre
  // de conciliation, Code judiciaire belge art. 734 et s. — juridiquement
  // distinct, hors périmètre).
  // -------------------------------------------------------------------------
  /** Ancienneté du salarié en mois — sert au calcul du palier BCA (L. 1235-1 al. 3 CT). */
  conciliationCphAncienneteMois?: number | null;
  /** Salaire mensuel brut de référence en € — base du montant minimum BCA. */
  conciliationCphSalaire?: number | null;
  /** Montant total des demandes du salarié en € — base de comparaison BCA vs Macron. */
  conciliationCphMontantDemandes?: number | null;
  /** SF-212-37 — flag F-205 déclenchant F-DT-84 conciliation CPH BCA (FRANCE only). F-212 19/19. */
  conciliationCphEnvisagee?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-206-07 — sous-objet `resiliation_judiciaire_detail` (FRANCE only)
  // Pré-fill F-DT-40 (résiliation judiciaire du contrat aux torts de l'employeur).
  // Tous nullables — restent `null` pour un dossier Travail BE (la résiliation
  // judiciaire CPH avec effets licenciement sans cause à la date du jugement
  // est un mécanisme franco-français — Cass. soc. 16/03/1989 ; Cass. soc.
  // 20/01/1998 ; art. L.1411-1 CT ; art. 1224, 1227-1228 C. civ.).
  // -------------------------------------------------------------------------
  /** Défaut ou retard de paiement du salaire (Cass. soc. 20/03/2013 n°11-26.770). */
  resiliationJudDefautPaiementSalaire?: boolean | null;
  /** Montant cumulé d'impayés en € (seuil significatif backend : 1500 €). */
  resiliationJudMontantImpayes?: number | null;
  /** Harcèlement moral ou sexuel (L.1152-1 / L.1153-1 CT). */
  resiliationJudHarcelement?: boolean | null;
  /** Manquement à l'obligation de sécurité (L.4121-1 CT). */
  resiliationJudManquementSecurite?: boolean | null;
  /** Modification unilatérale d'un élément essentiel du contrat. */
  resiliationJudModificationContrat?: boolean | null;
  /** Déclassement professionnel / mise à l'écart. */
  resiliationJudDeclassement?: boolean | null;
  /** Discrimination (L.1132-1 CT). */
  resiliationJudDiscrimination?: boolean | null;
  /** Non-paiement des heures supplémentaires (L.3171-4 CT). */
  resiliationJudHeuresSupNonPayees?: boolean | null;
  /** Non-respect des durées maximales / temps de repos (L.3121-18 s. CT). */
  resiliationJudNonRespectRepos?: boolean | null;
  /** Manquements persistants au jour de la demande (Cass. soc. 30/03/2010 — critère central). */
  resiliationJudManquementsPersistants?: boolean | null;
  /** Salarié toujours en poste (vs sorti — rappel : la voie suppose le maintien du contrat). */
  resiliationJudSalarieEnPoste?: boolean | null;
  /** Licenciement intervenu en cours d'instance (Cass. soc. 21/12/2006 n°05-42.251 — bascule date d'effet). */
  resiliationJudLicenciementEnCours?: boolean | null;
  // -------------------------------------------------------------------------
  // SF-246-29 — sous-objet `rupture_periode_essai_detail` (FRANCE uniquement)
  // Pré-fill exhaustif F-DT-38 (rupture de période d'essai — qualification
  // régulière / abusive / nulle / illégale, L.1221-19 à L.1221-25 CT).
  // Tous nullables — restent `null` pour un dossier Travail BE (la période
  // d'essai a été abolie en droit belge par la Loi 26/12/2013 statut unique).
  // Préfixe `rpe…` pour éviter toute collision avec les champs existants
  // (grossesseAuMomentRupture, atMpDetecte, etc.).
  // -------------------------------------------------------------------------
  /** Catégorie socio-professionnelle — OUVRIER_EMPLOYE | AGENT_MAITRISE_TECHNICIEN | CADRE. Détermine durée légale max (L.1221-19). */
  rpeCategorieSocioProfessionnelle?: string | null;
  /** Durée du CDD en mois [0, 36] — détermine l'essai max selon le ratio L.1242-10. Null pour CDI / intérim. */
  rpeDureeCddMois?: number | null;
  /** Durée de la période d'essai PRÉVUE AU CONTRAT en mois [0, 24]. */
  rpeDureePeriodeEssaiMois?: number | null;
  /** Renouvellement de la période d'essai initialement notifié par l'employeur. */
  rpeRenouvellementInvoque?: boolean | null;
  /** L'accord de branche / CCN applicable PRÉVOIT le renouvellement (pré-requis L.1221-21). */
  rpeAccordBrancheRenouvellement?: boolean | null;
  /** Le salarié a expressément ACCEPTÉ PAR ÉCRIT le renouvellement (L.1221-23 — accord tacite refusé). */
  rpeAccordEcritSalarieRenouvellement?: boolean | null;
  /** Auteur de la rupture — EMPLOYEUR | SALARIE. */
  rpeAuteurRupture?: string | null;
  /** Délai de prévenance EFFECTIVEMENT respecté avant la prise d'effet de la rupture (jours [0, 30]). */
  rpeDelaiPrevenanceJours?: number | null;
  /** Motif rattaché à l'évaluation des aptitudes professionnelles (Cass. soc. 20/11/2007 — finalité de l'essai). */
  rpeMotifLieCompetences?: boolean | null;
  /** Motif réel économique / organisationnel (caractérise un détournement de la finalité de l'essai → abus). */
  rpeMotifEconomique?: boolean | null;
  /** Description (≤ 500 car.) d'une atteinte à une liberté fondamentale → caractérise verdict NULLE. */
  rpeAtteinteLiberteFondamentale?: string | null;
  /** Lettre de rupture écrite et formellement motivée présente aux pièces. */
  rpeLettreRuptureMotivee?: boolean | null;
  /** Motifs énoncés dans la lettre étayés par des pièces (évaluations, rapports d'incidents, courriers). */
  rpeMotifsAveresParPieces?: boolean | null;
  /** CCN applicable prévoit des dispositions plus favorables effectivement respectées par la rupture. */
  rpeCcnPlusFavorableRespectee?: boolean | null;
  // SF-252-01 — 7 nouveaux champs pour les 5 protections nullité additionnelles
  // de F-DT-38 (audit 2026-05-20). FRANCE uniquement, nullables.
  /** Salarié bénéficiant d'un statut protecteur L.2411-1 (élu CSE / DS / etc.). */
  rpeSalarieProtege?: boolean | null;
  /** Autorisation préalable de l'inspection du travail obtenue (L.2411-1 et s.). */
  rpeAutorisationInspectionTravail?: boolean | null;
  /** Lanceur d'alerte au sens de L.1132-3-3 (loi Sapin II / Waserman). */
  rpeLanceurAlerte?: boolean | null;
  /** Témoin ou victime de harcèlement (L.1132-3-1 / L.1152-2 / L.1153-2-3). */
  rpeTemoinHarcelement?: boolean | null;
  /** Exercice du droit de retrait L.4131-3 (danger grave et imminent). */
  rpeDroitRetraitExerce?: boolean | null;
  /** Grossesse déclarée à l'employeur APRÈS la rupture (L.1225-5). */
  rpeGrossesseDeclareePostRupture?: boolean | null;
  /** Date ISO YYYY-MM-DD de notification de la grossesse à l'employeur (L.1225-5). */
  rpeDateNotificationGrossesse?: string | null;
  // SF-218-01 : 2 champs IA pour pré-fill F-DT-86 appel CPH cour d'appel
  // (Travail FR uniquement, nullables). Le délai d'appel d'un mois (art. 538 CPC ;
  // R. 1461-1 CPC) court à compter de la notification du jugement CPH. Régime BE
  // distinct — ces champs restent null pour un dossier travail belge.
  /** SF-218-01 : date ISO YYYY-MM-DD de notification du jugement CPH (point de départ du délai d'appel). */
  dateNotificationJugement?: string | null;
  /** SF-218-01 : flag F-205 — déclenche F-DT-86 appel CPH cour d'appel (FR). True si jugement prud'homal + intention d'appel. */
  appelCphEnvisage?: boolean | null;
  // SF-218-04 : 3 champs IA pour l'outil F-DT-88 exécution du jugement CPH / AGS
  // (Travail FR uniquement, nullables). Exécution forcée d'un jugement prud'homal
  // (art. 514 CPC ; R. 1454-28 CPC) et relais garantie AGS quand l'employeur est en
  // procédure collective (L. 3253-6 et s.). Régime BE distinct — ces champs restent
  // null pour un dossier travail belge.
  /** SF-218-04 : montant total des condamnations prononcées par le jugement CPH (pré-fill montantCondamnation F-DT-88). */
  montantCondamnationCph?: number | null;
  /** SF-218-04 : situation détectée de l'employeur (IN_BONIS | REDRESSEMENT | LIQUIDATION) — pré-fill situationEmployeur F-DT-88. */
  situationEmployeurDetectee?: 'IN_BONIS' | 'REDRESSEMENT' | 'LIQUIDATION' | null;
  /** SF-218-04 : flag F-205 — déclenche F-DT-88 exécution jugement CPH / AGS (FR). True si jugement CPH favorable + difficulté d'exécution / procédure collective. */
  executionJugementCphEnvisagee?: boolean | null;
  // SF-218-06 : 2 champs IA pour l'outil F-DT-87 pourvoi en cassation chambre
  // sociale (Travail FR uniquement, nullables). Le délai de pourvoi de 2 mois
  // (art. 612 CPC) court à compter de la notification de l'arrêt de la Cour
  // d'appel. Régime BE distinct — ces champs restent null pour un dossier
  // travail belge.
  /** SF-218-06 : date ISO YYYY-MM-DD de notification de l'arrêt de la Cour d'appel (point de départ du délai de pourvoi de 2 mois, art. 612 CPC — pré-fill dateNotificationArret F-DT-87). */
  dateNotificationArretAppel?: string | null;
  /** SF-218-06 : flag F-205 — déclenche F-DT-87 pourvoi cassation soc (FR). True si arrêt de Cour d'appel défavorable + intention de pourvoi. FLAG de visibilité (pas un champ du formulaire). */
  pourvoiCassationSocEnvisage?: boolean | null;
  // SF-218-08 : 2 champs IA pour l'outil F-DT-89 saisie sur rémunération
  // (quotité saisissable, Travail FR uniquement, nullables). Le barème par
  // tranches (R. 3252-2) et la majoration par personne à charge (R. 3252-3)
  // relèvent du droit français. Régime BE distinct — ces champs restent null
  // pour un dossier travail belge.
  /** SF-218-08 : nombre de personnes à charge du débiteur saisi (majore les seuils du barème R. 3252-3 — pré-fill nombrePersonnesACharge F-DT-89). */
  nombrePersonnesACharge?: number | null;
  /** SF-218-08 : flag F-205 — déclenche F-DT-89 saisie sur rémunération (FR). True si mention « saisie sur salaire », « quotité saisissable », « titre exécutoire », « commissaire de justice ». FLAG de visibilité (pas un champ du formulaire). */
  saisieRemunerationDetectee?: boolean | null;
  // SF-218-10 : 3 champs IA pour l'outil F-DT-90 action de groupe en discrimination
  // (recevabilité, Travail FR uniquement, nullables). L'action de groupe en
  // discrimination (L. 1134-7 et s., loi J21 du 18/11/2016) relève du droit
  // français — ces champs restent null pour un dossier travail belge.
  /** SF-218-10 : date de la mise en demeure de l'employeur préalable à l'action de groupe (YYYY-MM-DD — pré-fill dateMiseEnDemeure F-DT-90, point de départ du délai de carence de 6 mois L. 1134-9). */
  dateMiseEnDemeureDiscrimination?: string | null;
  /** SF-218-10 : motif de discrimination pressenti parmi les critères L. 1132-1 (best-effort — pré-fill motifDiscrimination F-DT-90). */
  motifDiscrimination?: 'ORIGINE' | 'SEXE' | 'AGE' | 'HANDICAP' | 'ETAT_SANTE'
    | 'GROSSESSE' | 'ACTIVITE_SYNDICALE' | 'RELIGION' | 'ORIENTATION_SEXUELLE' | 'AUTRE' | null;
  /** SF-218-10 : flag F-205 — déclenche F-DT-90 action de groupe discrimination (FR). True si mention « action de groupe », « discrimination systémique », « plusieurs salariés », « organisation syndicale / association ». FLAG de visibilité (pas un champ du formulaire). */
  actionGroupeDiscriminationEnvisagee?: boolean | null;
}

/** SF-155-04 : agrégat heures sup (totaux déclarés 25 % / 50 % / hors contingent). */
export interface HeuresSupMentionnees {
  totalDeclarees25pct?: number | null;
  totalDeclarees50pct?: number | null;
  horsContingent?: number | null;
}

/**
 * SF-212-23 — sous-objet IA pour l'outil F-DT-56 (égalité salariale
 * femmes/hommes, FRANCE). Tous champs nullables ; null implique pas de
 * données IA pour la projection sur le formulaire UI.
 */
export interface EgaliteSalarialeDetail {
  /** Sexe du salarié — FEMME | HOMME. */
  sexeSalarie?: string | null;
  /** Salaire mensuel brut en euros. */
  salaireBrut?: number | null;
  /** Ancienneté dans l'entreprise en mois. */
  anciennete?: number | null;
  /** Écart en pourcentage avec les comparants identifiés (0 à 100). */
  ecartPourcentage?: number | null;
}

/**
 * SF-212-35 / F-256 — sous-objet IA pour l'outil F-DT-46 (PDV / RCC conformité,
 * FRANCE — L. 1237-17 à L. 1237-19-14 CT ; ord. n°2017-1387 du 22/09/2017).
 * Tous champs nullables ; null implique pas de données IA pour la projection
 * sur le formulaire UI. Désormais projeté côté backend par F-256 (refactor
 * TravailExtractedData en sous-records — slot libéré).
 */
export interface PdvRccDetail {
  /** Type de dispositif — RCC | PDV. */
  pdvRccTypeDispositif?: string | null;
  /** Accord majoritaire signé (≥ 50 % suffrages) — L. 1237-19-1 CT. */
  pdvRccAccordMajoritaire?: boolean | null;
  /** Validation DREETS obtenue — L. 1237-19-3 CT. */
  pdvRccValidationDREETS?: boolean | null;
  /** Indemnités ≥ légales — L. 1237-19-1 al. 5 CT. */
  pdvRccIndemnitesLegales?: boolean | null;
}

/**
 * SF-212-29 — sous-objet IA pour l'outil F-DT-77 (congé maternité /
 * paternité, FRANCE — L. 1225-1 à L. 1225-40 CT ; L. 331-3 CSS ; loi du
 * 16/03/2021). Tous champs nullables ; null implique pas de données IA
 * pour la projection sur le formulaire UI.
 */
export interface CongeMaternitePaterniteDetail {
  /** Type de congé — MATERNITE | PATERNITE. */
  congeMaternitePaterniteType?: string | null;
  /** Rang de l'enfant (1=premier, 2=deuxième, 3=troisième+) — L. 1225-17 CT. */
  congeMaterniteRangEnfant?: number | null;
  /** Naissance multiple (jumeaux, triplés+) — surcharge la durée. */
  congeMaterniteNaissanceMultiple?: boolean | null;
  /** Date de début effective du congé (ISO YYYY-MM-DD). */
  congeMaterniteDateDebut?: string | null;
  /** Salaire mensuel brut en euros — base IJ CPAM L. 331-3 CSS. */
  congeMaterniteSalaireMensuelBrut?: number | null;
}

/**
 * F-256 SF-212-17 — sous-objet IA pour l'outil F-DT-43 (rupture anticipée du
 * CDD, FRANCE — L. 1243-1 à L. 1243-4 CT ; L. 1243-8 CT ; L. 1226-4-2 CT).
 * Tous champs nullables. Réactivé par F-256.
 */
export interface RuptureAnticipeeCddDetail {
  /** Auteur de la rupture anticipée — EMPLOYEUR | SALARIE. */
  ruptureAnticipeeCddAuteur?: string | null;
  /** Motif invoqué — ACCORD_PARTIES | FAUTE_GRAVE | FORCE_MAJEURE | INAPTITUDE | CDI_EMBAUCHE | AUTRE. */
  ruptureAnticipeeCddMotif?: string | null;
  /** Date du terme normal du CDD (ISO YYYY-MM-DD) — distincte de la date de rupture effective. */
  ruptureAnticipeeCddDateTerme?: string | null;
}

/**
 * F-256 SF-212-21 — sous-objet IA pour l'outil F-DT-41 (démission validité
 * équivoque, FRANCE — Cass. soc. 09/05/2007 ; volonté claire et non équivoque).
 * Tous champs nullables. Réactivé par F-256.
 */
export interface DemissionEquivoqueDetail {
  /** Mode d'expression de la démission (texte libre court — ex. "SMS", "mail", "courrier"). */
  demissionModeExpression?: string | null;
  /** Altercation ou conflit ouvert au moment de la démission. */
  demissionContexteAltercation?: boolean | null;
  /** Pression directe ou indirecte exercée par l'employeur. */
  demissionPression?: boolean | null;
  /** Rétractation rapide produite par le salarié. */
  demissionRetractation?: boolean | null;
  /** Manquements graves de l'employeur contemporains de la démission. */
  demissionManquementsEmployeur?: boolean | null;
}


export interface ImmigrationExtractedData {
  dateExpirationTitre?: string | null;
  typeTitreSejour?: string | null;
  typeProcedureDetectee?: string | null;
  dateDepotProcedure?: string | null;
  typeTitreSejourCode?: string | null;
  /** SF-214-26 (F-IM-37) : panne ANEF détectée par l'analyse (téléservice indisponible). */
  anefPanneDetectee?: boolean | null;
  nationaliteUe?: boolean | null;
  typeRecoursCode?: string | null;
  dateNotificationDecisionContestee?: string | null;
  /** SF-IM-01-04 : type de checklist inféré pour F-IM-01 (13 régimes V1). */
  inferredChecklistType?: string | null;
  /** SF-155-04-00-BE-immig-FR : date notification OQTF avec délai (F-IM-08-02). FR uniquement. */
  dateNotificationOqtf?: string | null;
  /** SF-155-04-00-BE-immig-FR : code motif OQTF avec délai (F-IM-08-02), aligné enum MotifOqtf front. FR uniquement. */
  motifOqtfCode?: 'REFUS_TITRE' | 'EXPIRATION_TITRE' | 'SEJOUR_IRREGULIER' | 'RETRAIT_TITRE' | 'AUTRE' | null;
  /** SF-155-04-00-BE-immig-FR : indicateur détection recours OQTF déjà formé (F-IM-08-02 / F-IM-08-04). FR uniquement. */
  recoursFormeDetected?: DetectedAnswer | null;
  /** SF-155-04-00-BE-immig-FR : horodatage notification OQTF sans délai ISO partiel (F-IM-08-04, urgence 48h). FR uniquement. */
  dateHeureNotificationOqtfSansDelai?: string | null;
  /** SF-155-04-00-BE-immig-FR : placement en CRA concomitant à l'OQTF sans délai (F-IM-08-04). FR uniquement. */
  placementCraDetected?: boolean | null;
  // SF-155-04-00-BE-immig-BE : 4 champs Annexe 13 BE pour pré-fill F-IM-08-06
  // (BELGIQUE uniquement — dossiers FR : null).
  /** Date de notification de l'Annexe 13 / OQT belge (YYYY-MM-DD). */
  dateNotificationAnnexe13?: string | null;
  /** Délai de départ volontaire imposé par l'OE (entier ≥ 0, typiquement 0/7/30). */
  delaiDepartImposeJours?: number | null;
  /**
   * Code motif OQT belge (Annexe13BeCalculator.MOTIFS_VALIDES) :
   * SEJOUR_IRREGULIER_ART_7 | REFUS_SEJOUR_APRES_DEMANDE | FIN_SEJOUR_REGULIER | AUTRE.
   */
  motifOqtCodeBe?: string | null;
  /** Indices factuels d'un transfert imminent vers CRA ou frontière (signal critique). */
  transfertImminentDetected?: boolean | null;
  /**
   * F-197 SF-197-02 : override avocat single-value du type de procédure
   * (Immigration). Si présent, prend précédence sur {@link #typeProcedureDetectee}
   * pour le pré-remplissage des outils décisionnels au prochain run de
   * Synthèse enrichie (F-IM-08/20). Null tant qu'aucun override n'a été
   * posé. Persisté côté backend dans la table `case_file_type_litige_override`
   * (cf. SF-197-01).
   */
  typeProcedureAvocatOverride?: string | null;
  /**
   * SF-246-04 : date de l'ordonnance de protection JAF (Cciv 515-9) pour
   * pré-fill de l'outil F-IM-24 (victime de violences L.425-6). FR uniquement
   * — dossier BE : null.
   */
  dateOrdonnanceProtectionJaf?: string | null;
  /** F-235 : nationalité du requérant en texte libre (ex. "Algérienne"). */
  nationalite?: string | null;
  /** SF-246-16 : identité requérant + référence décision contestée pour pré-fill F-IM-06. */
  nomRequerant?: string | null;
  prenomRequerant?: string | null;
  dateDecisionContestee?: string | null;
  referenceDecision?: string | null;
  /**
   * SF-246-17 : pré-fill outils dublin-recours (F-IM-22) et crrv-refus-visa (F-IM-23).
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   * `dublinEtatMembreResponsable` : texte libre (≤ 200 car.).
   * `dublinMotifTransfert` : code parmi DEMANDE_ASILE_AUTRE_ETAT / VISA_DELIVRE_AUTRE_ETAT /
   *   ENTREE_IRREGULIERE_AUTRE_ETAT / MEMBRE_FAMILLE_AUTRE_ETAT / AUTRE.
   * `crrvTypeVisa` : code parmi COURT_SEJOUR / LONG_SEJOUR / REGROUPEMENT_FAMILIAL / ETUDIANT / AUTRE.
   * `crrvMotifRefus` : texte libre (≤ 500 car.).
   */
  dublinEtatMembreResponsable?: string | null;
  dublinMotifTransfert?: string | null;
  crrvTypeVisa?: string | null;
  crrvMotifRefus?: string | null;
  /**
   * SF-246-18 : pré-fill outils AES Immigration FR.
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   * `aesDateEntreeFrance` : date d'entrée en France ISO YYYY-MM-DD.
   * `aesDureePresenceMois` : mois entiers depuis aesDateEntreeFrance (calculé backend).
   * `aesAnneesScolariteConsecutives` : années d'études consécutives en France.
   * `aesNiveauEtudes` : LYCEE / BAC_PLUS_1_2 / BAC_PLUS_3_4 / BAC_PLUS_5_PLUS.
   * `aesDureeScolaritePlusAncienEnfantAnnees` : années scolarité enfant le plus ancien.
   * `aesMotifHumanitaire` : code motif humanitaire (6 valeurs).
   * `aesMoisActiviteSalariee` : mois salariat dans les 24 derniers mois (0–24).
   * `aesCodeMetier` : code ROME ou libellé métier en tension.
   * `aesCalculPresenceDeclenche` : flag d'activation de l'outil AES présence
   *   prouvée (F-IM-30) — visibility CONTEXTUAL (SF-214-11).
   */
  aesDateEntreeFrance?: string | null;
  aesCalculPresenceDeclenche?: boolean | null;
  aesDureePresenceMois?: number | null;
  aesAnneesScolariteConsecutives?: number | null;
  aesNiveauEtudes?: string | null;
  aesDureeScolaritePlusAncienEnfantAnnees?: number | null;
  aesMotifHumanitaire?: string | null;
  aesMoisActiviteSalariee?: number | null;
  aesCodeMetier?: string | null;
  /**
   * SF-246-19 : pré-fill statut & dispositifs Immigration FR.
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   * `changementTitreEnvisage` : code titre envisagé (même whitelist que typeTitreSejourCode).
   * `changementRemunerationEur` : rémunération brute annuelle en euros (> 0, ≤ 500 000).
   * `natDureeResidenceReguliereAnnees` : durée résidence régulière en années (0–70, voies DECRET/ASCENDANT).
   * `natDureeMariageAnnees` : durée mariage avec Français(e) en années (0–70, voie MARIAGE).
   * `natAgeDemandeur` : âge du demandeur en années (0–120, voie ASCENDANT).
   * `mineursDateNaissance` : date de naissance du mineur YYYY-MM-DD (non future).
   * `algerienPresenceReguliereMois` : durée présence régulière en mois (0–600, régime algérien).
   * `asileDateDecisionAnterieure` : date décision antérieure asile YYYY-MM-DD (non future).
   * `asileProcedureeAccelereee` : SF-214-20 — procédure d'asile traitée en accélérée (F-IM-34 AJ CNDA, délai de recours réduit).
   * `eloiDureePresenceIrreguliereMois` : durée présence irrégulière en mois (0–600, IRTF).
   * `eloiMotifMenace` : motif menace (ORDRE_PUBLIC / SECURITE_ETAT / TERRORISME / RECIDIVE_GRAVE / AUTRE).
   */
  changementTitreEnvisage?: string | null;
  changementRemunerationEur?: number | null;
  natDureeResidenceReguliereAnnees?: number | null;
  natDureeMariageAnnees?: number | null;
  natAgeDemandeur?: number | null;
  mineursDateNaissance?: string | null;
  algerienPresenceReguliereMois?: number | null;
  asileDateDecisionAnterieure?: string | null;
  asileProcedureeAccelereee?: boolean | null;
  eloiDureePresenceIrreguliereMois?: number | null;
  eloiMotifMenace?: string | null;
  /**
   * SF-214-28 : pré-fill outil MNA évaluation âge / recours JE Immigration FR
   * (F-IM-38-mna-evaluation-age-fr). FRANCE uniquement — dossier BE : null.
   * `mnaEvaluationRefusee` : l'ASE a refusé l'évaluation / la prise en charge du mineur isolé.
   * `mnaExamenOsseuxOrdonne` : un examen osseux a été ordonné dans le cadre de l'évaluation de l'âge.
   * (la date de naissance déclarée réutilise `mineursDateNaissance` existant.)
   */
  mnaEvaluationRefusee?: boolean | null;
  mnaExamenOsseuxOrdonne?: boolean | null;
  /**
   * SF-214-18 : pré-fill outil OFPRA introduction Immigration FR (F-IM-33).
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   * `procedureAsileDetectee` : flag détection d'une procédure d'asile dans le
   *   dossier (visibility CONTEXTUAL de l'outil OFPRA introduction).
   * `gudaPassageEffectue` : indice de passage au guichet unique (GUDA) extrait
   *   des documents (attestation de demande d'asile).
   * `aesDateEntreeFrance` (déjà déclaré ci-dessus) sert de pré-fill pour la date
   *   d'arrivée en France.
   */
  procedureAsileDetectee?: boolean | null;
  gudaPassageEffectue?: boolean | null;
  /**
   * SF-246-20 : pré-fill lot Immigration BE — 4 outils belgian-9bis / 9ter / 40bis / 40ter.
   * BELGIQUE UNIQUEMENT — null pour dossiers FRANCE. Tous nullables.
   * `be9bisDateEntreeBelgique` : date d'entrée en Belgique YYYY-MM-DD (art. 9bis, Annexe 26 / passeport), non future.
   * `be9bisDureePresenceMois` : mois entiers depuis be9bisDateEntreeBelgique jusqu'à aujourd'hui (calculé backend).
   * `be9terDateDebutSymptomes` : date du début des symptômes médicaux YYYY-MM-DD (art. 9ter, certificat médical), non future.
   * `be40bisLienFamilial` : lien familial 40bis — whitelist CONJOINT / ENFANT / ASCENDANT / PARTENAIRE_ENREGISTRE.
   * `be40terLienFamilial` : lien familial 40ter — même whitelist.
   * `be40terRevenusMensuelsNets` : revenus mensuels nets du regroupant belge (€, > 0, ≤ 30 000).
   */
  be9bisDateEntreeBelgique?: string | null;
  be9bisDureePresenceMois?: number | null;
  be9terDateDebutSymptomes?: string | null;
  be40bisLienFamilial?: string | null;
  be40terLienFamilial?: string | null;
  be40terRevenusMensuelsNets?: number | null;
  /**
   * SF-214-01 / SF-214-02 — Pré-fill outil F-IM-25 « Étranger malade L. 425-9 CESEDA ».
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   *
   * `etrangerMaladeDetecte` : signal global (mentions « maladie grave », « OFII médical »,
   *   « L.425-9 » dans les pièces).
   * `etrangerMaladePathologie` : pathologie principale extraite (texte libre ≤ 500 car.).
   * `etrangerMaladeTraitementDisponible` : true si les pièces indiquent que le traitement
   *   est disponible dans le pays d'origine.
   * `etrangerMaladeAvisOFII` : avis du collège médical OFII (FAVORABLE/DEFAVORABLE/EN_ATTENTE).
   * `etrangerMalaDateAvisOFII` : date de l'avis OFII ISO YYYY-MM-DD (non future).
   */
  etrangerMaladeDetecte?: boolean | null;
  etrangerMaladePathologie?: string | null;
  etrangerMaladeTraitementDisponible?: boolean | null;
  etrangerMaladeAvisOFII?: 'FAVORABLE' | 'DEFAVORABLE' | 'EN_ATTENTE' | null;
  etrangerMalaDateAvisOFII?: string | null;
  /**
   * SF-215-01 / SF-215-02 — Pré-fill outil F-IM-25-single-permit-be
   * « Permis unique BE » (travail + séjour, art. 61/25-2 à 61/25-7 Loi 15/12/1980,
   * Accord coopération 02/02/2018, Décret CRWAPE 16/05/2019, AGW 02/05/2019).
   * BELGIQUE uniquement — dossier FR : null. Tous nullables.
   *
   * `singlePermitDateDebut` : date début validité permit (ISO YYYY-MM-DD).
   * `singlePermitDateFin`   : date fin validité permit (ISO YYYY-MM-DD).
   * `singlePermitRegion`    : région d'instruction (WALLONIE / FLANDRE / BRUXELLES).
   * `singlePermitTypeActivite` : nature de l'activité (SALARIE/STAGIAIRE/DETACHE/CHERCHEUR/ETUDIANT).
   * `singlePermitMotif`     : motif de la demande (NOUVEAU / RENOUVELLEMENT).
   */
  singlePermitDateDebut?: string | null;
  singlePermitDateFin?: string | null;
  singlePermitRegion?: 'WALLONIE' | 'FLANDRE' | 'BRUXELLES' | null;
  singlePermitTypeActivite?: 'SALARIE' | 'STAGIAIRE' | 'DETACHE' | 'CHERCHEUR' | 'ETUDIANT' | null;
  singlePermitMotif?: 'NOUVEAU' | 'RENOUVELLEMENT' | null;
  /**
   * SF-215-03 / SF-215-04 — Pré-fill outil F-IM-26-regroupement-10ter-be
   * « Regroupement familial art. 10ter (BE) » — Loi 15/12/1980 art. 10ter,
   * AR 17/05/2007 (seuil ressources 120 % RIS), Arrêté Royal d'exécution.
   * BELGIQUE uniquement — dossier FR : null. Tous nullables.
   *
   * `be10terLienFamilial` : lien familial — whitelist
   *   CONJOINT / PARTENAIRE_ENREGISTRE / ENFANT_MOINS_21 /
   *   ENFANT_21_PLUS_CHARGE / ASCENDANT_CHARGE.
   * `be10terTypeCarte` : type de carte du regroupant — whitelist CARTE_B / CARTE_C.
   * `be10terRevenusMensuels` : revenus mensuels nets du regroupant en € (entier 0–100 000).
   * `be10terDureeSejour` : durée du séjour régulier du regroupant en mois (entier 0–600).
   */
  be10terLienFamilial?: 'CONJOINT' | 'PARTENAIRE_ENREGISTRE' | 'ENFANT_MOINS_21' | 'ENFANT_21_PLUS_CHARGE' | 'ASCENDANT_CHARGE' | null;
  be10terTypeCarte?: 'CARTE_B' | 'CARTE_C' | null;
  be10terRevenusMensuels?: number | null;
  be10terDureeSejour?: number | null;
  /**
   * SF-214-03 / SF-214-04 — Pré-fill outil F-IM-26-regroupement-familial-fr
   * « Regroupement familial L.434-1+ CESEDA (FR) » — ressources SMIC + surface
   * habitable. FRANCE uniquement — dossier BE : null. Tous nullables.
   *
   * `regroupementFamilialEnvisage` : true si les pièces évoquent une demande de
   *   regroupement familial (signal global, pas un champ saisissable).
   * `regroupementRessourcesMensuelles` : ressources mensuelles nettes du
   *   regroupant en € (double, → ressourcesMensuellesNettes).
   * `regroupementType` : type de regroupement — whitelist
   *   CONJOINT / ENFANT_MINEUR / AUTRE (→ typeRegroupement).
   * Note : la durée de séjour régulier est pré-remplie via `aesDureePresenceMois`.
   */
  regroupementFamilialEnvisage?: boolean | null;
  regroupementRessourcesMensuelles?: number | null;
  regroupementType?: 'CONJOINT' | 'ENFANT_MINEUR' | 'AUTRE' | null;
  /**
   * SF-215-13 / SF-215-14 — Pré-fill outil F-IM-31-cce-annulation-30j-be
   * « Recours CCE annulation 30j (BE) » — Loi 15/12/1980 art. 39/2 §2 et
   * 39/57 §1er (délai de droit commun 30 jours calendaires).
   * ⚠️ CCE = Conseil du Contentieux des Étrangers (droit des étrangers belge),
   * PAS la Centrale des Crédits. BELGIQUE uniquement — dossier FR : null.
   *
   * `recoursCceEnvisage`        : flag CONTEXTUAL — un recours CCE est envisagé.
   * `recoursCceDateNotification`: date de notification de la décision attaquée
   *   (ISO yyyy-MM-dd) — champ pré-fill RÉEL.
   * `recoursCceTypeDecision`    : type de décision attaquée — whitelist
   *   REFUS_TITRE / REFUS_REGROUPEMENT / REFUS_9BIS / REFUS_9TER /
   *   OQT_ANNEXE13 / DECISION_CGRA / AUTRE — champ pré-fill RÉEL.
   */
  recoursCceEnvisage?: boolean | null;
  recoursCceDateNotification?: string | null;
  recoursCceTypeDecision?: 'REFUS_TITRE' | 'REFUS_REGROUPEMENT' | 'REFUS_9BIS' | 'REFUS_9TER' | 'OQT_ANNEXE13' | 'DECISION_CGRA' | 'AUTRE' | null;
  /**
   * SF-215-15 / SF-215-16 — Pré-fill outil F-IM-32-cce-extreme-urgence-5j-be
   * « Recours CCE extrême urgence 5j (BE) » — Loi 15/12/1980 art. 39/82
   * (5 jours OUVRABLES à compter de l'acte exécutoire). Cas d'urgence absolue.
   * ⚠️ CCE = Conseil du Contentieux des Étrangers. BELGIQUE uniquement — FR : null.
   *
   * `recoursCceExtremeUrgence`        : flag CONTEXTUAL — un recours en extrême
   *   urgence est pertinent (acte exécutoire imminent).
   * `recoursExtremeUrgenceDateActe`   : date de l'acte exécutoire attaqué
   *   (ISO yyyy-MM-dd) — champ pré-fill RÉEL.
   * `recoursExtremeUrgenceTypeActe`   : type d'acte exécutoire — whitelist
   *   OQT_EXECUTE / TRANSFERT_DUBLIN / REFUS_ACCES_TERRITOIRE /
   *   EXPULSION_IMMEDIATE / AUTRE — champ pré-fill RÉEL.
   */
  recoursCceExtremeUrgence?: boolean | null;
  recoursExtremeUrgenceDateActe?: string | null;
  recoursExtremeUrgenceTypeActe?: 'OQT_EXECUTE' | 'TRANSFERT_DUBLIN' | 'REFUS_ACCES_TERRITOIRE' | 'EXPULSION_IMMEDIATE' | 'AUTRE' | null;
  /**
   * SF-215-17 / SF-215-18 — Pré-fill outil F-IM-33-annexe13quinquies-ie-be
   * « Annexe 13quinquies OQT + interdiction d'entrée (BE) » — Loi 15/12/1980
   * art. 74/11 (interdiction d'entrée Schengen 3 / 5 / 8 ans selon le motif),
   * recours en annulation devant le CCE (Conseil du Contentieux des Étrangers,
   * art. 39/2 §2 — 30 jours calendaires). BELGIQUE uniquement — dossier FR : null.
   *
   * `interdictionEntreeDetectee`        : flag CONTEXTUAL — une annexe 13quinquies
   *   (OQT + interdiction d'entrée) est détectée dans le dossier.
   * `interdictionEntreeDateNotification`: date de notification de l'annexe
   *   (ISO yyyy-MM-dd) — champ pré-fill RÉEL.
   * `interdictionEntreeMotif`           : motif de l'interdiction — whitelist
   *   SEJOUR_IRREGULIER / MENACE_ORDRE_PUBLIC / RAISONS_SECURITE_NATIONALE /
   *   ATTEINTE_INTERET_UE / DECISION_JUDICIAIRE — champ pré-fill RÉEL.
   */
  interdictionEntreeDetectee?: boolean | null;
  interdictionEntreeDateNotification?: string | null;
  interdictionEntreeMotif?: 'SEJOUR_IRREGULIER' | 'MENACE_ORDRE_PUBLIC' | 'RAISONS_SECURITE_NATIONALE' | 'ATTEINTE_INTERET_UE' | 'DECISION_JUDICIAIRE' | null;
  /**
   * SF-215-19 / SF-215-20 — Pré-fill outil F-IM-34-protection-temporaire-ukraine-be
   * « Protection temporaire Ukraine (BE) » — décision d'exécution (UE) 2022/382
   * (directive 2001/55/CE). BELGIQUE uniquement — dossier FR : null.
   *
   * `protectionTemporaireUkraineDetectee` : flag CONTEXTUAL — une situation de
   *   protection temporaire Ukraine est détectée dans le dossier.
   * `ptUkraineDateArrivee`                : date d'arrivée sur le territoire
   *   (ISO yyyy-MM-dd) — champ pré-fill RÉEL.
   * `ptUkraineNationalite`                : nationalité ukrainienne détectée
   *   (booléen) — champ pré-fill RÉEL (pré-coche la checkbox nationalité).
   */
  protectionTemporaireUkraineDetectee?: boolean | null;
  ptUkraineDateArrivee?: string | null;
  ptUkraineNationalite?: boolean | null;
  /**
   * SF-215-05 / SF-215-06 — Pré-fill outil F-IM-27-regroupement-10bis-be
   * « Regroupement familial art. 10bis (BE) » — Loi 15/12/1980 art. 10bis,
   * AR 17/05/2007 (seuil ressources 120 % RIS). À la différence de l'art.
   * 10ter, le regroupant détient un séjour LIMITÉ (carte A) — d'où la
   * condition supplémentaire `be10bisDateFinCarteA` (validité du titre à
   * la date d'analyse). BELGIQUE uniquement — dossier FR : null. Tous nullables.
   *
   * `be10bisLienFamilial` : lien familial — whitelist
   *   CONJOINT / PARTENAIRE_ENREGISTRE / ENFANT_MOINS_21 /
   *   ENFANT_21_PLUS_CHARGE / ASCENDANT_CHARGE.
   * `be10bisRevenusMensuels` : revenus mensuels nets du regroupant en € (entier 0–100 000).
   * `be10bisDureeSejour` : durée du séjour régulier du regroupant en mois (entier 0–600).
   * `be10bisDateFinCarteA` : date de fin de validité de la carte A (ISO YYYY-MM-DD).
   *
   * NB : `typeCarteRegroupant` n'a qu'une valeur possible (CARTE_A) pour
   * l'art. 10bis, donc non pré-rempli — forcé côté composant.
   */
  be10bisLienFamilial?: 'CONJOINT' | 'PARTENAIRE_ENREGISTRE' | 'ENFANT_MOINS_21' | 'ENFANT_21_PLUS_CHARGE' | 'ASCENDANT_CHARGE' | null;
  be10bisRevenusMensuels?: number | null;
  be10bisDureeSejour?: number | null;
  be10bisDateFinCarteA?: string | null;
  /**
   * SF-215-05 / SF-215-06 — Flag de visibilité CONTEXTUAL pour l'outil
   * F-IM-27-regroupement-10bis-be. True si le pipeline IA détecte un
   * regroupement familial vers un titulaire de séjour limité (carte A).
   * Synonyme métier de `regroupement_10bis_detecte` côté backend.
   */
  regroupementTiersLimiteDetecte?: boolean | null;
  /**
   * SF-215-07 / SF-215-08 — Pré-fill outil F-IM-28-naturalisation-12bis-be
   * « Naturalisation art. 12bis (BE) » — Code de la nationalité belge
   * (Loi 28/06/1984, mod. Loi 04/12/2012). BELGIQUE uniquement —
   * dossier FR : null. Tous nullables.
   *
   * `naturalisationBeDureeSejour` : durée du séjour légal en mois (entier 0–600).
   * `naturalisationBeTypeSejour`  : statut du titre — `LIMITE` (carte A)
   *   ou `ILLIMITE` (carte B / C / D / F+ / K / L). Seul `ILLIMITE` ouvre
   *   l'une des deux voies 12bis.
   * `naturalisationBeNiveauLangue`: niveau atteint dans une des 3 langues
   *   officielles — `INFERIEUR_A2` / `A2` / `SUPERIEUR_A2` (CECRL).
   *
   * NB : les 4 critères restants (preuveIntegration, preuveEmploi,
   * menaceOrdrePublic, condamnationPenale) sont aspirationnels —
   * `PREFILL_COUNT_ALWAYS_ZERO` côté UI car le LLM ne peut pas les
   * inférer de manière fiable depuis les pièces du dossier.
   */
  naturalisationBeDureeSejour?: number | null;
  naturalisationBeTypeSejour?: 'LIMITE' | 'ILLIMITE' | null;
  naturalisationBeNiveauLangue?: 'INFERIEUR_A2' | 'A2' | 'SUPERIEUR_A2' | null;
  /**
   * SF-215-07 / SF-215-08 — Flag de visibilité CONTEXTUAL pour l'outil
   * F-IM-28-naturalisation-12bis-be. True si le pipeline IA détecte une
   * démarche de naturalisation 12bis envisagée. Synonyme métier de
   * `naturalisation_be_envisagee` côté backend.
   *
   * NB : ce même flag pilote AUSSI la visibilité CONTEXTUAL de l'outil
   * F-IM-29-naturalisation-conjoint-belge-be (SF-215-09 / SF-215-10) —
   * voie « conjoint Belge » art. 16 CNB.
   */
  naturalisationBeEnvisagee?: boolean | null;
  /**
   * SF-215-09 / SF-215-10 — Pré-fill outil
   * F-IM-29-naturalisation-conjoint-belge-be — voie « conjoint Belge »
   * article 16 du Code de la nationalité belge. BELGIQUE uniquement —
   * dossier FR : null. Tous nullables.
   *
   * `naturalisationBeArt16DateMarriage` : date du mariage (ISO yyyy-MM-dd).
   * `naturalisationBeArt16DureeCohabitation` : durée cohabitation
   *   effective en mois (entier 0–600).
   * `naturalisationBeArt16NiveauLangue` : niveau atteint dans une des 3
   *   langues officielles — `INFERIEUR_A2` / `A2` / `SUPERIEUR_A2`
   *   (CECRL). Whitelist alignée sur l'enum SF-215-07 partagée.
   *
   * NB : les 4 critères restants (cohabitationLegale, preuveIntegration,
   * menaceOrdrePublic, condamnationPenale) sont aspirationnels —
   * `PREFILL_COUNT_ALWAYS_ZERO` côté UI car le LLM ne peut pas les
   * inférer de manière fiable depuis les pièces du dossier.
   */
  naturalisationBeArt16DateMarriage?: string | null;
  naturalisationBeArt16DureeCohabitation?: number | null;
  naturalisationBeArt16NiveauLangue?: 'INFERIEUR_A2' | 'A2' | 'SUPERIEUR_A2' | null;
  /**
   * SF-215-11 / SF-215-12 — Pré-fill outil composite F-IM-30-aesm-mena-be
   * « AESM + tutelle DGDE (MENA) ». BELGIQUE uniquement — dossier FR : null.
   * Tous nullables.
   *
   * `menaAge` : âge actuel du Mineur Étranger Non Accompagné (entier 0-17).
   * `menaDateArrivee` : date d'arrivée sur le territoire belge (ISO yyyy-MM-dd, non future).
   * `menaDureeScolaire` : durée d'inscription scolaire effective en mois
   *   (entier 0-120, déclenche un bonus AESM +5 pts si > 24).
   *
   * NB : les 5 critères restants (tuteurDesigne, integrationScolaire,
   * projetVieElabore, perspectiveAutonomie, menaceOrdrePublic) sont
   * aspirationnels — `PREFILL_COUNT_ALWAYS_ZERO` côté UI car le LLM
   * ne peut pas les inférer de manière fiable depuis les pièces.
   */
  menaAge?: number | null;
  menaDateArrivee?: string | null;
  menaDureeScolaire?: number | null;
  /**
   * SF-214-05 / SF-214-06 — Pré-fill outil F-IM-27-vpf-liens-personnels-l42323-fr
   * « Vie privée et familiale — liens personnels L.423-23 CESEDA (FR) » — scoring
   * d'éligibilité au titre VPF (durée de résidence, attaches familiales, intégration).
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   *
   * `viePriveeFamilialeDetectee` : signal global (flag CONTEXTUAL) — les pièces
   *   évoquent une demande de titre « vie privée et familiale » L.423-23.
   * `vpfNiveauIntegration` : niveau d'intégration estimé par le pipeline IA —
   *   whitelist FORT / MOYEN / FAIBLE (→ niveauIntegration).
   * Note : la durée de résidence est pré-remplie via `aesDureePresenceMois`,
   *   la minorité à l'entrée via `clientMineurDetecte`, et la présence d'enfants
   *   en France est déduite de `aesDureeScolaritePlusAncienEnfantAnnees > 0`.
   */
  viePriveeFamilialeDetectee?: boolean | null;
  vpfNiveauIntegration?: 'FORT' | 'MOYEN' | 'FAIBLE' | null;
  /**
   * SF-214-06 — true si le pipeline IA détecte que le client est entré en France
   * mineur (→ entreeEnFranceMineur de l'outil VPF liens personnels). FR : nullable.
   */
  clientMineurDetecte?: boolean | null;
  /**
   * SF-214-08 — true si le pipeline IA détecte que la validation VLS-TS auprès
   * de l'OFII a déjà été effectuée (outil F-IM-28). FR uniquement : nullable.
   * La date d'entrée en France est pré-remplie depuis `aesDateEntreeFrance`.
   */
  vlsTsValidationOFIIEffectuee?: boolean | null;
  /**
   * SF-214-16 — true si le pipeline IA détecte qu'un recouvrement / renouvellement
   * de titre est en cours (récépissé ou attestation de prolongation détecté). FR
   * uniquement : nullable. Sert de flag de contexte pour l'outil F-IM-32.
   */
  recouvrementTitreEnCours?: boolean | null;
  /**
   * SF-214-16 — type de document de séjour détecté pour pré-fill de l'outil
   * F-IM-32 (récépissé vs attestation). FR uniquement : nullable.
   */
  recepisseOuAttestationType?: 'RECEPISSE' | 'ATTESTATION_PROLONGATION' | 'INCONNU' | null;
  /**
   * SF-214-22 — true si le pipeline IA détecte un profil de victime de traite
   * des êtres humains / proxénétisme (flag de contexte pour l'outil F-IM-35).
   * FR uniquement : nullable.
   */
  victimeTraiteDetectee?: boolean | null;
  /**
   * SF-214-22 — true si une plainte / un témoignage contre l'auteur de la
   * traite est détecté. Sert de pré-fill (plainteDeposee) pour l'outil F-IM-35.
   * FR uniquement : nullable.
   */
  tehPlainteDeposee?: boolean | null;
  /**
   * SF-214-22 — date ISO YYYY-MM-DD de la plainte / du témoignage détecté.
   * Sert de pré-fill (datePlainte) pour l'outil F-IM-35. FR uniquement : nullable.
   */
  tehDatePlainte?: string | null;
  /**
   * SF-214-24 — true si le pipeline IA détecte un projet de demande de carte de
   * résident de dix ans (flag de contexte CONTEXTUAL pour l'outil F-IM-36).
   * FR uniquement : nullable.
   */
  carteResidentEnvisagee?: boolean | null;
  /**
   * SF-214-24 — ressources mensuelles nettes (€) détectées par le pipeline IA,
   * pré-fill de l'outil carte de résident F-IM-36 (ressourcesMensuellesNettes).
   * FR uniquement : nullable.
   */
  carteResidentRessources?: number | null;
  /**
   * SF-214-30 — Pré-fill outil F-IM-39-naturalisation-recours-tj-fr « Recours TJ
   * naturalisation » (recours juridictionnel devant le tribunal judiciaire contre
   * un refus d'enregistrement / une contestation de nationalité, délai 6 mois).
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   *
   * `naturalisationVoie` : voie de la déclaration de nationalité détectée par le
   *   pipeline IA — whitelist MARIAGE / ASCENDANT / MINEUR_22_1 (→ voieNaturalisation).
   * `naturalisationDateRefus` : date ISO YYYY-MM-DD du refus d'enregistrement ou
   *   de la contestation (→ dateRefusDeclaration, point de départ du délai 6 mois).
   * Note : le type de refus (refus d'enregistrement vs contestation de
   *   nationalité) reste une qualification juridique laissée à l'avocat.
   */
  naturalisationVoie?: 'MARIAGE' | 'ASCENDANT' | 'MINEUR_22_1' | null;
  naturalisationDateRefus?: string | null;

  /**
   * SF-214-34 — Pré-fill outil F-IM-41-appel-caa-cassation-ce-fr « Appel CAA /
   * cassation CE » (délais d'appel devant la cour administrative d'appel contre
   * un jugement de TA en contentieux des étrangers, délai 1 mois ou 15 j en OQTF).
   * FRANCE uniquement — dossier BE : null. Tous nullables.
   *
   * `recoursEnvisageDetecte` : indice CONTEXTUAL signalant que les pièces
   *   évoquent un recours / appel envisagé (pas un champ de saisie de l'outil,
   *   sert au scoring de pertinence d'affichage).
   * `recoursDateJugementTA` : date ISO YYYY-MM-DD du jugement du tribunal
   *   administratif (→ dateJugementTA, point de départ du délai d'appel CAA).
   */
  recoursEnvisageDetecte?: boolean | null;
  recoursDateJugementTA?: string | null;

  /**
   * SF-214-36 — Pré-fill / contexte outil F-IM-42-assignation-residence-fr
   * « Assignation à résidence » (CESEDA, assignation prononcée en vue de
   * l'exécution d'une mesure d'éloignement). FRANCE uniquement — dossier BE :
   * null. Tous nullables.
   *
   * `assignationResidenceDetectee` : indice CONTEXTUAL signalant que les pièces
   *   évoquent une assignation à résidence (sert au scoring de pertinence
   *   d'affichage, pas un champ de saisie de l'outil).
   * `assignationDateNotification` : date ISO YYYY-MM-DD de notification de
   *   l'arrêté d'assignation (→ dateNotificationAssignation, point de départ).
   */
  assignationResidenceDetectee?: boolean | null;
  assignationDateNotification?: string | null;

  /**
   * SF-214-38 — Pré-fill / contexte outil F-IM-43-itf-judiciaire-fr
   * « ITF judiciaire » (interdiction du territoire français prononcée par le
   * juge pénal, art. 131-30 et s. du Code pénal). FRANCE uniquement — dossier
   * BE : null. Tous nullables.
   *
   * `itfJudiciaireDateCondamnation` : date ISO YYYY-MM-DD de la condamnation
   *   pénale prononçant l'ITF (→ dateCondamnation, point de départ des délais).
   * `itfJudiciaireDureeAnnees` : durée de l'ITF en années (→ dureeITFAnnees).
   */
  itfJudiciaireDateCondamnation?: string | null;
  itfJudiciaireDureeAnnees?: number | null;
  /**
   * SF-214-42 — Pré-fill / contexte outil F-IM-45-retrait-titre-fraude-fr
   * « Retrait de titre pour fraude » (retrait d'un titre de séjour obtenu par
   * fraude — mariage gris, fausses déclarations, fraude documentaire — ou suite
   * à la perte des conditions de délivrance, CESEDA). FRANCE uniquement —
   * dossier BE : null. Tous nullables.
   *
   * `retraitTitreFraudeDetecte` : signal global de détection d'une décision de
   *   retrait de titre pour fraude (visibility CONTEXTUAL de l'outil).
   * `retraitTitreDateRetrait` : date ISO YYYY-MM-DD de la décision de retrait
   *   (→ dateRetrait, point de départ du délai de recours TA).
   * `retraitTitreMotif` : motif du retrait (→ motifRetrait).
   */
  retraitTitreFraudeDetecte?: boolean | null;
  retraitTitreDateRetrait?: string | null;
  retraitTitreMotif?: 'MARIAGE_GRIS' | 'FAUSSES_DECLARATIONS' | 'FRAUDE_DOCUMENTAIRE' | 'PERTE_CONDITIONS' | null;
  /**
   * SF-220-01 — F-IM-47 régime franco-tunisien (accord 17/03/1988, FR uniquement, null pour BE).
   * 3 champs de pré-fill. La visibilité de l'outil est conditionnée au champ `nationalite`='Tunisienne'
   * (pas de flag pivot dédié).
   * `regimeTunisienCategorie` : catégorie de demande (whitelist 5 codes).
   * `regimeTunisienDureeSejour` : durée de séjour envisagée en mois (≥ 0).
   * `regimeTunisienTitreEnCours` : titre de séjour déjà en cours.
   */
  regimeTunisienCategorie?: 'ETUDIANT' | 'COMMERCANT' | 'SALARIE' | 'FAMILIAL' | 'AUTRE' | null;
  regimeTunisienDureeSejour?: number | null;
  regimeTunisienTitreEnCours?: boolean | null;
  /**
   * SF-220-02 — F-IM-48 portée territoriale du titre à Mayotte (FR uniquement, null/false pour BE).
   * 1 flag pivot CONTEXTUAL (`mayotteDetecte`) + 3 champs de pré-fill.
   * `mayotteDetecte` : contexte mahorais détecté (pilote la visibilité de l'outil).
   * `mayotteTitreDelivreAMayotte` : titre délivré à Mayotte (portée territorialisée).
   * `mayotteTypeTitre` : type de titre (whitelist 5 codes).
   * `mayotteProjetDeplacementMetropole` : déplacement vers la métropole projeté.
   */
  mayotteDetecte?: boolean | null;
  mayotteTitreDelivreAMayotte?: boolean | null;
  mayotteTypeTitre?: 'VPF' | 'SALARIE' | 'ETUDIANT' | 'RESIDENT' | 'AUTRE' | null;
  mayotteProjetDeplacementMetropole?: boolean | null;
  /**
   * SF-220-03 — F-IM-49 VPF jeune majeur L.423-22 (FR uniquement, null/false pour BE).
   * 1 flag pivot CONTEXTUAL (`jeuneMajeurExMnaDetecte`) + 4 champs de pré-fill.
   * `jeuneMajeurExMnaDetecte` : jeune majeur ex-MNA scolarisé détecté (pilote la visibilité).
   * `jeuneMajeurAge` : âge du jeune (≥ 0).
   * `jeuneMajeurEntreMineur` : entré en France mineur.
   * `jeuneMajeurPriseEnChargeAse` : pris en charge par l'aide sociale à l'enfance (ASE).
   * `jeuneMajeurScolarise` : scolarisé ou en formation.
   */
  jeuneMajeurExMnaDetecte?: boolean | null;
  jeuneMajeurAge?: number | null;
  jeuneMajeurEntreMineur?: boolean | null;
  jeuneMajeurPriseEnChargeAse?: boolean | null;
  jeuneMajeurScolarise?: boolean | null;

  /**
   * SF-220-04 — F-IM-50 VPF au titre d'un PACS L.423-23 (FR uniquement, null/false pour BE).
   * 1 flag pivot CONTEXTUAL (`pacsDetecte`) + 4 champs de pré-fill.
   * `pacsDetecte` : contexte de PACS détecté (pilote la visibilité de l'outil).
   * `pacsConclu` : PACS effectivement conclu.
   * `pacsDate` : date du PACS (ISO yyyy-MM-dd).
   * `pacsDureeVieCommune` : durée de vie commune en mois (≥ 0).
   * `pacsIntensiteCommunauteVie` : intensité de la communauté de vie (whitelist 4 codes).
   */
  pacsDetecte?: boolean | null;
  pacsConclu?: boolean | null;
  pacsDate?: string | null;
  pacsDureeVieCommune?: number | null;
  pacsIntensiteCommunauteVie?: 'FORTE' | 'MOYENNE' | 'FAIBLE' | 'NON_ETABLIE' | null;
  /**
   * SF-220-05 — F-IM-51 déchéance de nationalité (Cciv 25 / 25-1) — FRANCE uniquement.
   * 1 flag pivot CONTEXTUAL (`decheanceNationaliteDetectee`) + 4 champs de pré-fill.
   * `decheanceNationaliteDetectee` : contexte de déchéance détecté (pilote la visibilité de l'outil).
   * `decheanceMotif` : motif de déchéance (whitelist 4 codes).
   * `decheanceBinational` : personne binationale (la déchéance ne peut rendre apatride).
   * `decheanceMesurePrononcee` : mesure déjà prononcée par décret.
   * `decheanceDateDecret` : date du décret (ISO yyyy-MM-dd).
   */
  decheanceNationaliteDetectee?: boolean | null;
  decheanceMotif?: 'TERRORISME' | 'ATTEINTE_INTERETS_NATION' | 'FRAUDE_ACQUISITION' | 'AUTRE' | null;
  decheanceBinational?: boolean | null;
  decheanceMesurePrononcee?: boolean | null;
  decheanceDateDecret?: string | null;

  /**
   * SF-220-06 — F-IM-52 signalement SIS aux fins de non-admission (Règl. UE
   * 2018/1860 / CESEDA L.312-3) — FRANCE uniquement. 1 flag pivot CONTEXTUAL
   * (`signalementSisDetecte`) + 3 champs de pré-fill.
   * `signalementSisDetecte` : contexte de signalement SIS détecté (pilote la visibilité de l'outil).
   * `signalementSisEtatSignalant` : État à l'origine du signalement (whitelist 3 codes).
   * `signalementSisMotifSignalement` : motif du signalement (whitelist 4 codes).
   * `signalementSisTitreSejourValide` : titre de séjour FR en cours de validité.
   */
  signalementSisDetecte?: boolean | null;
  signalementSisEtatSignalant?: 'FRANCE' | 'AUTRE_ETAT_MEMBRE' | 'INCONNU' | null;
  signalementSisMotifSignalement?:
    | 'IRTF'
    | 'MESURE_ELOIGNEMENT_ETRANGERE'
    | 'MENACE_ORDRE_PUBLIC'
    | 'AUTRE'
    | null;
  signalementSisTitreSejourValide?: boolean | null;

  /**
   * SF-221-01 — F-IM-53 prorogation de la carte A (séjour temporaire BE,
   * art. 13 Loi 15/12/1980 + art. 33 AR 08/10/1981) — BELGIQUE uniquement.
   * 1 flag pivot CONTEXTUAL (`carteAProrogationDetecte`) + 3 champs de pré-fill.
   * `carteAProrogationDetecte` : contexte de prorogation carte A détecté (pilote la visibilité).
   * `carteAProrogationDateExpiration` : date d'expiration de la carte A (ISO yyyy-MM-dd, future ou passée).
   * `carteAProrogationMotifPersiste` : le motif de séjour persiste à l'identique.
   * `carteAProrogationConditionsReunies` : les conditions initiales sont toujours réunies.
   */
  carteAProrogationDetecte?: boolean | null;
  carteAProrogationDateExpiration?: string | null;
  carteAProrogationMotifPersiste?: boolean | null;
  carteAProrogationConditionsReunies?: boolean | null;

  /**
   * SF-221-02 — F-IM-54 carte B séjour ILLIMITÉ d'un ressortissant tiers (BE,
   * art. 14 Loi 15/12/1980, passage carte A → carte B après 5 ans) — BELGIQUE uniquement.
   * 1 flag pivot CONTEXTUAL (`carteBSejourIllimiteDetecte`) + 3 champs de pré-fill.
   * `carteBSejourIllimiteDetecte` : contexte de passage au séjour illimité détecté (pilote la visibilité).
   * `carteBDateDebutSejour` : date de début du séjour régulier ininterrompu (ISO yyyy-MM-dd, non future).
   * `carteBSejourIninterrompu` : le séjour régulier a été ininterrompu.
   * `carteBMotifStable` : le motif de séjour est stable.
   */
  carteBSejourIllimiteDetecte?: boolean | null;
  carteBDateDebutSejour?: string | null;
  carteBSejourIninterrompu?: boolean | null;
  carteBMotifStable?: boolean | null;

  /**
   * SF-221-03 — F-IM-55 statut de RÉSIDENT LONGUE DURÉE UE (BE, art. 15bis Loi
   * 15/12/1980, directive 2003/109/CE, 5 ans de séjour légal) — BELGIQUE uniquement.
   * 1 flag pivot CONTEXTUAL (`residenceLongueDureeUeDetecte`) + 4 champs de pré-fill.
   * `residenceLongueDureeUeDetecte` : contexte de statut résident longue durée UE détecté (pilote la visibilité).
   * `rlueDateDebutSejour` : date de début du séjour légal ininterrompu (ISO yyyy-MM-dd, non future).
   * `rlueRessourcesSuffisantes` : ressources stables, régulières et suffisantes.
   * `rlueAssuranceMaladie` : assurance maladie couvrant l'ensemble des risques.
   * `rlueIntegrationRemplie` : condition d'intégration remplie.
   */
  residenceLongueDureeUeDetecte?: boolean | null;
  rlueDateDebutSejour?: string | null;
  rlueRessourcesSuffisantes?: boolean | null;
  rlueAssuranceMaladie?: boolean | null;
  rlueIntegrationRemplie?: boolean | null;

  /**
   * SF-221-04 — F-IM-56 détention en CENTRE FERMÉ + requête de mise en liberté (BE,
   * art. 7/27/29/74/5 + 71 et s. Loi 15/12/1980, AR 02/08/2002) — BELGIQUE uniquement.
   * 1 flag pivot CONTEXTUAL (`detentionCentreFermeDetecte`) + 3 champs de pré-fill.
   * `detentionCentreFermeDetecte` : contexte de détention en centre fermé détecté (pilote la visibilité).
   * `detentionDateDebut` : date de début de la détention (ISO yyyy-MM-dd, non future).
   * `detentionBaseLegale` : base légale du maintien — ART_7 / ART_27 / ART_29 / ART_74_5 / AUTRE.
   * `detentionDateNotification` : date de notification de la décision de détention (ISO yyyy-MM-dd, non future).
   */
  detentionCentreFermeDetecte?: boolean | null;
  detentionDateDebut?: string | null;
  detentionBaseLegale?: string | null;
  detentionDateNotification?: string | null;
  /**
   * SF-221-05 — F-IM-57 recours CCE en SUSPENSION ORDINAIRE (référé administratif,
   * art. 39/82 Loi 15/12/1980, loi 15/09/2006) — BELGIQUE uniquement. Distinct de
   * l'annulation 30j (F-IM-31) et de l'extrême urgence 5j (F-IM-32).
   * 1 flag pivot CONTEXTUAL (`cceSuspensionDetecte`) + 3 champs de pré-fill.
   * `cceSuspensionDetecte` : contexte de recours en suspension détecté (pilote la visibilité).
   * `cceSuspensionDateNotification` : date de notification de la décision attaquée (ISO yyyy-MM-dd, non future).
   * `cceSuspensionUrgence` : urgence (non extrême) invocable / documentée.
   * `cceSuspensionPrejudiceGrave` : risque de préjudice grave difficilement réparable documenté.
   */
  cceSuspensionDetecte?: boolean | null;
  cceSuspensionDateNotification?: string | null;
  cceSuspensionUrgence?: boolean | null;
  cceSuspensionPrejudiceGrave?: boolean | null;
  /**
   * SF-221-06 — F-IM-58 titre de séjour VICTIME DE LA TRAITE DES ÊTRES HUMAINS
   * (art. 61/2 et s. Loi 15/12/1980, circulaire du 26/09/2008) — BELGIQUE uniquement.
   * Régime BE PROPRE (3 phases), distinct du pendant FR `victimeTraiteDetectee` (F-IM-35).
   * 1 flag pivot CONTEXTUAL (`victimeTraiteDetecte`) + 3 champs de pré-fill.
   * `victimeTraiteDetecte` : contexte de traite des êtres humains détecté (pilote la visibilité).
   * `victimeTraitePhase` : phase de la procédure (REFLEXION_45J / DECLARATION_FAITE / PROCEDURE_PENALE_EN_COURS / AUCUNE).
   * `victimeTraiteRupture` : rupture avec le réseau documentée.
   * `victimeTraiteAccompagnement` : accompagnement par un centre spécialisé agréé documenté.
   */
  victimeTraiteDetecte?: boolean | null;
  victimeTraitePhase?:
    | 'REFLEXION_45J'
    | 'DECLARATION_FAITE'
    | 'PROCEDURE_PENALE_EN_COURS'
    | 'AUCUNE'
    | null;
  victimeTraiteRupture?: boolean | null;
  victimeTraiteAccompagnement?: boolean | null;
}

export interface CaseAnalysisVersionSummary {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  updatedAt: string;
  faitsCount: number | null;
  pointsJuridiquesCount: number | null;
  risquesCount: number | null;
  questionsOuvertesCount: number | null;
  timelineCount: number | null;
}

export interface DiffItem {
  text: string;
  reason: string | null;
}

export interface SectionDiff {
  added: DiffItem[];
  removed: DiffItem[];
  unchanged: DiffItem[];
  enriched: DiffItem[];
}

export interface TimelineDiffItem {
  date: string;
  evenement: string;
  reason: string | null;
}

export interface TimelineSectionDiff {
  added: TimelineDiffItem[];
  removed: TimelineDiffItem[];
  unchanged: TimelineDiffItem[];
  enriched: TimelineDiffItem[];
}

export interface AnalysisDiff {
  from: { id: string; version: number; analysisType: string; updatedAt: string };
  to:   { id: string; version: number; analysisType: string; updatedAt: string };
  faits: SectionDiff;
  pointsJuridiques: SectionDiff;
  risques: SectionDiff;
  questionsOuvertes: SectionDiff;
  timeline: TimelineSectionDiff;
}
