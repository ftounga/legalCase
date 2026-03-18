import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WorkspaceMemberService } from './workspace-member.service';
import { WorkspaceMember } from '../models/workspace-member.model';

const mockMember: WorkspaceMember = {
  userId: 'u1', email: 'alice@test.com', firstName: 'Alice',
  lastName: 'Martin', memberRole: 'LAWYER', createdAt: '2026-03-01T10:00:00Z'
};

describe('WorkspaceMemberService', () => {
  let service: WorkspaceMemberService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(WorkspaceMemberService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getMembers — GET /api/v1/workspaces/current/members', () => {
    service.getMembers().subscribe(members => {
      expect(members.length).toBe(1);
      expect(members[0].email).toBe('alice@test.com');
    });
    const req = http.expectOne('/api/v1/workspaces/current/members');
    expect(req.request.method).toBe('GET');
    req.flush([mockMember]);
  });

  it('removeMember — DELETE /api/v1/workspaces/current/members/:userId', () => {
    service.removeMember('u1').subscribe();
    const req = http.expectOne('/api/v1/workspaces/current/members/u1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
