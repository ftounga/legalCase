import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ReconnaissancePaternelleRequest,
  ReconnaissancePaternelleResponse,
} from '../models/reconnaissance-paternelle.model';

/**
 * SF-FA-18-02 : wrapper HttpClient pour l'outil décisionnel
 * "Reconnaissance paternelle" (F-FA-18). FR uniquement.
 *
 * Consomme l'API figée dans SF-FA-18-01 (backend, mergé PR #652).
 */
@Injectable({ providedIn: 'root' })
export class ReconnaissancePaternelleService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ReconnaissancePaternelleRequest):
      Observable<ReconnaissancePaternelleResponse> {
    return this.http.post<ReconnaissancePaternelleResponse>(
      `/api/v1/case-files/${caseFileId}/reconnaissance-paternelle-analysis`, request);
  }

  get(caseFileId: string): Observable<ReconnaissancePaternelleResponse> {
    return this.http.get<ReconnaissancePaternelleResponse>(
      `/api/v1/case-files/${caseFileId}/reconnaissance-paternelle-analysis`);
  }
}
