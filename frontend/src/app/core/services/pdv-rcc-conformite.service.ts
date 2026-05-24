import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PdvRccConformiteRequest,
  PdvRccConformiteResponse,
} from '../models/pdv-rcc-conformite.model';

/**
 * SF-212-36 : wrapper HttpClient pour l'outil décisionnel "PDV / RCC —
 * conformité" (F-DT-46-pdv-rcc-conformite, FR — L. 1237-17 à L. 1237-19-14
 * CT ; ord. 22/09/2017 ; décret 2017-1718 du 20/12/2017).
 * Consomme l'API figée dans SF-212-35 (backend).
 */
@Injectable({ providedIn: 'root' })
export class PdvRccConformiteService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: PdvRccConformiteRequest,
  ): Observable<PdvRccConformiteResponse> {
    return this.http.post<PdvRccConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/pdv-rcc-conformite`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<PdvRccConformiteResponse> {
    return this.http.get<PdvRccConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/pdv-rcc-conformite`,
    );
  }
}
