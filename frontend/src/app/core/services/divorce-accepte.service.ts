import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceAccepteRequest,
  DivorceAccepteResponse,
} from '../models/divorce-accepte.model';

/**
 * SF-FA-10-02 : service HTTP pour l'outil F-FA-10 (divorce accepté FR).
 * Wrappe les endpoints `POST` / `GET /api/v1/case-files/{id}/divorce-accepte`
 * livrés par SF-FA-10-01 (PR #514).
 */
@Injectable({ providedIn: 'root' })
export class DivorceAccepteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceAccepteRequest): Observable<DivorceAccepteResponse> {
    return this.http.post<DivorceAccepteResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-accepte`,
      request,
    );
  }

  get(caseFileId: string): Observable<DivorceAccepteResponse> {
    return this.http.get<DivorceAccepteResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-accepte`,
    );
  }
}
