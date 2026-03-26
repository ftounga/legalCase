import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalysisDiff, CaseAnalysisResult, CaseAnalysisVersionSummary } from '../models/case-analysis.model';

@Injectable({ providedIn: 'root' })
export class CaseAnalysisService {
  constructor(private http: HttpClient) {}

  getAnalysis(caseFileId: string): Observable<CaseAnalysisResult> {
    return this.http.get<CaseAnalysisResult>(`/api/v1/case-files/${caseFileId}/case-analysis`);
  }

  getVersions(caseFileId: string): Observable<CaseAnalysisVersionSummary[]> {
    return this.http.get<CaseAnalysisVersionSummary[]>(`/api/v1/case-files/${caseFileId}/case-analysis/versions`);
  }

  getByVersion(caseFileId: string, version: number): Observable<CaseAnalysisResult> {
    return this.http.get<CaseAnalysisResult>(`/api/v1/case-files/${caseFileId}/case-analysis/versions/${version}`);
  }

  getDiff(caseFileId: string, fromId: string, toId: string): Observable<AnalysisDiff> {
    return this.http.get<AnalysisDiff>(`/api/v1/case-files/${caseFileId}/case-analysis/diff`, {
      params: { fromId, toId }
    });
  }
}
