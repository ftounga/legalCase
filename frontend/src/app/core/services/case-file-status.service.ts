import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseFile } from '../models/case-file.model';

@Injectable({ providedIn: 'root' })
export class CaseFileStatusService {
  private readonly apiUrl = '/api/v1/case-files';

  constructor(private http: HttpClient) {}

  close(id: string): Observable<CaseFile> {
    return this.http.patch<CaseFile>(`${this.apiUrl}/${id}/close`, {});
  }

  reopen(id: string): Observable<CaseFile> {
    return this.http.patch<CaseFile>(`${this.apiUrl}/${id}/reopen`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
