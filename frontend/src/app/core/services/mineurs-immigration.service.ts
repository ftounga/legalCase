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
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-19-mineurs')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-19-mineurs';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: MineursImmigrationRequest): Observable<MineursImmigrationResponse> {
    return this.http.post<MineursImmigrationResponse>(
      `/api/v1/simulators/${MineursImmigrationService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
