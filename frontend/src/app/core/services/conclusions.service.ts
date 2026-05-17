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
}
