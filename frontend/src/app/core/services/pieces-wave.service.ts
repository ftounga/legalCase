import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PiecesWave } from '../models/pieces-wave.model';

/** F-283 / SF-283-02 — accès à la « vague de pièces » (pièces ajoutées depuis la dernière analyse). */
@Injectable({ providedIn: 'root' })
export class PiecesWaveService {
  constructor(private http: HttpClient) {}

  wave(caseFileId: string): Observable<PiecesWave> {
    return this.http.get<PiecesWave>(`/api/v1/case-files/${caseFileId}/pieces-wave`);
  }
}
