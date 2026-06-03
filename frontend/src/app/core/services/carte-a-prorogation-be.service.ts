import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CarteAProrogationBeRequest,
  CarteAProrogationBeResponse,
} from '../models/carte-a-prorogation-be.model';

/**
 * SF-221-01 : wrapper HttpClient pour l'outil décisionnel
 * « Prorogation de la carte A (séjour temporaire BE) » (F-IM-53-carte-a-prorogation-be).
 *
 * BELGIQUE uniquement — calcul du délai de dépôt (30-45 j avant expiration) et des
 * conditions de prorogation (Loi 15/12/1980 art. 13 + AR 08/10/1981 art. 33).
 *
 * Consomme l'API figée dans SF-221-01 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/carte-a-prorogation-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/carte-a-prorogation-be-analysis
 *
 * Pattern miroir de {@link CceAnnulationBeService}.
 */
@Injectable({ providedIn: 'root' })
export class CarteAProrogationBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CarteAProrogationBeRequest):
      Observable<CarteAProrogationBeResponse> {
    return this.http.post<CarteAProrogationBeResponse>(
      `/api/v1/case-files/${caseFileId}/carte-a-prorogation-be-analysis`, request);
  }

  get(caseFileId: string): Observable<CarteAProrogationBeResponse> {
    return this.http.get<CarteAProrogationBeResponse>(
      `/api/v1/case-files/${caseFileId}/carte-a-prorogation-be-analysis`);
  }

  /** `toolId` aligné sur la clé `TOOL_REGISTRY.get('F-IM-53-carte-a-prorogation-be')`. */
  static readonly STANDALONE_TOOL_ID = 'F-IM-53-carte-a-prorogation-be';
}
