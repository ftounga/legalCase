import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementBeStatutUniquePreavisRequest,
  LicenciementBeStatutUniquePreavisResponse,
} from '../models/licenciement-be-statut-unique-preavis.model';

/**
 * SF-213-03b : wrapper HttpClient pour l'outil décisionnel
 * "Préavis statut unique BE" (F-213, BE uniquement — Loi 26/12/2013
 * portant introduction d'un statut unique entre ouvriers et employés
 * + art. 37/2 §1er Loi 03/07/1978 — barème par paliers d'ancienneté).
 * Consomme l'API figée dans SF-213-03 backend.
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/licenciement-be-statut-unique-preavis}.</p>
 */
@Injectable({ providedIn: 'root' })
export class LicenciementBeStatutUniquePreavisService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/licenciement-be-statut-unique-preavis`;
  }

  calculate(
    caseFileId: string,
    request: LicenciementBeStatutUniquePreavisRequest,
  ): Observable<LicenciementBeStatutUniquePreavisResponse> {
    return this.http.post<LicenciementBeStatutUniquePreavisResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<LicenciementBeStatutUniquePreavisResponse> {
    return this.http.get<LicenciementBeStatutUniquePreavisResponse>(this.endpoint(caseFileId));
  }
}
