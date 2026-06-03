import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdoptionBeRequest,
  AdoptionBeResponse,
} from '../models/adoption-be.model';

/**
 * SF-223-02 : wrapper HttpClient pour l'outil décisionnel "Recevabilité de
 * l'adoption en Belgique" (`adoption-be`). Consomme l'API figée dans SF-223-02
 * (backend).
 */
@Injectable({ providedIn: 'root' })
export class AdoptionBeService {
  constructor(private http: HttpClient) {}

  /** POST — analyse la recevabilité de l'adoption et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: AdoptionBeRequest,
  ): Observable<AdoptionBeResponse> {
    return this.http.post<AdoptionBeResponse>(
      `/api/v1/case-files/${caseFileId}/adoption-be-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AdoptionBeResponse> {
    return this.http.get<AdoptionBeResponse>(
      `/api/v1/case-files/${caseFileId}/adoption-be-analysis`,
    );
  }
}
