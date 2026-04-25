import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OrdonnanceProtectionRequest,
  OrdonnanceProtectionResponse,
} from '../models/ordonnance-protection.model';

/**
 * SF-FA-14-02 : wrapper HttpClient pour l'outil décisionnel
 * "Ordonnance de protection" (F-FA-14, art. 515-9 à 515-13 Cciv).
 * Consomme l'API figée dans SF-FA-14-01 (backend PR #568).
 */
@Injectable({ providedIn: 'root' })
export class OrdonnanceProtectionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: OrdonnanceProtectionRequest):
      Observable<OrdonnanceProtectionResponse> {
    return this.http.post<OrdonnanceProtectionResponse>(
      `/api/v1/case-files/${caseFileId}/ordonnance-protection`, request);
  }

  get(caseFileId: string): Observable<OrdonnanceProtectionResponse> {
    return this.http.get<OrdonnanceProtectionResponse>(
      `/api/v1/case-files/${caseFileId}/ordonnance-protection`);
  }
}
