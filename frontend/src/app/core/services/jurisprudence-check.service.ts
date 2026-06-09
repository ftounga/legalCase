import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  JurisprudenceCheck,
  JurisprudenceCheckResponse,
} from '../models/jurisprudence-check.model';

/**
 * F-179 SF-179-03 — accès aux vérifications de jurisprudence citée d'un dossier.
 */
@Injectable({ providedIn: 'root' })
export class JurisprudenceCheckService {
  constructor(private http: HttpClient) {}

  /** Renvoie les checks de la dernière analyse DONE du dossier. */
  getChecks(caseFileId: string): Observable<JurisprudenceCheckResponse> {
    return this.http.get<JurisprudenceCheckResponse>(
      `/api/v1/case-files/${caseFileId}/jurisprudence-checks`,
    );
  }

  /**
   * F-98 SF-98-56 — marque (ou démarque) une citation comme adverse à réfuter.
   * Autorisé côté serveur uniquement sur les statuts SUSPECT / NOT_FOUND.
   * Renvoie le check à jour.
   */
  markAdverse(
    caseFileId: string,
    checkId: string,
    markedAdverse: boolean,
  ): Observable<JurisprudenceCheck> {
    return this.http.patch<JurisprudenceCheck>(
      `/api/v1/case-files/${caseFileId}/jurisprudence-checks/${checkId}/adverse-marking`,
      { markedAdverse },
    );
  }
}
