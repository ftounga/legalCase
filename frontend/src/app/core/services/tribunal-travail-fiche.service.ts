import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TribunalTravailFiche } from '../models/tribunal-travail-fiche.model';

@Injectable({ providedIn: 'root' })
export class TribunalTravailFicheService {
  constructor(private http: HttpClient) {}

  private url(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/tribunal-travail-fiche`;
  }

  get(caseFileId: string): Observable<TribunalTravailFiche> {
    return this.http.get<TribunalTravailFiche>(this.url(caseFileId));
  }

  save(caseFileId: string, fiche: Omit<TribunalTravailFiche, 'id' | 'piecesList' | 'updatedAt'>): Observable<TribunalTravailFiche> {
    return this.http.put<TribunalTravailFiche>(this.url(caseFileId), fiche);
  }
}
