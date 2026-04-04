import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReferentialResponse, ReferentialUpdateResponse } from '../models/referential.model';

@Injectable({ providedIn: 'root' })
export class ReferentialService {
  constructor(private http: HttpClient) {}

  getReferentials(domain: string): Observable<ReferentialResponse> {
    return this.http.get<ReferentialResponse>(`/api/v1/referentials`, {
      params: { domain }
    });
  }

  updateReferential(id: string, label: string, valueJson: string, force: boolean): Observable<ReferentialUpdateResponse> {
    return this.http.put<ReferentialUpdateResponse>(`/api/v1/referentials/${id}`, { label, valueJson, force });
  }
}
