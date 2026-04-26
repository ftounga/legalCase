import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CommunauteUniverselleRequest,
  CommunauteUniverselleResponse,
} from '../models/communaute-universelle.model';

/**
 * SF-FA-16-02 : wrapper HttpClient pour l'outil décisionnel "Communauté
 * universelle" (F-FA-16). FR uniquement.
 * Consomme l'API figée dans SF-FA-16-01 (backend, mergé PR #648).
 */
@Injectable({ providedIn: 'root' })
export class CommunauteUniverselleService {

  constructor(private http: HttpClient) {}

  calculate(
    caseFileId: string,
    request: CommunauteUniverselleRequest,
  ): Observable<CommunauteUniverselleResponse> {
    return this.http.post<CommunauteUniverselleResponse>(
      `/api/v1/case-files/${caseFileId}/communaute-universelle-analysis`,
      request,
    );
  }

  get(caseFileId: string): Observable<CommunauteUniverselleResponse> {
    return this.http.get<CommunauteUniverselleResponse>(
      `/api/v1/case-files/${caseFileId}/communaute-universelle-analysis`,
    );
  }
}
