import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CongesPayesIndemniteRequest,
  CongesPayesIndemniteResponse,
} from '../models/conges-payes-indemnite.model';

/**
 * SF-DT-26-02 : wrapper HttpClient pour l'outil décisionnel
 * "Indemnité compensatrice de congés payés" (F-DT-26).
 *
 * Consomme l'API figée dans SF-DT-26-01 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/conges-payes-indemnite
 */
@Injectable({ providedIn: 'root' })
export class CongesPayesIndemniteService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-26-conges-payes-indemnite';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: CongesPayesIndemniteRequest):
      Observable<CongesPayesIndemniteResponse> {
    return this.http.post<CongesPayesIndemniteResponse>(
      `/api/v1/case-files/${caseFileId}/conges-payes-indemnite`, request);
  }

  get(caseFileId: string): Observable<CongesPayesIndemniteResponse> {
    return this.http.get<CongesPayesIndemniteResponse>(
      `/api/v1/case-files/${caseFileId}/conges-payes-indemnite`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: CongesPayesIndemniteRequest): Observable<CongesPayesIndemniteResponse> {
    return this.http.post<CongesPayesIndemniteResponse>(
      `/api/v1/simulators/${CongesPayesIndemniteService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
