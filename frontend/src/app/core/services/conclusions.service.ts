import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ConclusionGenerationResponse,
  ConclusionResponse,
} from '../models/conclusion.model';

/**
 * F-98 / SF-98-01 — Wrapper HttpClient pour la génération du projet de
 * conclusions juridiques d'un dossier.
 *
 * Consomme le contrat API figé dans la mini-spec
 * `docs/features/F-98/SF-98-01-conclusions-cph-fond-fr.md` :
 *  - `GET  /api/v1/case-files/{id}/conclusions` → état courant
 *  - `POST /api/v1/case-files/{id}/conclusions/generate` → déclenchement async
 */
@Injectable({ providedIn: 'root' })
export class ConclusionsService {
  constructor(private http: HttpClient) {}

  /** État courant des conclusions du dossier. */
  getConclusion(caseFileId: string): Observable<ConclusionResponse> {
    return this.http.get<ConclusionResponse>(
      `/api/v1/case-files/${caseFileId}/conclusions`,
    );
  }

  /**
   * Déclenche la génération asynchrone du projet de conclusions.
   * Réponse `202` ; le suivi se fait ensuite par polling de `getConclusion`.
   */
  generate(caseFileId: string): Observable<ConclusionGenerationResponse> {
    return this.http.post<ConclusionGenerationResponse>(
      `/api/v1/case-files/${caseFileId}/conclusions/generate`,
      {},
    );
  }
}
