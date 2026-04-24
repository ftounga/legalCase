import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MotifGraveBeRequest,
  MotifGraveBeResponse,
} from '../models/motif-grave-be.model';

/**
 * SF-DT-27-02 : wrapper HttpClient pour l'outil décisionnel
 * "Motif grave BE" (F-DT-27). BE uniquement.
 * Consomme l'API figée dans SF-DT-27-01 (backend, PR #497).
 */
@Injectable({ providedIn: 'root' })
export class MotifGraveBeService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: MotifGraveBeRequest):
      Observable<MotifGraveBeResponse> {
    return this.http.post<MotifGraveBeResponse>(
      `/api/v1/case-files/${caseFileId}/motif-grave-be`, request);
  }

  get(caseFileId: string): Observable<MotifGraveBeResponse> {
    return this.http.get<MotifGraveBeResponse>(
      `/api/v1/case-files/${caseFileId}/motif-grave-be`);
  }
}
