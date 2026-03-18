import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsageEvent } from '../models/usage-event.model';

@Injectable({ providedIn: 'root' })
export class UsageEventService {
  constructor(private http: HttpClient) {}

  getUsageEvents(caseFileId: string): Observable<UsageEvent[]> {
    return this.http.get<UsageEvent[]>(`/api/v1/case-files/${caseFileId}/usage`);
  }
}
