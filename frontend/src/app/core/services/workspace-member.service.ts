import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WorkspaceMember } from '../models/workspace-member.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceMemberService {
  private readonly apiUrl = '/api/v1/workspaces/current/members';

  constructor(private http: HttpClient) {}

  getMembers(): Observable<WorkspaceMember[]> {
    return this.http.get<WorkspaceMember[]>(this.apiUrl);
  }

  removeMember(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}`);
  }
}
