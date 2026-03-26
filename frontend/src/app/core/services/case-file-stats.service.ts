import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseFileStats } from '../models/case-file-stats.model';

@Injectable({ providedIn: 'root' })
export class CaseFileStatsService {
  constructor(private http: HttpClient) {}

  getStats(caseFileId: string): Observable<CaseFileStats> {
    return this.http.get<CaseFileStats>(`/api/v1/case-files/${caseFileId}/stats`);
  }
}
