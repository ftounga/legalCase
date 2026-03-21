import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Workspace } from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly apiUrl = '/api/v1/workspaces';

  private readonly workspaceSwitchedSource = new Subject<void>();
  readonly workspaceSwitched$ = this.workspaceSwitchedSource.asObservable();

  constructor(private http: HttpClient) {}

  getCurrentWorkspace(): Observable<Workspace> {
    return this.http.get<Workspace>(`${this.apiUrl}/current`);
  }

  createWorkspace(name: string): Observable<Workspace> {
    return this.http.post<Workspace>(this.apiUrl, { name });
  }

  listWorkspaces(): Observable<Workspace[]> {
    return this.http.get<Workspace[]>(this.apiUrl);
  }

  switchWorkspace(id: string): Observable<Workspace> {
    return this.http.post<Workspace>(`${this.apiUrl}/${id}/switch`, {});
  }

  notifyWorkspaceSwitched(): void {
    this.workspaceSwitchedSource.next();
  }
}
