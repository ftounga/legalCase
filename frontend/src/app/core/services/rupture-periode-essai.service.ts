import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RupturePeriodeEssaiRequest,
  RupturePeriodeEssaiResponse,
} from '../models/rupture-periode-essai.model';

/**
 * SF-DT-38-02 : wrapper HttpClient pour l'outil décisionnel "Rupture de
 * période d'essai" (F-DT-38, qualification régulière / abusive / nulle /
 * illégale-requalif-licenciement). Consomme l'API figée dans SF-DT-38-01.
 */
@Injectable({ providedIn: 'root' })
export class RupturePeriodeEssaiService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste la qualification du dossier. */
  calculate(
    caseFileId: string,
    request: RupturePeriodeEssaiRequest,
  ): Observable<RupturePeriodeEssaiResponse> {
    return this.http.post<RupturePeriodeEssaiResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-periode-essai`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<RupturePeriodeEssaiResponse> {
    return this.http.get<RupturePeriodeEssaiResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-periode-essai`,
    );
  }
}
