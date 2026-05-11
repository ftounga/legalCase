import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HeuresSupRequest,
  HeuresSupResponse,
} from '../models/heures-sup.model';

/**
 * SF-DT-19-02 : wrapper HttpClient pour l'outil décisionnel
 * "Calculateur heures supplémentaires" (F-DT-19).
 * Consomme l'API figée dans SF-DT-19-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class HeuresSupService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-19-heures-sup';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: HeuresSupRequest):
      Observable<HeuresSupResponse> {
    return this.http.post<HeuresSupResponse>(
      `/api/v1/case-files/${caseFileId}/heures-sup`, request);
  }

  get(caseFileId: string): Observable<HeuresSupResponse> {
    return this.http.get<HeuresSupResponse>(
      `/api/v1/case-files/${caseFileId}/heures-sup`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: HeuresSupRequest): Observable<HeuresSupResponse> {
    return this.http.post<HeuresSupResponse>(
      `/api/v1/simulators/${HeuresSupService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
