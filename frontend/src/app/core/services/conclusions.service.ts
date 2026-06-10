import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ConclusionGenerationResponse,
  ConclusionLifecycleStatus,
  ConclusionResponse,
  ConclusionVersionSummary,
} from '../models/conclusion.model';

/**
 * F-98 / SF-98-01 + SF-98-52 — Wrapper HttpClient pour la génération et le
 * versioning du projet de conclusions juridiques d'un dossier.
 *
 * Consomme le contrat API figé dans les mini-specs
 * `docs/features/F-98/SF-98-01-conclusions-cph-fond-fr.md` et
 * `docs/features/F-98/SF-98-52-versions-conclusions.md` :
 *  - `GET   /api/v1/case-files/{id}/conclusions` → version la plus récente
 *  - `POST  /api/v1/case-files/{id}/conclusions/generate` → nouvelle version (async)
 *  - `GET   /api/v1/case-files/{id}/conclusions/versions` → historique
 *  - `GET   /api/v1/case-files/{id}/conclusions/versions/{versionId}` → une version
 *  - `PATCH /api/v1/case-files/{id}/conclusions/versions/{versionId}/lifecycle`
 *  - `PATCH /api/v1/case-files/{id}/conclusions/versions/{versionId}/content` (SF-98-49)
 */
@Injectable({ providedIn: 'root' })
export class ConclusionsService {
  constructor(private http: HttpClient) {}

  /** État courant des conclusions du dossier (version la plus récente). */
  getConclusion(caseFileId: string): Observable<ConclusionResponse> {
    return this.http.get<ConclusionResponse>(
      `/api/v1/case-files/${caseFileId}/conclusions`,
    );
  }

  /**
   * Déclenche la génération asynchrone d'une nouvelle version du projet de
   * conclusions. Réponse `202` ; le suivi se fait ensuite par polling.
   */
  generate(caseFileId: string): Observable<ConclusionGenerationResponse> {
    return this.http.post<ConclusionGenerationResponse>(
      `/api/v1/case-files/${caseFileId}/conclusions/generate`,
      {},
    );
  }

  /**
   * SF-98-52 — Historique des versions de conclusions du dossier,
   * trié par numéro de version décroissant.
   */
  listVersions(caseFileId: string): Observable<ConclusionVersionSummary[]> {
    return this.http.get<ConclusionVersionSummary[]>(
      `/api/v1/case-files/${caseFileId}/conclusions/versions`,
    );
  }

  /** SF-98-52 — Contenu complet d'une version donnée. */
  getVersion(
    caseFileId: string,
    versionId: string,
  ): Observable<ConclusionResponse> {
    return this.http.get<ConclusionResponse>(
      `/api/v1/case-files/${caseFileId}/conclusions/versions/${versionId}`,
    );
  }

  /**
   * SF-98-52 — Fait évoluer le cycle de vie d'une version
   * (brouillon → validé → déposé, retours possibles).
   * Renvoie `409` si la version n'est pas `DONE` et qu'on vise
   * `VALIDATED`/`DEPOSITED`, `400` si la valeur est inconnue.
   */
  updateLifecycle(
    caseFileId: string,
    versionId: string,
    lifecycleStatus: ConclusionLifecycleStatus,
  ): Observable<ConclusionResponse> {
    return this.http.patch<ConclusionResponse>(
      `/api/v1/case-files/${caseFileId}/conclusions/versions/${versionId}/lifecycle`,
      { lifecycleStatus },
    );
  }

  /**
   * SF-98-49 — Met à jour le texte d'une version de conclusions.
   * Modification réservée à une version au statut de génération `DONE` et au
   * cycle de vie `DRAFT` : le backend renvoie `409` pour une version non
   * générée ou `VALIDATED`/`DEPOSITED`, `400` si `content` est vide.
   */
  updateContent(
    caseFileId: string,
    versionId: string,
    content: string,
  ): Observable<ConclusionResponse> {
    return this.http.patch<ConclusionResponse>(
      `/api/v1/case-files/${caseFileId}/conclusions/versions/${versionId}/content`,
      { content },
    );
  }

  /**
   * F-265 / SF-265-01 — Co-rédaction au paragraphe : régénère le markdown d'UNE
   * section de l'acte selon une instruction libre de l'avocat. Synchrone, sans
   * persistance : la réponse `{ regeneratedMarkdown }` est insérée dans le
   * brouillon (round-trip markdown F-264), l'avocat enregistre ensuite via
   * `updateContent`. Réservé à une version `DONE` + `DRAFT` (sinon `409`).
   */
  regenerateSection(
    caseFileId: string,
    versionId: string,
    sectionMarkdown: string,
    instruction: string,
  ): Observable<SectionRegenerationResponse> {
    return this.http.post<SectionRegenerationResponse>(
      `/api/v1/case-files/${caseFileId}/conclusions/versions/${versionId}/sections/regenerate`,
      { sectionMarkdown, instruction },
    );
  }
}

/** F-265 / SF-265-01 — réponse de la régénération d'une section. */
export interface SectionRegenerationResponse {
  regeneratedMarkdown: string;
}
