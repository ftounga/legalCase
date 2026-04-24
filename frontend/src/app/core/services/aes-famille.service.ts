import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AesFamilleRequest, AesFamilleResponse } from '../models/aes-famille.model';

/**
 * SF-IM-09-06 : wrapper HttpClient pour l'outil décisionnel AES voie familiale
 * (L.435-1 CESEDA + circulaire Valls 28/11/2012). Consomme l'API figée dans
 * SF-IM-09-02 (PR #506).
 */
@Injectable({ providedIn: 'root' })
export class AesFamilleService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AesFamilleRequest): Observable<AesFamilleResponse> {
    return this.http.post<AesFamilleResponse>(
      `/api/v1/case-files/${caseFileId}/aes-famille`, request);
  }

  get(caseFileId: string): Observable<AesFamilleResponse> {
    return this.http.get<AesFamilleResponse>(
      `/api/v1/case-files/${caseFileId}/aes-famille`);
  }
}
