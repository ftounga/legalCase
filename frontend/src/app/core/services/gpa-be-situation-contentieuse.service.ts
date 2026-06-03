import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  GpaBeRequest,
  GpaBeResponse,
} from '../models/gpa-be-situation-contentieuse.model';

/**
 * SF-223-04 : wrapper HttpClient pour l'outil décisionnel "Situation
 * contentieuse post-GPA — Belgique" (`gpa-be-situation-contentieuse`). Consomme
 * l'API figée dans SF-223-04 (backend).
 */
@Injectable({ providedIn: 'root' })
export class GpaBeSituationContentieuseService {
  constructor(private http: HttpClient) {}

  /** POST — cadre l'établissement de la filiation post-GPA et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: GpaBeRequest,
  ): Observable<GpaBeResponse> {
    return this.http.post<GpaBeResponse>(
      `/api/v1/case-files/${caseFileId}/gpa-be-situation-contentieuse-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<GpaBeResponse> {
    return this.http.get<GpaBeResponse>(
      `/api/v1/case-files/${caseFileId}/gpa-be-situation-contentieuse-analysis`,
    );
  }
}
