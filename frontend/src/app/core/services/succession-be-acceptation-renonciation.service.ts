import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SuccessionBeAcceptationRenonciationRequest,
  SuccessionBeAcceptationRenonciationResponse,
} from '../models/succession-be-acceptation-renonciation.model';

/**
 * F-217 SF-217-13 : wrapper HttpClient pour l'outil décisionnel
 * "Succession BE — acceptation / renonciation"
 * (`succession-be-acceptation-renonciation`).
 * Consomme l'API figée dans SF-217-12 (backend).
 */
@Injectable({ providedIn: 'root' })
export class SuccessionBeAcceptationRenonciationService {
  constructor(private http: HttpClient) {}

  /** POST — analyse l'option et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: SuccessionBeAcceptationRenonciationRequest,
  ): Observable<SuccessionBeAcceptationRenonciationResponse> {
    return this.http.post<SuccessionBeAcceptationRenonciationResponse>(
      `/api/v1/case-files/${caseFileId}/succession-be-acceptation-renonciation`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(
    caseFileId: string,
  ): Observable<SuccessionBeAcceptationRenonciationResponse> {
    return this.http.get<SuccessionBeAcceptationRenonciationResponse>(
      `/api/v1/case-files/${caseFileId}/succession-be-acceptation-renonciation`,
    );
  }
}
