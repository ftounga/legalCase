import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HelpChatResponse {
  answer: string;
}

@Injectable({ providedIn: 'root' })
export class HelpService {
  private readonly base = '/api/v1/help';

  constructor(private http: HttpClient) {}

  chat(message: string): Observable<HelpChatResponse> {
    return this.http.post<HelpChatResponse>(`${this.base}/chat`, { message });
  }
}
