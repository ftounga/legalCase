import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SourceExplanation } from '../models/source-explanation.model';

/**
 * Service client pour l'endpoint transversal F-IA-03-15a.
 * SF-IA-03-18 : pas de cache — re-fetch à chaque appel pour rester frais
 * après ré-analyse. Volume léger (~10-15 rows par dossier).
 */
@Injectable({ providedIn: 'root' })
export class SourceExplanationService {
  constructor(private http: HttpClient) {}

  /**
   * Retourne la map {sourceKey → explanation} pour un dossier.
   * Fail-open : 404 ou erreur → map vide.
   */
  getForCaseFile(caseFileId: string): Observable<Map<string, SourceExplanation[]>> {
    return this.http.get<SourceExplanation[]>(`/api/v1/case-files/${caseFileId}/source-explanations`).pipe(
      map(list => {
        const out = new Map<string, SourceExplanation[]>();
        (list ?? []).forEach(e => {
          const arr = out.get(e.sourceKey) ?? [];
          arr.push(e);
          out.set(e.sourceKey, arr);
        });
        return out;
      }),
    );
  }

  /** Compat rétro : conservé comme no-op pour éviter de casser les appels existants. */
  invalidate(_caseFileId: string): void {
    // no-op : plus de cache.
  }
}
