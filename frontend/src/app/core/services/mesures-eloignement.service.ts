import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MesuresEloignementRequest,
  MesuresEloignementResponse,
} from '../models/mesures-eloignement.model';

/**
 * SF-IM-20-02 : wrapper HttpClient pour l'outil "Mesures d'éloignement
 * avancées" (F-IM-20). FR uniquement (CESEDA L.631+/L.612+/L.222+).
 * Consomme l'API figée dans SF-IM-20-01 (backend, mergé PR #645).
 */
@Injectable({ providedIn: 'root' })
export class MesuresEloignementService {

  constructor(private http: HttpClient) {}

  calculate(
    caseFileId: string,
    request: MesuresEloignementRequest,
  ): Observable<MesuresEloignementResponse> {
    return this.http.post<MesuresEloignementResponse>(
      `/api/v1/case-files/${caseFileId}/mesures-eloignement-analysis`, request);
  }

  get(caseFileId: string): Observable<MesuresEloignementResponse> {
    return this.http.get<MesuresEloignementResponse>(
      `/api/v1/case-files/${caseFileId}/mesures-eloignement-analysis`);
  }
}
