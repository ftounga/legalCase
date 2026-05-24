import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DemissionEquivoqueRequest,
  DemissionEquivoqueResponse,
} from '../models/demission-equivoque.model';

/**
 * SF-212-22 : wrapper HttpClient pour l'outil décisionnel « Démission —
 * validité et caractère équivoque » (F-DT-41-demission-validite-equivoque,
 * FR — L. 1237-1 CT ; Cass. soc. 09/05/2007). Consomme l'API figée dans
 * SF-212-21 (backend).
 */
@Injectable({ providedIn: 'root' })
export class DemissionEquivoqueService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: DemissionEquivoqueRequest,
  ): Observable<DemissionEquivoqueResponse> {
    return this.http.post<DemissionEquivoqueResponse>(
      `/api/v1/case-files/${caseFileId}/demission-validite-equivoque`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<DemissionEquivoqueResponse> {
    return this.http.get<DemissionEquivoqueResponse>(
      `/api/v1/case-files/${caseFileId}/demission-validite-equivoque`,
    );
  }
}
