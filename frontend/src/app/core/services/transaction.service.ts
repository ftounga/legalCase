import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TransactionRequest,
  TransactionResponse,
} from '../models/transaction.model';

/**
 * SF-DT-31-02 : wrapper HttpClient pour l'outil décisionnel
 * "Transaction / protocole transactionnel" (F-DT-31, art. 2044 Cciv).
 * Consomme l'API figée dans SF-DT-31-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class TransactionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: TransactionRequest):
      Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(
      `/api/v1/case-files/${caseFileId}/transaction`, request);
  }

  get(caseFileId: string): Observable<TransactionResponse> {
    return this.http.get<TransactionResponse>(
      `/api/v1/case-files/${caseFileId}/transaction`);
  }
}
