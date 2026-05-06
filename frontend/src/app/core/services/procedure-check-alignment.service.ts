import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { ProcedureCheckAlignment } from '../models/procedure-check-alignment.model';

/**
 * F-193 SF-193-02 — Lecture de l'alignement des checks procéduraux F-96
 * d'un dossier avec les outils décisionnels cibles.
 *
 * Endpoint backend : GET /api/v1/case-files/{id}/procedure-checks-alignment
 *
 * Comportement fail-open : 404, 500, timeout > 5 s → retourne `[]`.
 * Les sorties outils omettent silencieusement les blocs "Vérifications
 * confirmées / non conformes / à vérifier" et la tile dashboard
 * `F-193-procedure-checks-summary` n'apparaît pas — pattern miroir
 * {@link RetainedPisteAlignmentService} (F-192).
 *
 * Refresh :
 * <ol>
 *   <li>Au mount du dossier (call initial dans
 *   `<app-decisional-tools-panel>` / `<app-case-dashboard>`).</li>
 *   <li>À l'event SSE `ENRICHED_ANALYSIS DONE` propagé via
 *   `CaseDashboardRefreshService.refresh$`. <strong>Le PUT statut check
 *   F-96 ne déclenche PAS de refresh côté frontend</strong> — cohérence
 *   stricte avec F-96 / F-192 (le statut est un acte sans matérialisation
 *   immédiate dans les outils).</li>
 * </ol>
 */
@Injectable({ providedIn: 'root' })
export class ProcedureCheckAlignmentService {
  private readonly baseUrl = '/api/v1/case-files';
  private static readonly DEFAULT_TIMEOUT_MS = 5000;

  constructor(private http: HttpClient) {}

  getForCaseFile(caseFileId: string): Observable<ProcedureCheckAlignment[]> {
    return this.http
      .get<ProcedureCheckAlignment[]>(
        `${this.baseUrl}/${caseFileId}/procedure-checks-alignment`,
      )
      .pipe(
        timeout(ProcedureCheckAlignmentService.DEFAULT_TIMEOUT_MS),
        catchError((err) => {
          // eslint-disable-next-line no-console
          console.warn('[procedure-check-alignment] fail-open', err);
          return of([] as ProcedureCheckAlignment[]);
        }),
      );
  }
}
