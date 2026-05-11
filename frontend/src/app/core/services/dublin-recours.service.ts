import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DublinRecoursRequest,
  DublinRecoursResponse,
} from '../models/dublin-recours.model';

/**
 * SF-208-06 : wrapper HttpClient pour l'outil decisionnel
 * "Dublin recours 7 j suspensif FR" (F-IM-22). FR uniquement.
 * Consomme l'API figee dans SF-208-02 (backend, PR #915).
 */
@Injectable({ providedIn: 'root' })
export class DublinRecoursService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: DublinRecoursRequest):
      Observable<DublinRecoursResponse> {
    return this.http.post<DublinRecoursResponse>(
      `/api/v1/case-files/${caseFileId}/dublin-recours-analysis`, request);
  }

  get(caseFileId: string): Observable<DublinRecoursResponse> {
    return this.http.get<DublinRecoursResponse>(
      `/api/v1/case-files/${caseFileId}/dublin-recours-analysis`);
  }
}
