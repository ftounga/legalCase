import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MariageEtrangerBeReconnaissanceRequest,
  MariageEtrangerBeReconnaissanceResponse,
} from '../models/mariage-etranger-be-reconnaissance.model';

/**
 * F-217 SF-217-17 : wrapper HttpClient pour l'outil décisionnel
 * "Reconnaissance d'un mariage / divorce étranger en Belgique"
 * (`mariage-etranger-be-reconnaissance`).
 * Consomme l'API figée dans SF-217-16 (backend).
 */
@Injectable({ providedIn: 'root' })
export class MariageEtrangerBeReconnaissanceService {
  constructor(private http: HttpClient) {}

  /** POST — analyse la reconnaissance et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: MariageEtrangerBeReconnaissanceRequest,
  ): Observable<MariageEtrangerBeReconnaissanceResponse> {
    return this.http.post<MariageEtrangerBeReconnaissanceResponse>(
      `/api/v1/case-files/${caseFileId}/mariage-etranger-be-reconnaissance`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(
    caseFileId: string,
  ): Observable<MariageEtrangerBeReconnaissanceResponse> {
    return this.http.get<MariageEtrangerBeReconnaissanceResponse>(
      `/api/v1/case-files/${caseFileId}/mariage-etranger-be-reconnaissance`,
    );
  }
}
