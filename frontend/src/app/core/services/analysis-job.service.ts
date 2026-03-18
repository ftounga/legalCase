import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AnalysisJob } from '../models/analysis-job.model';

@Injectable({ providedIn: 'root' })
export class AnalysisJobService {
  constructor(private http: HttpClient) {}

  getJobs(caseFileId: string): Observable<AnalysisJob[]> {
    return this.http.get<AnalysisJob[]>(`/api/v1/case-files/${caseFileId}/analysis-jobs`);
  }
}
