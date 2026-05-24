import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ModificationContratRefusRequest,
  ModificationContratRefusResponse,
} from '../models/modification-contrat-refus.model';

/**
 * SF-212-12 : wrapper HttpClient pour l'outil décisionnel "Modification du
 * contrat — refus du salarié" (F-DT-70, FR — Cass. soc. ; L. 1222-6 CT).
 * Consomme l'API figée dans SF-212-11 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ModificationContratRefusService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: ModificationContratRefusRequest,
  ): Observable<ModificationContratRefusResponse> {
    return this.http.post<ModificationContratRefusResponse>(
      `/api/v1/case-files/${caseFileId}/modification-contrat-refus`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<ModificationContratRefusResponse> {
    return this.http.get<ModificationContratRefusResponse>(
      `/api/v1/case-files/${caseFileId}/modification-contrat-refus`,
    );
  }
}
