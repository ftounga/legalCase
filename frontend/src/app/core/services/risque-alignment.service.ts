import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { RisqueAlignment } from '../models/risque-alignment.model';

/**
 * F-195 SF-195-02 — Lecture de l'alignement des risques (statuts décidés par
 * l'avocat dans la synthèse) avec les outils décisionnels qui en dépendent.
 *
 * Endpoint backend (importé de SF-195-01) :
 *   {@code GET /api/v1/case-files/{id}/risques-alignment}
 *
 * <p>Comportement fail-open : 404, 500, timeout > 5 s → retourne `[]`.
 * Pattern miroir {@link RetainedPisteAlignmentService} (F-192) +
 * {@link ProcedureCheckAlignmentService} (F-193) +
 * {@link PieceManquanteAlignmentService} (F-194).</p>
 *
 * <p>Refresh :
 * <ol>
 *   <li>Au mount du dossier (call initial).</li>
 *   <li>À l'event SSE `ENRICHED_ANALYSIS DONE` propagé via
 *   `CaseDashboardRefreshService.refresh$`. <strong>Le PUT statut risque
 *   ne déclenche PAS de refresh côté frontend</strong> — cohérence stricte
 *   avec F-176 / F-192 / F-193 / F-194.</li>
 * </ol></p>
 *
 * <p><strong>Contrat figé en avance</strong> : ce service précède le merge de
 * SF-195-02 et SF-195-01. Pattern utilisé pour SF-193-03 et SF-194-03.</p>
 */
@Injectable({ providedIn: 'root' })
export class RisqueAlignmentService {
  private readonly baseUrl = '/api/v1/case-files';
  private static readonly DEFAULT_TIMEOUT_MS = 5000;

  constructor(private http: HttpClient) {}

  getForCaseFile(caseFileId: string): Observable<RisqueAlignment[]> {
    return this.http
      .get<RisqueAlignment[]>(`${this.baseUrl}/${caseFileId}/risques-alignment`)
      .pipe(
        timeout(RisqueAlignmentService.DEFAULT_TIMEOUT_MS),
        catchError((err) => {
          // eslint-disable-next-line no-console
          console.warn('[risque-alignment] fail-open', err);
          return of([] as RisqueAlignment[]);
        }),
      );
  }
}
