import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AesmMenaBeRequest,
  AesmMenaBeResponse,
} from '../models/aesm-mena-be.model';

/**
 * SF-215-12 : wrapper HttpClient pour l'outil décisionnel composite
 * « AESM + tutelle DGDE (MENA) » (F-IM-30-aesm-mena-be).
 *
 * BELGIQUE uniquement — outil dédié aux Mineurs Étrangers Non
 * Accompagnés (MENA). Composite 2 volets :
 *  - Tutelle DGDE (Loi 04/05/2007, saisine 24h ouvrables Service Tutelles)
 *  - AESM scoring (art. 9bis Loi 15/12/1980 adapté + Circulaire OE 15/09/2005)
 *
 * Consomme l'API figée dans SF-215-11 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/aesm-mena-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/aesm-mena-be-analysis
 *
 * Pattern miroir de {@link NaturalisationConjointBelgeBeService} (SF-215-10).
 */
@Injectable({ providedIn: 'root' })
export class AesmMenaBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AesmMenaBeRequest): Observable<AesmMenaBeResponse> {
    return this.http.post<AesmMenaBeResponse>(
      `/api/v1/case-files/${caseFileId}/aesm-mena-be-analysis`, request);
  }

  get(caseFileId: string): Observable<AesmMenaBeResponse> {
    return this.http.get<AesmMenaBeResponse>(
      `/api/v1/case-files/${caseFileId}/aesm-mena-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-30-aesm-mena-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-30-aesm-mena-be';
}
