import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WorkRightRequest, WorkRightResponse } from '../models/immigration-work-right.model';

@Injectable({ providedIn: 'root' })
export class ImmigrationWorkRightService {

  constructor(private http: HttpClient) {}

  resolve(caseFileId: string, request: WorkRightRequest): Observable<WorkRightResponse> {
    return this.http.post<WorkRightResponse>(
      `/api/v1/case-files/${caseFileId}/immigration/work-right`,
      request
    );
  }

  get(caseFileId: string): Observable<WorkRightResponse> {
    return this.http.get<WorkRightResponse>(
      `/api/v1/case-files/${caseFileId}/immigration/work-right`
    );
  }
}
