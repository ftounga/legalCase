import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseAnalysisResult } from '../models/case-analysis.model';

@Injectable({ providedIn: 'root' })
export class CaseAnalysisService {
  constructor(private http: HttpClient) {}

  getAnalysis(caseFileId: string): Observable<CaseAnalysisResult> {
    return this.http.get<CaseAnalysisResult>(`/api/v1/case-files/${caseFileId}/case-analysis`);
  }
}
