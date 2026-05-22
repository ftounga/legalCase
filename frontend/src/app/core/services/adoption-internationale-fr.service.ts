import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdoptionInternationaleRequest,
  AdoptionInternationaleResponse,
} from '../models/adoption-internationale-fr.model';

/**
 * SF-216-18 : wrapper HttpClient pour l'outil décisionnel "Adoption
 * internationale" (F-FA-ADOPTION-INTERNATIONALE, FR — art. 370-3 à 370-5
 * Cciv + Convention La Haye 1993). Consomme l'API figée dans SF-216-17.
 *
 * Endpoints :
 *   POST /api/v1/case-files/{caseFileId}/adoption-internationale
 *   GET  /api/v1/case-files/{caseFileId}/adoption-internationale
 */
@Injectable({ providedIn: 'root' })
export class AdoptionInternationaleFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Adoption internationale. */
  calculate(
    caseFileId: string,
    request: AdoptionInternationaleRequest,
  ): Observable<AdoptionInternationaleResponse> {
    return this.http.post<AdoptionInternationaleResponse>(
      `/api/v1/case-files/${caseFileId}/adoption-internationale`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AdoptionInternationaleResponse> {
    return this.http.get<AdoptionInternationaleResponse>(
      `/api/v1/case-files/${caseFileId}/adoption-internationale`,
    );
  }
}
