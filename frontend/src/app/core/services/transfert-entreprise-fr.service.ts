import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TransfertEntrepriseFrRequest,
  TransfertEntrepriseFrResponse,
} from '../models/transfert-entreprise-fr.model';

/**
 * SF-212-06 : wrapper HttpClient pour l'outil décisionnel "Transfert
 * d'entreprise — L. 1224-1" (F-DT-72, FR — L. 1224-1 CT, Cass. soc.
 * 18/07/2000 n°98-46.071, Directive 2001/23/CE). Consomme l'API figée
 * dans SF-212-05 (backend).
 */
@Injectable({ providedIn: 'root' })
export class TransfertEntrepriseFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de transfert d'entreprise pour le dossier. */
  calculate(
    caseFileId: string,
    request: TransfertEntrepriseFrRequest,
  ): Observable<TransfertEntrepriseFrResponse> {
    return this.http.post<TransfertEntrepriseFrResponse>(
      `/api/v1/case-files/${caseFileId}/transfert-entreprise-l1224-1`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<TransfertEntrepriseFrResponse> {
    return this.http.get<TransfertEntrepriseFrResponse>(
      `/api/v1/case-files/${caseFileId}/transfert-entreprise-l1224-1`,
    );
  }
}
