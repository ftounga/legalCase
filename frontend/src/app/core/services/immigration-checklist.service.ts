import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ImmigrationChecklist } from '../models/immigration-checklist.model';

@Injectable({ providedIn: 'root' })
export class ImmigrationChecklistService {
  constructor(private http: HttpClient) {}

  get(caseFileId: string, titreType: string, country: string): Observable<ImmigrationChecklist> {
    return this.http.get<ImmigrationChecklist>(
      `/api/v1/case-files/${caseFileId}/immigration-checklist`,
      { params: { titreType, country } }
    );
  }

  upsert(caseFileId: string, body: { titreType: string; country: string; pieces: { label: string; statut: string }[] }): Observable<ImmigrationChecklist> {
    return this.http.put<ImmigrationChecklist>(
      `/api/v1/case-files/${caseFileId}/immigration-checklist`,
      body
    );
  }
}
