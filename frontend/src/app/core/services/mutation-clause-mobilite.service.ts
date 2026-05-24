import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MutationClauseMobiliteRequest,
  MutationClauseMobiliteResponse,
} from '../models/mutation-clause-mobilite.model';

/**
 * SF-212-14 : wrapper HttpClient pour l'outil décisionnel "Mutation —
 * validité de la clause de mobilité" (F-DT-71-mutation-clause-mobilite,
 * FR — Cass. soc. constante). Consomme l'API figée dans SF-212-13 (backend).
 */
@Injectable({ providedIn: 'root' })
export class MutationClauseMobiliteService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: MutationClauseMobiliteRequest,
  ): Observable<MutationClauseMobiliteResponse> {
    return this.http.post<MutationClauseMobiliteResponse>(
      `/api/v1/case-files/${caseFileId}/mutation-clause-mobilite`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<MutationClauseMobiliteResponse> {
    return this.http.get<MutationClauseMobiliteResponse>(
      `/api/v1/case-files/${caseFileId}/mutation-clause-mobilite`,
    );
  }
}
