import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { JurisprudenceApplicableResponse } from '../models/jurisprudence-applicable.model';

/**
 * F-JU-02 / SF-JU-02-02 — Lecture de la jurisprudence applicable agrégée pour
 * un dossier (arrêts mappés F-JU-01 des outils décisionnels utilisés).
 *
 * Endpoint backend :
 *   {@code GET /api/v1/case-files/{id}/jurisprudence-applicable}
 *
 * <p>Comportement fail-open : 404, 500, timeout > 5 s → retourne
 * {@code { entries: [] }} (section omise du PDF synthèse). Pattern miroir
 * {@code AiQuestionAlignmentService} (F-196) /
 * {@code RisqueAlignmentService} (F-195).</p>
 *
 * <p>L'export PDF n'est jamais bloqué par cette section optionnelle —
 * invariant F-JU-02 SF-02 : la jurisprudence applicable est un enrichissement
 * additionnel, l'absence d'arrêts (cas nominal V1 tant qu'aucun
 * {@code ToolUsageContributor} n'est branché) ne dégrade pas la synthèse.</p>
 */
@Injectable({ providedIn: 'root' })
export class JurisprudenceApplicableService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/case-files';
  private static readonly DEFAULT_TIMEOUT_MS = 5000;
  private static readonly EMPTY: JurisprudenceApplicableResponse = { entries: [] };

  getJurisprudenceApplicable(caseFileId: string): Observable<JurisprudenceApplicableResponse> {
    return this.http
      .get<JurisprudenceApplicableResponse>(
        `${this.baseUrl}/${caseFileId}/jurisprudence-applicable`,
      )
      .pipe(
        timeout(JurisprudenceApplicableService.DEFAULT_TIMEOUT_MS),
        catchError((err) => {
          // eslint-disable-next-line no-console
          console.warn('[jurisprudence-applicable] fail-open', err);
          return of(JurisprudenceApplicableService.EMPTY);
        }),
      );
  }
}
