import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CongesPayesArretMaladieRequest,
  CongesPayesArretMaladieResponse,
} from '../models/conges-payes-arret-maladie.model';

/**
 * SF-206-04 : wrapper HttpClient pour l'outil décisionnel
 * "Congés payés acquis pendant arrêt maladie" (F-DT-75, FR — chiffrage du
 * rappel de congés payés acquis pendant arrêts maladie / AT-MP,
 * loi 22/04/2024 art. 37, L.3141-5 / L.3141-5-1 CT).
 * Consomme l'API figée dans SF-206-03 (backend).
 */
@Injectable({ providedIn: 'root' })
export class CongesPayesArretMaladieService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste le chiffrage de rappel pour le dossier. */
  calculate(
    caseFileId: string,
    request: CongesPayesArretMaladieRequest,
  ): Observable<CongesPayesArretMaladieResponse> {
    return this.http.post<CongesPayesArretMaladieResponse>(
      `/api/v1/case-files/${caseFileId}/conges-payes-arret-maladie`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<CongesPayesArretMaladieResponse> {
    return this.http.get<CongesPayesArretMaladieResponse>(
      `/api/v1/case-files/${caseFileId}/conges-payes-arret-maladie`,
    );
  }
}
