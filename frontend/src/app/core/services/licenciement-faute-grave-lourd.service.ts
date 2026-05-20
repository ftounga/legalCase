import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementFauteGraveLourdRequest,
  LicenciementFauteGraveLourdResponse,
} from '../models/licenciement-faute-grave-lourd.model';

/**
 * SF-212-02 : wrapper HttpClient pour l'outil décisionnel "Licenciement pour
 * faute grave / faute lourde" (F-DT-36, FR — L.1234-1 s. CT, Cass. soc.
 * 18/06/2013 n°11-14.393). Consomme l'API figée dans SF-212-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class LicenciementFauteGraveLourdService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de la faute disciplinaire pour le dossier. */
  calculate(
    caseFileId: string,
    request: LicenciementFauteGraveLourdRequest,
  ): Observable<LicenciementFauteGraveLourdResponse> {
    return this.http.post<LicenciementFauteGraveLourdResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement-faute-grave-lourde`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<LicenciementFauteGraveLourdResponse> {
    return this.http.get<LicenciementFauteGraveLourdResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement-faute-grave-lourde`,
    );
  }
}
