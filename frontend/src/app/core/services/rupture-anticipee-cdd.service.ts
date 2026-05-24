import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RuptureAnticipeeCddRequest,
  RuptureAnticipeeCddResponse,
} from '../models/rupture-anticipee-cdd.model';

/**
 * SF-212-18 : wrapper HttpClient pour l'outil décisionnel "Rupture anticipée
 * du CDD" (F-DT-43-rupture-anticipee-cdd, FR — L. 1243-1 à L. 1243-4 CT ;
 * L. 1243-8 CT ; Cass. soc. constante). Consomme l'API figée dans SF-212-17.
 */
@Injectable({ providedIn: 'root' })
export class RuptureAnticipeeCddService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: RuptureAnticipeeCddRequest,
  ): Observable<RuptureAnticipeeCddResponse> {
    return this.http.post<RuptureAnticipeeCddResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-anticipee-cdd`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<RuptureAnticipeeCddResponse> {
    return this.http.get<RuptureAnticipeeCddResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-anticipee-cdd`,
    );
  }
}
