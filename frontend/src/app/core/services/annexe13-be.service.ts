import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Annexe13BeRequest,
  Annexe13BeResponse,
} from '../models/annexe13-be.model';

/**
 * SF-IM-08-06 : wrapper HttpClient pour l'outil décisionnel
 * "Annexe 13 — OQT belge" (F-IM-08). BE uniquement.
 * Consomme l'API figée dans SF-IM-08-05 (backend).
 */
@Injectable({ providedIn: 'root' })
export class Annexe13BeService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Annexe13BeRequest):
      Observable<Annexe13BeResponse> {
    return this.http.post<Annexe13BeResponse>(
      `/api/v1/case-files/${caseFileId}/annexe13-be`, request);
  }

  get(caseFileId: string): Observable<Annexe13BeResponse> {
    return this.http.get<Annexe13BeResponse>(
      `/api/v1/case-files/${caseFileId}/annexe13-be`);
  }
}
