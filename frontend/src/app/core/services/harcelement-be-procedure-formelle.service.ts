import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HarcelementBeProcedureFormelleRequest,
  HarcelementBeProcedureFormelleResponse,
} from '../models/harcelement-be-procedure-formelle.model';

/**
 * SF-213-07b : wrapper HttpClient pour l'outil décisionnel
 * « Harcèlement BE — procédure formelle » (F-213, BE uniquement —
 * Loi 04/08/1996 art. 32bis–32sexies + AR 10/04/2014).
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/harcelement-be-procedure-formelle}.</p>
 *
 * <p>Distinct de {@code F-DT-11} (nullité licenciement représailles) —
 * cet outil intervient en amont (procédure interne) ; F-DT-11 intervient
 * en aval (sanction nullité d'une éventuelle rupture).</p>
 */
@Injectable({ providedIn: 'root' })
export class HarcelementBeProcedureFormelleService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/harcelement-be-procedure-formelle`;
  }

  calculate(
    caseFileId: string,
    request: HarcelementBeProcedureFormelleRequest,
  ): Observable<HarcelementBeProcedureFormelleResponse> {
    return this.http.post<HarcelementBeProcedureFormelleResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<HarcelementBeProcedureFormelleResponse> {
    return this.http.get<HarcelementBeProcedureFormelleResponse>(
      this.endpoint(caseFileId),
    );
  }
}
