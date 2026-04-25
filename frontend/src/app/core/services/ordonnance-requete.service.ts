import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OrdonnanceRequeteRequest,
  OrdonnanceRequeteResponse,
} from '../models/ordonnance-requete.model';

/**
 * SF-FA-23-02 : wrapper HttpClient pour l'outil décisionnel "Ordonnance sur
 * requête" (F-FA-23). Applicable FRANCE et BELGIQUE — la procédure existe à
 * l'identique dans les 2 pays (art. 493 CPC FR / art. 1025 CJ BE).
 *
 * Consomme l'API figée dans SF-FA-23-01 (backend, mergé PR #637).
 */
@Injectable({ providedIn: 'root' })
export class OrdonnanceRequeteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: OrdonnanceRequeteRequest): Observable<OrdonnanceRequeteResponse> {
    return this.http.post<OrdonnanceRequeteResponse>(
      `/api/v1/case-files/${caseFileId}/ordonnance-requete-analysis`, request);
  }

  get(caseFileId: string): Observable<OrdonnanceRequeteResponse> {
    return this.http.get<OrdonnanceRequeteResponse>(
      `/api/v1/case-files/${caseFileId}/ordonnance-requete-analysis`);
  }
}
