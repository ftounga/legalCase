import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PriseActeRuptureRequest,
  PriseActeRuptureResponse,
} from '../models/prise-acte-rupture.model';

/**
 * SF-206-06 : wrapper HttpClient pour l'outil décisionnel "Prise d'acte de
 * la rupture aux torts de l'employeur" (F-DT-39, FR — Cass. soc. 25/06/2003
 * n°01-42.679). Consomme l'API figée dans SF-206-05 (backend).
 */
@Injectable({ providedIn: 'root' })
export class PriseActeRuptureService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de prise d'acte pour le dossier. */
  calculate(
    caseFileId: string,
    request: PriseActeRuptureRequest,
  ): Observable<PriseActeRuptureResponse> {
    return this.http.post<PriseActeRuptureResponse>(
      `/api/v1/case-files/${caseFileId}/prise-acte-rupture`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<PriseActeRuptureResponse> {
    return this.http.get<PriseActeRuptureResponse>(
      `/api/v1/case-files/${caseFileId}/prise-acte-rupture`,
    );
  }
}
