import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { SourceExplanation } from '../models/source-explanation.model';

/**
 * Service client pour l'endpoint transversal F-IA-03-15a.
 * Un cache par caseFileId évite les appels répétés (les explications
 * changent uniquement quand une nouvelle analyse IA est lancée).
 */
@Injectable({ providedIn: 'root' })
export class SourceExplanationService {
  private cache = new Map<string, Map<string, SourceExplanation>>();

  constructor(private http: HttpClient) {}

  /**
   * Retourne la map {sourceKey → explanation} pour un dossier.
   * Fail-open : 404 ou erreur → map vide.
   */
  getForCaseFile(caseFileId: string): Observable<Map<string, SourceExplanation>> {
    const cached = this.cache.get(caseFileId);
    if (cached) return of(cached);

    return this.http.get<SourceExplanation[]>(`/api/v1/case-files/${caseFileId}/source-explanations`).pipe(
      map(list => {
        const map = new Map<string, SourceExplanation>();
        (list ?? []).forEach(e => map.set(e.sourceKey, e));
        return map;
      }),
      tap(map => this.cache.set(caseFileId, map)),
    );
  }

  /** Efface le cache pour un dossier (à appeler après une nouvelle analyse). */
  invalidate(caseFileId: string): void {
    this.cache.delete(caseFileId);
  }
}
