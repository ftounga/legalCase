import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EtatCivilBeModificationRequest,
  EtatCivilBeModificationResponse,
} from '../models/etat-civil-be-modification.model';

/**
 * SF-223-09 : wrapper HttpClient pour l'outil décisionnel "Modification de
 * l'état civil (Belgique)" (`etat-civil-be-modification`). Consomme l'API figée
 * dans SF-223-09 (backend).
 */
@Injectable({ providedIn: 'root' })
export class EtatCivilBeModificationService {
  constructor(private http: HttpClient) {}

  /** POST — qualifie la modification de l'état civil et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: EtatCivilBeModificationRequest,
  ): Observable<EtatCivilBeModificationResponse> {
    return this.http.post<EtatCivilBeModificationResponse>(
      `/api/v1/case-files/${caseFileId}/etat-civil-be-modification-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<EtatCivilBeModificationResponse> {
    return this.http.get<EtatCivilBeModificationResponse>(
      `/api/v1/case-files/${caseFileId}/etat-civil-be-modification-analysis`,
    );
  }
}
