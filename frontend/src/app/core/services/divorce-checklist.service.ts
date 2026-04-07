import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DivorceChecklistRequest, DivorceChecklistResponse } from '../models/divorce-checklist.model';

@Injectable({ providedIn: 'root' })
export class DivorceChecklistService {
  constructor(private http: HttpClient) {}
  save(caseFileId: string, req: DivorceChecklistRequest): Observable<DivorceChecklistResponse> {
    return this.http.post<DivorceChecklistResponse>(`/api/v1/case-files/${caseFileId}/divorce-checklist`, req);
  }
  get(caseFileId: string): Observable<DivorceChecklistResponse> {
    return this.http.get<DivorceChecklistResponse>(`/api/v1/case-files/${caseFileId}/divorce-checklist`);
  }
}
