import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SuccessionBeDevolutionReserveRequest,
  SuccessionBeDevolutionReserveResponse,
} from '../models/succession-be-devolution-reserve.model';

/**
 * F-217 SF-217-13 : wrapper HttpClient pour l'outil décisionnel
 * "Succession BE — dévolution légale et réserve héréditaire post-2017"
 * (`succession-be-devolution-reserve`).
 * Consomme l'API figée dans SF-217-11 (backend).
 */
@Injectable({ providedIn: 'root' })
export class SuccessionBeDevolutionReserveService {
  constructor(private http: HttpClient) {}

  /** POST — qualifie et persiste la dévolution / réserve de la succession. */
  calculate(
    caseFileId: string,
    request: SuccessionBeDevolutionReserveRequest,
  ): Observable<SuccessionBeDevolutionReserveResponse> {
    return this.http.post<SuccessionBeDevolutionReserveResponse>(
      `/api/v1/case-files/${caseFileId}/succession-be-devolution-reserve`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<SuccessionBeDevolutionReserveResponse> {
    return this.http.get<SuccessionBeDevolutionReserveResponse>(
      `/api/v1/case-files/${caseFileId}/succession-be-devolution-reserve`,
    );
  }
}
