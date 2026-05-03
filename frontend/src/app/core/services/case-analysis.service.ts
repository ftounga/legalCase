import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalysisDiff, CaseAnalysisPartialResponse, CaseAnalysisResult, CaseAnalysisVersionSummary } from '../models/case-analysis.model';

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

  /**
   * F-185 SF-185-01 — récupère l'état partiel courant pendant le streaming Sonnet.
   * 404 si aucune analyse n'est en cours sur ce dossier (à intercepter côté appelant
   * pour basculer sur l'écran "lance une analyse").
   */
  getPartial(caseFileId: string): Observable<CaseAnalysisPartialResponse> {
    return this.http.get<CaseAnalysisPartialResponse>(`/api/v1/case-files/${caseFileId}/case-analysis/partial`);
  }
}
