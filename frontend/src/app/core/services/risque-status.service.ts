import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  RisqueStatus,
  RisqueStatusPayload,
  RisqueStatutValue,
} from '../models/risque-status.model';

/**
 * F-195 SF-195-02 — Persistance du statut décidé par l'avocat sur un risque
 * listé par la synthèse.
 *
 * Endpoint backend (importé de SF-195-01) :
 *   {@code PUT /api/v1/case-files/{id}/risques/status}
 *   body : <code>{ risqueLibelleOriginal, statut, raisonEcarte? }</code>
 *
 * <p>Cohérence F-176 stricte : le PUT est un acte pur côté backend (pas de
 * recompute, pas de side-effect). La matérialisation risque → outil
 * ({@link RisqueAlignment}) et le recompute du `score_risque_avocat` ne se
 * font qu'au prochain run de Synthèse enrichie via l'event SSE
 * {@code ENRICHED_ANALYSIS DONE}. Aucun refresh côté frontend après PUT.</p>
 *
 * <p>Fail-fast : 400 / 401 / 403 / 404 / 5xx remontent au composant appelant
 * qui doit afficher un snackbar et rollback son optimistic update.</p>
 */
@Injectable({ providedIn: 'root' })
export class RisqueStatusService {
  private readonly baseUrl = '/api/v1/case-files';

  constructor(private http: HttpClient) {}

  update(
    caseFileId: string,
    payload: RisqueStatusPayload,
  ): Observable<RisqueStatus> {
    return this.http.put<RisqueStatus>(
      `${this.baseUrl}/${caseFileId}/risques/status`,
      payload,
    );
  }

  /**
   * Helper de convenance : permet aux composants d'appeler
   * `updateStatus(id, libelle, 'VALIDE')` sans avoir à construire le payload
   * à chaque fois.
   */
  updateStatus(
    caseFileId: string,
    risqueLibelleOriginal: string,
    statut: RisqueStatutValue,
    options?: { raisonEcarte?: string | null },
  ): Observable<RisqueStatus> {
    return this.update(caseFileId, {
      risqueLibelleOriginal,
      statut,
      raisonEcarte: options?.raisonEcarte ?? null,
    });
  }
}
