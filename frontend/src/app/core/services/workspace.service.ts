import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Workspace } from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly apiUrl = '/api/v1/workspaces';

  constructor(private http: HttpClient) {}

  getCurrentWorkspace(): Observable<Workspace> {
    return this.http.get<Workspace>(`${this.apiUrl}/current`);
  }
}
