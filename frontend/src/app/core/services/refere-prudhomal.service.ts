import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ReferePrudhomalRequest,
  ReferePrudhomalResponse,
} from '../models/refere-prudhomal.model';

/**
 * SF-DT-34-02 : wrapper HttpClient pour l'outil décisionnel
 * "Référé prud'homal — France" (F-DT-34, art. R.1454-1 Code du travail).
 * FR uniquement. Consomme l'API figée par SF-DT-34-01 (backend, parallèle).
 */
@Injectable({ providedIn: 'root' })
export class ReferePrudhomalService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ReferePrudhomalRequest):
      Observable<ReferePrudhomalResponse> {
    return this.http.post<ReferePrudhomalResponse>(
      `/api/v1/case-files/${caseFileId}/refere-prudhomal`, request);
  }

  get(caseFileId: string): Observable<ReferePrudhomalResponse> {
    return this.http.get<ReferePrudhomalResponse>(
      `/api/v1/case-files/${caseFileId}/refere-prudhomal`);
  }
}
