import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ContestationAreRequest,
  ContestationAreResponse,
} from '../models/contestation-are.model';

/**
 * SF-DT-35-02 : wrapper HttpClient pour l'outil décisionnel
 * "Contestation ARE / France Travail (ex-Pôle emploi)" (F-DT-35).
 * FR uniquement. Consomme l'API figée dans SF-DT-35-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ContestationAreService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-35-contestation-are-fr';

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ContestationAreRequest):
      Observable<ContestationAreResponse> {
    return this.http.post<ContestationAreResponse>(
      `/api/v1/case-files/${caseFileId}/contestation-are`, request);
  }

  get(caseFileId: string): Observable<ContestationAreResponse> {
    return this.http.get<ContestationAreResponse>(
      `/api/v1/case-files/${caseFileId}/contestation-are`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  analyzeStandalone(request: ContestationAreRequest): Observable<ContestationAreResponse> {
    return this.http.post<ContestationAreResponse>(
      `/api/v1/simulators/${ContestationAreService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
