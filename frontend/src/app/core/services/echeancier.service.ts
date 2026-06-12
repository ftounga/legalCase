import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EcheancierResponse } from '../models/echeancier.model';

/** F-284 / SF-284-02 — échéancier procédural proactif (lecture seule) d'un dossier. */
@Injectable({ providedIn: 'root' })
export class EcheancierService {
  constructor(private http: HttpClient) {}

  get(caseFileId: string): Observable<EcheancierResponse> {
    return this.http.get<EcheancierResponse>(`/api/v1/case-files/${caseFileId}/echeancier`);
  }
}
