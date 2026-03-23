import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CaseAnalysisCommandService {
  constructor(private http: HttpClient) {}

  triggerAnalysis(caseFileId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/case-files/${caseFileId}/analyze`, {});
  }
}
