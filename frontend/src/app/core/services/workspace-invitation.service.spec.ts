import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WorkspaceInvitationService } from './workspace-invitation.service';
import { WorkspaceInvitation } from '../models/workspace-invitation.model';

const mockInvitation: WorkspaceInvitation = {
  id: 'inv1', email: 'bob@test.com', role: 'LAWYER',
  status: 'PENDING', expiresAt: '2026-03-25T10:00:00Z', createdAt: '2026-03-18T10:00:00Z'
};

describe('WorkspaceInvitationService', () => {
  let service: WorkspaceInvitationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(WorkspaceInvitationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('createInvitation — POST /api/v1/workspaces/current/invitations', () => {
    service.createInvitation('bob@test.com', 'LAWYER').subscribe(inv => {
      expect(inv.email).toBe('bob@test.com');
    });
    const req = http.expectOne('/api/v1/workspaces/current/invitations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'bob@test.com', role: 'LAWYER' });
    req.flush(mockInvitation);
  });

  it('getInvitations — GET /api/v1/workspaces/current/invitations', () => {
    service.getInvitations().subscribe(invitations => {
      expect(invitations.length).toBe(1);
    });
    const req = http.expectOne('/api/v1/workspaces/current/invitations');
    expect(req.request.method).toBe('GET');
    req.flush([mockInvitation]);
  });

  it('revokeInvitation — DELETE /api/v1/workspaces/current/invitations/:id', () => {
    service.revokeInvitation('inv1').subscribe();
    const req = http.expectOne('/api/v1/workspaces/current/invitations/inv1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('acceptInvitation — POST /api/v1/workspace/invitations/accept', () => {
    service.acceptInvitation('tok123').subscribe();
    const req = http.expectOne('/api/v1/workspace/invitations/accept');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ token: 'tok123' });
    req.flush(null);
  });
});
