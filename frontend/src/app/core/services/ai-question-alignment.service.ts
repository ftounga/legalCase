import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { AiQuestionAlignment } from '../models/ai-question-alignment.model';

/**
 * F-196 SF-196-02 — Lecture de l'alignement des questions complémentaires
 * répondues par l'avocat (F-94) avec les outils décisionnels qui peuvent en
 * bénéficier.
 *
 * Endpoint backend (importé de SF-196-01) :
 *   {@code GET /api/v1/case-files/{id}/ai-questions-alignment}
 *
 * <p>Comportement fail-open : 404, 500, timeout > 5 s → retourne `[]`.
 * La tile dashboard {@code F-196-questions-summary} n'apparaît pas — pattern
 * miroir {@link RetainedPisteAlignmentService} (F-192) +
 * {@link ProcedureCheckAlignmentService} (F-193) +
 * {@link PieceManquanteAlignmentService} (F-194) +
 * {@link RisqueAlignmentService} (F-195).</p>
 *
 * <p>Refresh :
 * <ol>
 *   <li>Au mount du dossier (call initial dans
 *   `<app-decisional-tools-panel>` / `<app-case-dashboard>`).</li>
 *   <li>À l'event SSE `ENRICHED_ANALYSIS DONE` propagé via
 *   `CaseDashboardRefreshService.refresh$`. <strong>Le PUT réponse F-94
 *   ne déclenche PAS de refresh côté frontend</strong> — cohérence stricte
 *   avec F-176 / F-192 / F-193 / F-194 / F-195 (la réponse est un acte
 *   sans matérialisation immédiate dans les outils ; la matérialisation
 *   se fait au prochain run de Synthèse enrichie).</li>
 * </ol></p>
 *
 * <p><strong>Contrat figé en avance</strong> : ce service précède le merge de
 * SF-196-01 (backend). Pattern utilisé pour SF-194-02 et SF-195-02.</p>
 */
@Injectable({ providedIn: 'root' })
export class AiQuestionAlignmentService {
  private readonly baseUrl = '/api/v1/case-files';
  private static readonly DEFAULT_TIMEOUT_MS = 5000;

  constructor(private http: HttpClient) {}

  getForCaseFile(caseFileId: string): Observable<AiQuestionAlignment[]> {
    return this.http
      .get<AiQuestionAlignment[]>(
        `${this.baseUrl}/${caseFileId}/ai-questions-alignment`,
      )
      .pipe(
        timeout(AiQuestionAlignmentService.DEFAULT_TIMEOUT_MS),
        catchError((err) => {
          // eslint-disable-next-line no-console
          console.warn('[ai-question-alignment] fail-open', err);
          return of([] as AiQuestionAlignment[]);
        }),
      );
  }
}
