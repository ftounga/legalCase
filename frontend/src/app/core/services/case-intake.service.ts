import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseIntake, CaseIntakeUpdate } from '../models/case-intake.model';

/**
 * F-285 / SF-285-01 — Accès HTTP à la qualification d'entrée du dossier.
 * Consomme le contrat API figé : GET/PUT /api/v1/case-files/{id}/intake.
 */
@Injectable({ providedIn: 'root' })
export class CaseIntakeService {
  constructor(private http: HttpClient) {}

  /** Qualification courante d'un dossier (tout-null + qualified=false si jamais qualifié). */
  get(caseFileId: string): Observable<CaseIntake> {
    return this.http.get<CaseIntake>(`/api/v1/case-files/${caseFileId}/intake`);
  }

  /** Met à jour la qualification (tous les champs sont optionnels). */
  update(caseFileId: string, payload: CaseIntakeUpdate): Observable<CaseIntake> {
    return this.http.put<CaseIntake>(`/api/v1/case-files/${caseFileId}/intake`, payload);
  }
}
