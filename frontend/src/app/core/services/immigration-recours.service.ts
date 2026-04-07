import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RecoursRequest, RecoursResponse } from '../models/immigration-recours.model';

@Injectable({ providedIn: 'root' })
export class ImmigrationRecoursService {

  constructor(private http: HttpClient) {}

  generate(caseFileId: string, request: RecoursRequest): Observable<RecoursResponse> {
    return this.http.post<RecoursResponse>(
      `/api/v1/case-files/${caseFileId}/immigration/recours`,
      request
    );
  }

  get(caseFileId: string): Observable<RecoursResponse> {
    return this.http.get<RecoursResponse>(
      `/api/v1/case-files/${caseFileId}/immigration/recours`
    );
  }
}
