import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReAnalysisService {
  constructor(private http: HttpClient) {}

  reAnalyze(caseFileId: string): Observable<void> {
    return this.http.post<void>(`/api/v1/case-files/${caseFileId}/re-analyze`, {});
  }
}
