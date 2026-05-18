/**
 * F-242 SF-242-02 — citation de jurisprudence d'appui saisie par l'avocat,
 * rattachée à un point juridique d'un dossier (modèle miroir du backend
 * `JurisprudenceCitation` — SF-242-01).
 *
 * <p>À ne pas confondre avec `JurisprudenceCheck` (F-179) : F-179 vérifie les
 * arrêts détectés DANS les documents du dossier ; F-242 est ce que l'avocat
 * AJOUTE comme jurisprudence d'appui à un point juridique.</p>
 */
export interface JurisprudenceCitation {
  id: string;
  /** Index du point juridique dans la synthèse (snapshot au moment de la saisie). */
  pointJuridiqueIndex: number;
  /** Snapshot du texte du point juridique. */
  pointJuridiqueTexte: string;
  /** Référence de l'arrêt (ex. « Cass. soc. 12 oct. 2022, n° 21-12345 »). */
  reference: string;
  /** Une ligne de portée, optionnelle. */
  portee: string | null;
  /** Date de création ISO-8601. */
  createdAt: string;
  /** Date de dernière modification ISO-8601. */
  updatedAt: string;
}

/** Corps de `POST /api/v1/case-files/{id}/jurisprudence-citations`. */
export interface CreateJurisprudenceCitationRequest {
  pointJuridiqueIndex: number;
  pointJuridiqueTexte: string;
  reference: string;
  portee: string | null;
}

/** Corps de `PUT /api/v1/case-files/{id}/jurisprudence-citations/{citationId}`. */
export interface UpdateJurisprudenceCitationRequest {
  reference: string;
  portee: string | null;
}

/** Réponse de `GET /api/v1/case-files/{id}/jurisprudence-citations`. */
export interface JurisprudenceCitationListResponse {
  citations: JurisprudenceCitation[];
}
