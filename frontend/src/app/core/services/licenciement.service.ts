import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LicenciementRequest, LicenciementResponse } from '../models/licenciement.model';

/**
 * F-DT-08 — Service de l'outil décisionnel « Validité du licenciement ».
 *
 * Deux modes :
 *  - mode case-file (par défaut) : `analyze(caseFileId, req)` / `get(caseFileId)`
 *    persistent et lisent le résultat sur un dossier client donné.
 *  - mode simulateur autonome (F-163 SF-163-02a) : `analyzeStandalone(req)`
 *    POSTe sur le dispatcher `/api/v1/simulators/{toolId}/calculate` géré par
 *    SF-163-03 (backend). Aucune persistance, aucun dossier impliqué.
 */
@Injectable({ providedIn: 'root' })
export class LicenciementService {
  /**
   * F-163 SF-163-02a — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-DT-08-licenciement-validity')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-08-licenciement-validity';

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: LicenciementRequest): Observable<LicenciementResponse> {
    return this.http.post<LicenciementResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement`, request);
  }

  get(caseFileId: string): Observable<LicenciementResponse> {
    return this.http.get<LicenciementResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement`);
  }

  /**
   * F-163 SF-163-02a — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: LicenciementRequest): Observable<LicenciementResponse> {
    return this.http.post<LicenciementResponse>(
      `/api/v1/simulators/${LicenciementService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
