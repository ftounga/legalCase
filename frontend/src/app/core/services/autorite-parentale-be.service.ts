import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AutoriteParentaleBeRequest,
  AutoriteParentaleBeResponse,
} from '../models/autorite-parentale-be.model';

/**
 * SF-217-05 : wrapper HttpClient pour l'outil décisionnel "Autorité parentale
 * (Belgique)" (Vague 2 Famille BE). Consomme l'API figée dans SF-217-04 (backend).
 */
@Injectable({ providedIn: 'root' })
export class AutoriteParentaleBeService {
  constructor(private http: HttpClient) {}

  /** POST — qualifie et persiste l'analyse d'autorité parentale du dossier. */
  calculate(
    caseFileId: string,
    request: AutoriteParentaleBeRequest,
  ): Observable<AutoriteParentaleBeResponse> {
    return this.http.post<AutoriteParentaleBeResponse>(
      `/api/v1/case-files/${caseFileId}/autorite-parentale-be`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AutoriteParentaleBeResponse> {
    return this.http.get<AutoriteParentaleBeResponse>(
      `/api/v1/case-files/${caseFileId}/autorite-parentale-be`,
    );
  }
}
