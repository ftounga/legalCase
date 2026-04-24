import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TravailProcedureCode,
  TravailProcedureJalon,
} from '../models/travail-procedure.model';

/**
 * SF-136-02 : wrapper HttpClient pour la lecture du calendrier procédural
 * travail (FR + BE). Consomme l'endpoint `GET
 * /api/v1/case-files/{caseFileId}/travail-procedure-jalons` exposé par
 * `TravailProcedureController`.
 */
@Injectable({ providedIn: 'root' })
export class TravailProcedureService {

  constructor(private http: HttpClient) {}

  /**
   * Récupère la liste des jalons procéduraux pour un type de procédure.
   *
   * @param caseFileId UUID du dossier (isolation workspace côté backend)
   * @param typeProcedure code parmi les 6 valeurs autorisées
   * @param dateDeclencheur ISO `YYYY-MM-DD` optionnel ; si fourni → chaque
   *        jalon embarque `dateCible = dateDeclencheur + offsetDays`.
   */
  getJalons(caseFileId: string,
            typeProcedure: TravailProcedureCode,
            dateDeclencheur?: string | null): Observable<TravailProcedureJalon[]> {
    let params = new HttpParams().set('typeProcedure', typeProcedure);
    if (dateDeclencheur) {
      params = params.set('dateDeclencheur', dateDeclencheur);
    }
    return this.http.get<TravailProcedureJalon[]>(
      `/api/v1/case-files/${caseFileId}/travail-procedure-jalons`,
      { params },
    );
  }
}
