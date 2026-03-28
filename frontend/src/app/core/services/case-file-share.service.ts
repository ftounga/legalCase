import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PublicShareResponse, ShareResponse } from '../models/share.model';

@Injectable({ providedIn: 'root' })
export class CaseFileShareService {
  constructor(private http: HttpClient) {}

  createShare(caseFileId: string, expiresInDays: number): Observable<ShareResponse> {
    return this.http.post<ShareResponse>(
      `/api/v1/case-files/${caseFileId}/shares`,
      { expiresInDays }
    );
  }

  listShares(caseFileId: string): Observable<ShareResponse[]> {
    return this.http.get<ShareResponse[]>(`/api/v1/case-files/${caseFileId}/shares`);
  }

  revokeShare(caseFileId: string, shareId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/case-files/${caseFileId}/shares/${shareId}`);
  }

  getPublicShare(token: string): Observable<PublicShareResponse> {
    return this.http.get<PublicShareResponse>(`/api/v1/public/shares/${token}`);
  }
}
