import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MineursImmigrationRequest,
  MineursImmigrationResponse,
} from '../models/mineurs-immigration.model';

/**
 * SF-IM-19-02 : wrapper HttpClient pour l'outil décisionnel "Mineurs
 * étrangers — éligibilité" (F-IM-19). FR uniquement (CESEDA + Cciv + CASF).
 *
 * Consomme l'API figée dans SF-IM-19-01 (backend, mergé PR #642).
 */
@Injectable({ providedIn: 'root' })
export class MineursImmigrationService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: MineursImmigrationRequest): Observable<MineursImmigrationResponse> {
    return this.http.post<MineursImmigrationResponse>(
      `/api/v1/case-files/${caseFileId}/mineurs-immigration-analysis`, request);
  }

  get(caseFileId: string): Observable<MineursImmigrationResponse> {
    return this.http.get<MineursImmigrationResponse>(
      `/api/v1/case-files/${caseFileId}/mineurs-immigration-analysis`);
  }
}
