import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TransfertEntrepriseCct32bisRequest,
  TransfertEntrepriseCct32bisResponse,
} from '../models/transfert-entreprise-cct-32bis.model';

/**
 * SF-219-08b : wrapper HttpClient pour l'outil décisionnel
 * « Transfert d'entreprise — CCT n° 32bis du 07/06/1985 » (F-219, BE
 * uniquement — Loi 17/03/1965 sur les fermetures/transferts ; Directive
 * 2001/23/CE).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/transfert-entreprise-cct-32bis}.</p>
 *
 * <p>Outil <b>BE-only</b> — l'art. L. 1224-1 FR a un régime parent
 * (maintien automatique des contrats en cas de transfert d'entité
 * économique gardant son identité), mais sans les spécificités
 * procédurales CCT 32bis / Loi 17/03/1965 (info-consultation préalable
 * sanctionnée pénalement, responsabilité solidaire 1 an cédant /
 * cessionnaire). Le gate {@code workspaceCountry} est porté côté
 * composant ; le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class TransfertEntrepriseCct32bisService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/transfert-entreprise-cct-32bis`;
  }

  calculate(
    caseFileId: string,
    request: TransfertEntrepriseCct32bisRequest,
  ): Observable<TransfertEntrepriseCct32bisResponse> {
    return this.http.post<TransfertEntrepriseCct32bisResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<TransfertEntrepriseCct32bisResponse> {
    return this.http.get<TransfertEntrepriseCct32bisResponse>(
      this.endpoint(caseFileId),
    );
  }
}
