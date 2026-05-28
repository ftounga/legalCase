import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AuditoratTravailBeRequest,
  AuditoratTravailBeResponse,
} from '../models/auditorat-travail-be.model';

/**
 * SF-219-25b : wrapper HttpClient pour l'outil décisionnel
 * « Auditorat du travail BE — orientation et checklist de saisine du
 * parquet spécialisé en droit social pénal » (F-219, BE uniquement —
 * Code judiciaire art. 138bis + Code d'instruction criminelle art. 24
 * + Loi du 03/08/1992 sur le Code judiciaire + Loi du 06/06/2010
 * introduisant le Code pénal social).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/auditorat-travail-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. La France n'a pas d'auditorat du travail :
 * les infractions au droit du travail FR sont poursuivies par le
 * procureur de la République sur PV de l'inspection du travail (Code
 * du travail art. L. 8112-1 et s.). Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class AuditoratTravailBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/auditorat-travail-be`;
  }

  analyze(
    caseFileId: string,
    request: AuditoratTravailBeRequest,
  ): Observable<AuditoratTravailBeResponse> {
    return this.http.post<AuditoratTravailBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<AuditoratTravailBeResponse> {
    return this.http.get<AuditoratTravailBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
