import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MajeursProtegesRequest,
  MajeursProtegesResponse,
} from '../models/majeurs-proteges.model';

/**
 * SF-FA-25-02 : wrapper HttpClient pour l'outil décisionnel
 * "Majeurs protégés — choix du régime" (F-FA-25, FRANCE uniquement,
 * art. 425-494 Cciv + art. 494-1 Cciv habilitation familiale).
 *
 * Consomme l'API figée par SF-FA-25-01 (backend, parallèle) :
 *   - POST /api/v1/case-files/{caseFileId}/majeurs-proteges → calcul + persistance
 *   - GET  /api/v1/case-files/{caseFileId}/majeurs-proteges → récupération
 */
@Injectable({ providedIn: 'root' })
export class MajeursProtegesService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: MajeursProtegesRequest):
      Observable<MajeursProtegesResponse> {
    return this.http.post<MajeursProtegesResponse>(
      `/api/v1/case-files/${caseFileId}/majeurs-proteges`, request);
  }

  get(caseFileId: string): Observable<MajeursProtegesResponse> {
    return this.http.get<MajeursProtegesResponse>(
      `/api/v1/case-files/${caseFileId}/majeurs-proteges`);
  }
}
