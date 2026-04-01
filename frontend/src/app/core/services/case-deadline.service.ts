import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseDeadline } from '../models/case-deadline.model';

@Injectable({ providedIn: 'root' })
export class CaseDeadlineService {
  constructor(private http: HttpClient) {}

  private url(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/deadlines`;
  }

  list(caseFileId: string): Observable<CaseDeadline[]> {
    return this.http.get<CaseDeadline[]>(this.url(caseFileId));
  }

  create(caseFileId: string, label: string, dueDate: string): Observable<CaseDeadline> {
    return this.http.post<CaseDeadline>(this.url(caseFileId), { label, dueDate });
  }

  update(caseFileId: string, deadlineId: string, label: string, dueDate: string): Observable<CaseDeadline> {
    return this.http.put<CaseDeadline>(`${this.url(caseFileId)}/${deadlineId}`, { label, dueDate });
  }

  delete(caseFileId: string, deadlineId: string): Observable<void> {
    return this.http.delete<void>(`${this.url(caseFileId)}/${deadlineId}`);
  }

  validateDeadline(caseFileId: string, deadlineId: string, action: 'ACCEPT' | 'REJECT'): Observable<CaseDeadline | null> {
    return this.http.patch<CaseDeadline | null>(
      `${this.url(caseFileId)}/${deadlineId}/validate`,
      { action }
    );
  }
}
