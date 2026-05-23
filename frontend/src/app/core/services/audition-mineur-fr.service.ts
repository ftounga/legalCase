import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AuditionMineurRequest,
  AuditionMineurResponse,
} from '../models/audition-mineur-fr.model';

/**
 * SF-216-14 : wrapper HttpClient pour l'outil décisionnel "Audition du
 * mineur par le JAF" (F-FA-AUDITION-MINEUR, FR — art. 388-1 Cciv
 * + art. 1074-1 à 1074-3 CPC). Consomme l'API figée dans SF-216-13.
 *
 * Endpoints :
 *   POST /api/v1/case-files/{caseFileId}/audition-mineur
 *   GET  /api/v1/case-files/{caseFileId}/audition-mineur
 */
@Injectable({ providedIn: 'root' })
export class AuditionMineurFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Audition du mineur. */
  calculate(
    caseFileId: string,
    request: AuditionMineurRequest,
  ): Observable<AuditionMineurResponse> {
    return this.http.post<AuditionMineurResponse>(
      `/api/v1/case-files/${caseFileId}/audition-mineur`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AuditionMineurResponse> {
    return this.http.get<AuditionMineurResponse>(
      `/api/v1/case-files/${caseFileId}/audition-mineur`,
    );
  }
}
