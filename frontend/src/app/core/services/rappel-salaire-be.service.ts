import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RappelSalaireBeRequest,
  RappelSalaireBeResponse,
} from '../models/rappel-salaire-be.model';

/**
 * SF-213-02b : wrapper HttpClient pour l'outil décisionnel
 * "Rappel de salaire BE" (F-213, BE uniquement). Consomme l'API figée
 * dans SF-213-02 backend.
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/rappel-salaire-be}.</p>
 */
@Injectable({ providedIn: 'root' })
export class RappelSalaireBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/rappel-salaire-be`;
  }

  calculate(
    caseFileId: string,
    request: RappelSalaireBeRequest,
  ): Observable<RappelSalaireBeResponse> {
    return this.http.post<RappelSalaireBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<RappelSalaireBeResponse> {
    return this.http.get<RappelSalaireBeResponse>(this.endpoint(caseFileId));
  }
}
