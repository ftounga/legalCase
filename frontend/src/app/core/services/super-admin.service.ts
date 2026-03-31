import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageResponse, SuperAdminWorkspace, SuperAdminUsage, SuperAdminUser, SuperAdminMetrics } from '../models/super-admin.model';

@Injectable({ providedIn: 'root' })
export class SuperAdminService {
  private readonly base = '/api/v1/super-admin';

  constructor(private http: HttpClient) {}

  listWorkspaces(page = 0, size = 20): Observable<PageResponse<SuperAdminWorkspace>> {
    return this.http.get<PageResponse<SuperAdminWorkspace>>(`${this.base}/workspaces`, {
      params: { page, size }
    });
  }

  getUsage(): Observable<SuperAdminUsage[]> {
    return this.http.get<SuperAdminUsage[]>(`${this.base}/usage`);
  }

  listUsers(page = 0, size = 20): Observable<PageResponse<SuperAdminUser>> {
    return this.http.get<PageResponse<SuperAdminUser>>(`${this.base}/users`, {
      params: { page, size }
    });
  }

  deleteWorkspace(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/workspaces/${id}`);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/users/${id}`);
  }

  getMetrics(): Observable<SuperAdminMetrics> {
    return this.http.get<SuperAdminMetrics>(`${this.base}/metrics`);
  }
}
