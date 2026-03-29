import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseNote } from '../models/case-note.model';

@Injectable({ providedIn: 'root' })
export class CaseNoteService {
  constructor(private http: HttpClient) {}

  private url(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/notes`;
  }

  list(caseFileId: string): Observable<CaseNote[]> {
    return this.http.get<CaseNote[]>(this.url(caseFileId));
  }

  create(caseFileId: string, content: string): Observable<CaseNote> {
    return this.http.post<CaseNote>(this.url(caseFileId), { content });
  }

  update(caseFileId: string, noteId: string, content: string): Observable<CaseNote> {
    return this.http.put<CaseNote>(`${this.url(caseFileId)}/${noteId}`, { content });
  }

  delete(caseFileId: string, noteId: string): Observable<void> {
    return this.http.delete<void>(`${this.url(caseFileId)}/${noteId}`);
  }
}
