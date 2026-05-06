import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  TypeLitigeOverridePayload,
  TypeLitigeOverrideResponse,
} from '../models/type-litige-override.model';

/**
 * F-197 SF-197-02 — Persistance de l'override single-value du type de litige
 * (Travail FR) ou du type de procédure (Immigration) décidé manuellement par
 * l'avocat.
 *
 * <p>Endpoints backend (importés de SF-197-01) :</p>
 * <ul>
 *   <li>{@code PUT /api/v1/case-files/{id}/type-litige-override}
 *       body : <code>{ type: string, raison?: string }</code> → 200</li>
 *   <li>{@code GET /api/v1/case-files/{id}/type-litige-override} → 200
 *       <code>{ typeLitigeAvocat?, typeProcedureAvocat?, raison? }</code></li>
 * </ul>
 *
 * <p>Cohérence F-176 stricte : le PUT est un acte pur côté backend (pas de
 * recompute, pas de side-effect). La propagation outils (TOOL_REGISTRY
 * `aiData.typeLitigeAvocatOverride`) ainsi que l'éventuel re-flagging par
 * l'IA ne se font qu'au prochain run de Synthèse enrichie via l'event SSE
 * {@code ENRICHED_ANALYSIS DONE}. Aucun refresh côté frontend après PUT.</p>
 *
 * <p>Fail-fast : 400 / 401 / 403 / 404 / 5xx remontent au composant appelant
 * qui doit afficher un snackbar et garder le dialog ouvert (CA-erreur
 * SF-197-02).</p>
 */
@Injectable({ providedIn: 'root' })
export class TypeLitigeOverrideService {
  private readonly baseUrl = '/api/v1/case-files';

  constructor(private http: HttpClient) {}

  /** PUT — pose ou met à jour l'override single-value pour le dossier. */
  update(
    caseFileId: string,
    payload: TypeLitigeOverridePayload,
  ): Observable<TypeLitigeOverrideResponse> {
    return this.http.put<TypeLitigeOverrideResponse>(
      `${this.baseUrl}/${caseFileId}/type-litige-override`,
      {
        type: payload.type,
        raison: payload.raison ?? null,
      },
    );
  }

  /**
   * GET — lit l'override courant. Retourne tous les champs à {@code null} si
   * aucun override n'a été posé. Fail-open silencieux côté composant
   * appelant si timeout / 5xx (CA-09 SF-197-02).
   */
  getForCaseFile(caseFileId: string): Observable<TypeLitigeOverrideResponse> {
    return this.http.get<TypeLitigeOverrideResponse>(
      `${this.baseUrl}/${caseFileId}/type-litige-override`,
    );
  }
}
