import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface OcrRetryPreview {
  failedDocsCount: number;
  estimatedPages: number;
  monthlyRemaining: number;
  packsRemaining: number;
  canRetry: boolean;
}

@Injectable({ providedIn: 'root' })
export class OcrRetryService {
  constructor(private http: HttpClient) {}

  preview(caseFileId: string): Observable<OcrRetryPreview> {
    return this.http.get<OcrRetryPreview>(`/api/v1/case-files/${caseFileId}/ocr-retry-preview`);
  }

  retry(caseFileId: string): Observable<{ retryedCount: number }> {
    return this.http.post<{ retryedCount: number }>(`/api/v1/case-files/${caseFileId}/ocr-retry`, {});
  }
}
