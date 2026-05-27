import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CumulRccAllocationsRequest,
  CumulRccAllocationsResponse,
} from '../models/cumul-rcc-allocations.model';

/**
 * SF-219-04b : wrapper HttpClient pour l'outil décisionnel
 * « Cumul RCC + allocations / pension (Belgique) » (F-219, BE uniquement —
 * CCT 17 + AR 25/11/1991 ONEM + AR 03/05/2007 art. 22).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/cumul-rcc-allocations}.</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif distinct. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend renvoie
 * 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class CumulRccAllocationsService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/cumul-rcc-allocations`;
  }

  calculate(
    caseFileId: string,
    request: CumulRccAllocationsRequest,
  ): Observable<CumulRccAllocationsResponse> {
    return this.http.post<CumulRccAllocationsResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<CumulRccAllocationsResponse> {
    return this.http.get<CumulRccAllocationsResponse>(
      this.endpoint(caseFileId),
    );
  }
}
