import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PrudhomeFiche } from '../models/prudhome-fiche.model';

@Injectable({ providedIn: 'root' })
export class PrudhomeFicheService {
  constructor(private http: HttpClient) {}

  private url(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/prudhome-fiche`;
  }

  get(caseFileId: string): Observable<PrudhomeFiche> {
    return this.http.get<PrudhomeFiche>(this.url(caseFileId));
  }

  save(caseFileId: string, fiche: Omit<PrudhomeFiche, 'id' | 'piecesList' | 'updatedAt'>): Observable<PrudhomeFiche> {
    return this.http.put<PrudhomeFiche>(this.url(caseFileId), fiche);
  }
}
