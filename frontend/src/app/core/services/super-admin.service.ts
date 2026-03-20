import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SuperAdminWorkspace, SuperAdminUsage, SuperAdminUser } from '../models/super-admin.model';

@Injectable({ providedIn: 'root' })
export class SuperAdminService {
  private readonly base = '/api/v1/super-admin';

  constructor(private http: HttpClient) {}

  listWorkspaces(): Observable<SuperAdminWorkspace[]> {
    return this.http.get<SuperAdminWorkspace[]>(`${this.base}/workspaces`);
  }

  getUsage(): Observable<SuperAdminUsage[]> {
    return this.http.get<SuperAdminUsage[]>(`${this.base}/usage`);
  }

  listUsers(): Observable<SuperAdminUser[]> {
    return this.http.get<SuperAdminUser[]>(`${this.base}/users`);
  }

  deleteWorkspace(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/workspaces/${id}`);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/users/${id}`);
  }
}
