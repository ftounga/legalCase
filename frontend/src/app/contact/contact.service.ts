import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ContactPayload {
  nom: string;
  email: string;
  telephone?: string;
  sujet: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ContactService {
  private http = inject(HttpClient);

  send(payload: ContactPayload): Observable<{ status: string }> {
    return this.http.post<{ status: string }>('/api/v1/contact', payload);
  }
}
