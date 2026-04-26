import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DevolutionLegaleRequest,
  DevolutionLegaleResponse,
} from '../models/devolution-legale.model';

/**
 * SF-FA-24-02 : wrapper HttpClient pour l'outil décisionnel "Dévolution
 * légale successorale" (F-FA-24). FR uniquement — art. 731 et s. Cciv.
 * Consomme l'API figée dans SF-FA-24-01 (backend, mergé PR #651).
 */
@Injectable({ providedIn: 'root' })
export class DevolutionLegaleService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DevolutionLegaleRequest): Observable<DevolutionLegaleResponse> {
    return this.http.post<DevolutionLegaleResponse>(
      `/api/v1/case-files/${caseFileId}/devolution-legale-analysis`, request);
  }

  get(caseFileId: string): Observable<DevolutionLegaleResponse> {
    return this.http.get<DevolutionLegaleResponse>(
      `/api/v1/case-files/${caseFileId}/devolution-legale-analysis`);
  }
}
