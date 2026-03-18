import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WorkspaceInvitation } from '../models/workspace-invitation.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceInvitationService {
  private readonly baseUrl = '/api/v1/workspaces/current/invitations';
  private readonly acceptUrl = '/api/v1/workspace/invitations/accept';

  constructor(private http: HttpClient) {}

  createInvitation(email: string, role: string): Observable<WorkspaceInvitation> {
    return this.http.post<WorkspaceInvitation>(this.baseUrl, { email, role });
  }

  getInvitations(): Observable<WorkspaceInvitation[]> {
    return this.http.get<WorkspaceInvitation[]>(this.baseUrl);
  }

  revokeInvitation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  acceptInvitation(token: string): Observable<void> {
    return this.http.post<void>(this.acceptUrl, { token });
  }
}
