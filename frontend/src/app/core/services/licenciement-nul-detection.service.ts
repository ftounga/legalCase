import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementNulDetectionRequest,
  LicenciementNulDetectionResponse,
} from '../models/licenciement-nul-detection.model';

/**
 * SF-DT-16-02 : wrapper HttpClient pour l'outil décisionnel
 * "Licenciement nul — détection multi-protections + indemnité plancher 6 mois"
 * (F-DT-16). Consomme l'API figée dans SF-DT-16-01 (backend, PR #580).
 */
@Injectable({ providedIn: 'root' })
export class LicenciementNulDetectionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: LicenciementNulDetectionRequest):
      Observable<LicenciementNulDetectionResponse> {
    return this.http.post<LicenciementNulDetectionResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement-nul-detection`, request);
  }

  get(caseFileId: string): Observable<LicenciementNulDetectionResponse> {
    return this.http.get<LicenciementNulDetectionResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement-nul-detection`);
  }
}
