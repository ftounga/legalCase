import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { RetainedPisteAlignment } from '../models/retained-piste-alignment.model';

/**
 * F-192 SF-192-02 / SF-192-03 — Lecture de l'alignement des pistes 🟢 Retenue
 * d'un dossier avec les outils décisionnels cibles.
 *
 * Endpoint backend : GET /api/v1/case-files/{id}/retained-pistes-alignment
 *
 * Comportement fail-open : 404, 500, timeout > 5 s → retourne `[]`. L'export
 * PDF synthèse omet alors silencieusement la section « Stratégies retenues »
 * sans bloquer l'avocat (cf. CA-10 SF-192-03).
 */
@Injectable({ providedIn: 'root' })
export class RetainedPisteAlignmentService {
  private readonly baseUrl = '/api/v1/case-files';
  private static readonly DEFAULT_TIMEOUT_MS = 5000;

  constructor(private http: HttpClient) {}

  getForCaseFile(caseFileId: string): Observable<RetainedPisteAlignment[]> {
    return this.http
      .get<RetainedPisteAlignment[]>(
        `${this.baseUrl}/${caseFileId}/retained-pistes-alignment`,
      )
      .pipe(
        timeout(RetainedPisteAlignmentService.DEFAULT_TIMEOUT_MS),
        catchError((err) => {
          // eslint-disable-next-line no-console
          console.warn('[retained-piste-alignment] fail-open', err);
          return of([] as RetainedPisteAlignment[]);
        }),
      );
  }
}
