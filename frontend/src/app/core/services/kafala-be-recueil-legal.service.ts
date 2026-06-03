import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  KafalaBeRequest,
  KafalaBeResponse,
} from '../models/kafala-be-recueil-legal.model';

/**
 * SF-223-03 : wrapper HttpClient pour l'outil décisionnel "Recueil légal
 * (kafala) — Belgique" (`kafala-be-recueil-legal`). Consomme l'API figée dans
 * SF-223-03 (backend).
 */
@Injectable({ providedIn: 'root' })
export class KafalaBeRecueilLegalService {
  constructor(private http: HttpClient) {}

  /** POST — qualifie le sort de la kafala et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: KafalaBeRequest,
  ): Observable<KafalaBeResponse> {
    return this.http.post<KafalaBeResponse>(
      `/api/v1/case-files/${caseFileId}/kafala-be-recueil-legal-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<KafalaBeResponse> {
    return this.http.get<KafalaBeResponse>(
      `/api/v1/case-files/${caseFileId}/kafala-be-recueil-legal-analysis`,
    );
  }
}
