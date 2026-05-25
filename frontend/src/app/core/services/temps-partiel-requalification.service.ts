import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TempsPartielRequalificationRequest,
  TempsPartielRequalificationResponse,
} from '../models/temps-partiel-requalification.model';

/**
 * SF-212-34 : wrapper HttpClient pour l'outil décisionnel "Temps partiel
 * — requalification en temps plein" (F-DT-49, FR — L. 3123-1 à L. 3123-20 CT,
 * L. 3245-1 prescription rappel salaire). Consomme l'API figée dans
 * SF-212-33 (backend).
 */
@Injectable({ providedIn: 'root' })
export class TempsPartielRequalificationService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de requalification pour le dossier. */
  calculate(
    caseFileId: string,
    request: TempsPartielRequalificationRequest,
  ): Observable<TempsPartielRequalificationResponse> {
    return this.http.post<TempsPartielRequalificationResponse>(
      `/api/v1/case-files/${caseFileId}/temps-partiel-requalification`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<TempsPartielRequalificationResponse> {
    return this.http.get<TempsPartielRequalificationResponse>(
      `/api/v1/case-files/${caseFileId}/temps-partiel-requalification`,
    );
  }
}
