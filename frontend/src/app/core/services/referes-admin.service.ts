import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ReferesAdminRequest,
  ReferesAdminResponse,
} from '../models/referes-admin.model';

/**
 * SF-IM-08-08 : wrapper HttpClient pour l'outil décisionnel
 * "Référés administratifs L.521-1 / L.521-2 — France" (F-IM-08).
 * FR uniquement. Consomme l'API figée dans SF-IM-08-07 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ReferesAdminService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ReferesAdminRequest):
      Observable<ReferesAdminResponse> {
    return this.http.post<ReferesAdminResponse>(
      `/api/v1/case-files/${caseFileId}/referes-admin`, request);
  }

  get(caseFileId: string): Observable<ReferesAdminResponse> {
    return this.http.get<ReferesAdminResponse>(
      `/api/v1/case-files/${caseFileId}/referes-admin`);
  }
}
