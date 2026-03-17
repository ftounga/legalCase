import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface HealthResponse {
  message: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class HomeService {
  private readonly apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  getHello(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(`${this.apiUrl}/hello`);
  }
}
