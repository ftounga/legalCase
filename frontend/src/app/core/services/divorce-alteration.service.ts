import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceAlterationRequest,
  DivorceAlterationResponse,
} from '../models/divorce-alteration.model';

/**
 * SF-FA-08-02 : wrapper HttpClient pour l'outil décisionnel
 * "Divorce pour altération définitive du lien conjugal" (F-FA-08).
 * Consomme l'API figée dans SF-FA-08-01 (backend PR #513).
 */
@Injectable({ providedIn: 'root' })
export class DivorceAlterationService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceAlterationRequest):
      Observable<DivorceAlterationResponse> {
    return this.http.post<DivorceAlterationResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-alteration`, request);
  }

  get(caseFileId: string): Observable<DivorceAlterationResponse> {
    return this.http.get<DivorceAlterationResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-alteration`);
  }
}
