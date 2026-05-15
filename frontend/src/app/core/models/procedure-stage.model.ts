// F-243 / SF-243-02 — Modèles du stade procédural du dossier.
// Alignés sur le contrat API figé de SF-243-01 (endpoints A/B/C).

/** Une juridiction du référentiel procédural (ex. Conseil de prud'hommes). */
export interface ProcedureJurisdictionOption {
  code: string;
  label: string;
}

/** Un stade rattaché à une juridiction parente. */
export interface ProcedureStageOption {
  code: string;
  label: string;
  jurisdictionCode: string;
}

/** Une position juridique valide pour un ou plusieurs stades. */
export interface ProcedurePositionOption {
  code: string;
  label: string;
  stageCodes: string[];
}

/**
 * Référentiel des valeurs possibles pour un domaine + pays donnés.
 * Endpoint A : GET /api/v1/procedure-stage/options?domain=&country=
 */
export interface ProcedureStageOptions {
  domain: string;
  country: string;
  jurisdictions: ProcedureJurisdictionOption[];
  stages: ProcedureStageOption[];
  positions: ProcedurePositionOption[];
}

/**
 * Stade procédural courant d'un dossier.
 * Endpoints B (GET) et C (PATCH) : /api/v1/case-files/{id}/procedure-stage
 * Les codes et leurs `*Label` valent `null` si non renseignés.
 */
export interface ProcedureStage {
  caseFileId: string;
  jurisdiction: string | null;
  jurisdictionLabel: string | null;
  stage: string | null;
  stageLabel: string | null;
  position: string | null;
  positionLabel: string | null;
}

/**
 * Corps de la requête PATCH — les 3 champs sont nullable.
 * Envoyer `null` efface le champ correspondant.
 */
export interface ProcedureStageUpdate {
  jurisdiction: string | null;
  stage: string | null;
  position: string | null;
}
