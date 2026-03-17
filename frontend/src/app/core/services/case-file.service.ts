import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseFile, CreateCaseFileRequest } from '../models/case-file.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class CaseFileService {
  private readonly apiUrl = '/api/v1/case-files';

  constructor(private http: HttpClient) {}

  list(page = 0, size = 20): Observable<Page<CaseFile>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<CaseFile>>(this.apiUrl, { params });
  }

  getById(id: string): Observable<CaseFile> {
    return this.http.get<CaseFile>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateCaseFileRequest): Observable<CaseFile> {
    return this.http.post<CaseFile>(this.apiUrl, request);
  }
}
