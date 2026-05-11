import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PartageImmobilierRequest, PartageImmobilierResponse } from '../models/partage-immobilier.model';

/**
 * F-FA-05 — Service de l'outil décisionnel « Partage immobilier ».
 *
 * Deux modes :
 *  - mode case-file (par défaut) : `calculate(caseFileId, req)` / `get(caseFileId)`
 *    persistent et lisent le résultat sur un dossier client donné.
 *  - mode simulateur autonome (F-163 SF-163-02c) : `calculateStandalone(req)`
 *    POSTe sur le dispatcher `/api/v1/simulators/{toolId}/calculate` géré par
 *    SF-163-03 (backend). Aucune persistance, aucun dossier impliqué.
 */
@Injectable({ providedIn: 'root' })
export class PartageImmobilierService {
  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-05-partage-immobilier')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-05-partage-immobilier';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: PartageImmobilierRequest): Observable<PartageImmobilierResponse> {
    return this.http.post<PartageImmobilierResponse>(`/api/v1/case-files/${caseFileId}/partage-immobilier`, request);
  }

  get(caseFileId: string): Observable<PartageImmobilierResponse> {
    return this.http.get<PartageImmobilierResponse>(`/api/v1/case-files/${caseFileId}/partage-immobilier`);
  }

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: PartageImmobilierRequest): Observable<PartageImmobilierResponse> {
    return this.http.post<PartageImmobilierResponse>(
      `/api/v1/simulators/${PartageImmobilierService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
