import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StrategicOption, StrategicOptionStatus } from '../models/strategic-option.model';

@Injectable({ providedIn: 'root' })
export class StrategicOptionService {
  constructor(private http: HttpClient) {}

  list(caseFileId: string, analysisId: string): Observable<StrategicOption[]> {
    return this.http.get<StrategicOption[]>(
      `/api/v1/case-files/${caseFileId}/analyses/${analysisId}/strategic-options`
    );
  }

  updateStatus(
    optionId: string,
    statut: StrategicOptionStatus,
    raisonDiscard?: string | null
  ): Observable<StrategicOption> {
    const body: { statut: StrategicOptionStatus; raisonDiscard?: string | null } = { statut };
    if (raisonDiscard !== undefined) {
      body.raisonDiscard = raisonDiscard;
    }
    return this.http.patch<StrategicOption>(`/api/v1/strategic-options/${optionId}`, body);
  }
}
