import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';
import { PieceManquanteAlignment } from '../models/piece-manquante-alignment.model';

/**
 * F-194 SF-194-02 — Lecture de l'alignement des pièces manquantes (statuts
 * décidés par l'avocat dans la synthèse) avec les outils décisionnels qui en
 * dépendent.
 *
 * Endpoint backend (importé de SF-194-01) :
 *   {@code GET /api/v1/case-files/{id}/pieces-manquantes-alignment}
 *
 * <p>Comportement fail-open : 404, 500, timeout > 5 s → retourne `[]`.
 * Les sorties outils omettent silencieusement les pré-cochages
 * `piecesObtenues` et la tile dashboard {@code F-194-pieces-summary}
 * n'apparaît pas — pattern miroir
 * {@link RetainedPisteAlignmentService} (F-192) +
 * {@link ProcedureCheckAlignmentService} (F-193).</p>
 *
 * <p>Refresh :
 * <ol>
 *   <li>Au mount du dossier (call initial dans
 *   `<app-decisional-tools-panel>` / `<app-case-dashboard>`).</li>
 *   <li>À l'event SSE `ENRICHED_ANALYSIS DONE` propagé via
 *   `CaseDashboardRefreshService.refresh$`. <strong>Le PUT statut pièce
 *   ne déclenche PAS de refresh côté frontend</strong> — cohérence stricte
 *   avec F-176 / F-192 / F-193 (le statut est un acte sans matérialisation
 *   immédiate dans les outils).</li>
 * </ol></p>
 */
@Injectable({ providedIn: 'root' })
export class PieceManquanteAlignmentService {
  private readonly baseUrl = '/api/v1/case-files';
  private static readonly DEFAULT_TIMEOUT_MS = 5000;

  constructor(private http: HttpClient) {}

  getForCaseFile(caseFileId: string): Observable<PieceManquanteAlignment[]> {
    return this.http
      .get<PieceManquanteAlignment[]>(
        `${this.baseUrl}/${caseFileId}/pieces-manquantes-alignment`,
      )
      .pipe(
        timeout(PieceManquanteAlignmentService.DEFAULT_TIMEOUT_MS),
        catchError((err) => {
          // eslint-disable-next-line no-console
          console.warn('[piece-manquante-alignment] fail-open', err);
          return of([] as PieceManquanteAlignment[]);
        }),
      );
  }
}
