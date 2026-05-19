import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PrescriptionBeLitigeTravailRequest,
  PrescriptionBeLitigeTravailResponse,
} from '../models/prescription-be-litige-travail.model';

/**
 * SF-207-01b : wrapper HttpClient pour l'outil décisionnel "Prescription
 * d'une action de droit du travail belge" (F-207, BE uniquement).
 * Consomme l'API figée dans SF-207-01 backend (PR #1119).
 */
@Injectable({ providedIn: 'root' })
export class PrescriptionBeLitigeTravailService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/prescription-be-litige-travail`;
  }

  calculate(caseFileId: string, request: PrescriptionBeLitigeTravailRequest):
      Observable<PrescriptionBeLitigeTravailResponse> {
    return this.http.post<PrescriptionBeLitigeTravailResponse>(
      this.endpoint(caseFileId), request);
  }

  get(caseFileId: string): Observable<PrescriptionBeLitigeTravailResponse> {
    return this.http.get<PrescriptionBeLitigeTravailResponse>(this.endpoint(caseFileId));
  }
}
