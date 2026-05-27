import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TransactionBeTravailRequest,
  TransactionBeTravailResponse,
} from '../models/transaction-be-travail.model';

/**
 * SF-213-06b : wrapper HttpClient pour l'outil décisionnel "Transaction de
 * fin de contrat BE" (F-213, BE uniquement — art. 2044 Cciv BE + Loi
 * 03/07/1978 art. 6). Consomme l'API figée dans SF-213-06 backend.
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/transaction-be-travail}.</p>
 *
 * <p><b>Distinct de TransactionService (SF-DT-31-02)</b> qui couvre la
 * transaction FR (art. 2044 Cciv FR — régime juridique différent).
 * Les deux outils coexistent et sont gated par {@code workspaceCountry}.</p>
 */
@Injectable({ providedIn: 'root' })
export class TransactionBeTravailService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/transaction-be-travail`;
  }

  calculate(
    caseFileId: string,
    request: TransactionBeTravailRequest,
  ): Observable<TransactionBeTravailResponse> {
    return this.http.post<TransactionBeTravailResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<TransactionBeTravailResponse> {
    return this.http.get<TransactionBeTravailResponse>(this.endpoint(caseFileId));
  }
}
