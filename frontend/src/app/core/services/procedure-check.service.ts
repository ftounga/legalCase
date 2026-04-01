import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProcedureCheck, ProcedureCheckStatus } from '../models/procedure-check.model';

@Injectable({ providedIn: 'root' })
export class ProcedureCheckService {
  constructor(private http: HttpClient) {}

  list(caseFileId: string, analysisId: string): Observable<ProcedureCheck[]> {
    return this.http.get<ProcedureCheck[]>(
      `/api/v1/case-files/${caseFileId}/analyses/${analysisId}/procedure-checks`
    );
  }

  updateStatus(checkId: string, statut: ProcedureCheckStatus): Observable<ProcedureCheck> {
    return this.http.patch<ProcedureCheck>(`/api/v1/procedure-checks/${checkId}`, { statut });
  }
}
