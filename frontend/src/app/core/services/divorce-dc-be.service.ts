import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceDcBeRequest,
  DivorceDcBeResponse,
} from '../models/divorce-dc-be.model';

/**
 * F-243 : service HTTP pour l'outil "Divorce par consentement mutuel — Belgique"
 * (F-FA-11 BE). Wrappe les endpoints `POST` / `GET`
 * `/api/v1/case-files/{id}/divorce-dc-be-analysis` exposés par
 * `DivorceDcBeController` (backend SF-211-01).
 */
@Injectable({ providedIn: 'root' })
export class DivorceDcBeService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceDcBeRequest): Observable<DivorceDcBeResponse> {
    return this.http.post<DivorceDcBeResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-dc-be-analysis`,
      request,
    );
  }

  get(caseFileId: string): Observable<DivorceDcBeResponse> {
    return this.http.get<DivorceDcBeResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-dc-be-analysis`,
    );
  }
}
