import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PmaGpaBioethiqueRequest,
  PmaGpaBioethiqueResponse,
} from '../models/pma-gpa-bioethique.model';

/**
 * SF-FA-27-02 : wrapper HttpClient pour l'outil décisionnel
 * "PMA / GPA / bioéthique" (F-FA-27). FR uniquement.
 *
 * Consomme l'API figée dans SF-FA-27-01 (backend, mergé PR #656).
 * URL : `/api/v1/case-files/{caseFileId}/pma-gpa-bioethique`.
 */
@Injectable({ providedIn: 'root' })
export class PmaGpaBioethiqueService {

  constructor(private http: HttpClient) {}

  calculate(
    caseFileId: string,
    request: PmaGpaBioethiqueRequest,
  ): Observable<PmaGpaBioethiqueResponse> {
    return this.http.post<PmaGpaBioethiqueResponse>(
      `/api/v1/case-files/${caseFileId}/pma-gpa-bioethique`,
      request,
    );
  }

  get(caseFileId: string): Observable<PmaGpaBioethiqueResponse> {
    return this.http.get<PmaGpaBioethiqueResponse>(
      `/api/v1/case-files/${caseFileId}/pma-gpa-bioethique`,
    );
  }
}
