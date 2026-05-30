import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  VrpIndemniteClienteleRequest,
  VrpIndemniteClienteleResponse,
} from '../models/vrp-indemnite-clientele.model';

/**
 * SF-218-12 : wrapper HttpClient pour l'outil décisionnel "VRP : statut,
 * préavis et indemnité de clientèle" (F-DT-104, FR — éligibilité et estimation
 * de l'indemnité de clientèle, art. L.7313-13 CT). Consomme l'API figée dans
 * SF-218-11 (backend).
 */
@Injectable({ providedIn: 'root' })
export class VrpIndemniteClienteleService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse VRP pour le dossier. */
  calculate(
    caseFileId: string,
    request: VrpIndemniteClienteleRequest,
  ): Observable<VrpIndemniteClienteleResponse> {
    return this.http.post<VrpIndemniteClienteleResponse>(
      `/api/v1/case-files/${caseFileId}/vrp-indemnite-clientele-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<VrpIndemniteClienteleResponse> {
    return this.http.get<VrpIndemniteClienteleResponse>(
      `/api/v1/case-files/${caseFileId}/vrp-indemnite-clientele-analysis`,
    );
  }
}
