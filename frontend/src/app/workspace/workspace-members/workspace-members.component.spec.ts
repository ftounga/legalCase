import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { WorkspaceMembersComponent } from './workspace-members.component';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { AuthService } from '../../core/services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { WorkspaceMember } from '../../core/models/workspace-member.model';
import { WorkspaceInvitation } from '../../core/models/workspace-invitation.model';

const mockMember: WorkspaceMember = {
  userId: 'u1', email: 'alice@test.com', firstName: 'Alice',
  lastName: 'Martin', memberRole: 'LAWYER', createdAt: '2026-03-01T10:00:00Z'
};

const mockInvitation: WorkspaceInvitation = {
  id: 'inv1', email: 'bob@test.com', role: 'LAWYER',
  status: 'PENDING', expiresAt: '2026-03-25T10:00:00Z', createdAt: '2026-03-18T10:00:00Z'
};

describe('WorkspaceMembersComponent', () => {
  let component: WorkspaceMembersComponent;
  let memberService: jasmine.SpyObj<WorkspaceMemberService>;
  let invitationService: jasmine.SpyObj<WorkspaceInvitationService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    memberService = jasmine.createSpyObj('WorkspaceMemberService', ['getMembers', 'removeMember']);
    invitationService = jasmine.createSpyObj('WorkspaceInvitationService', ['getInvitations', 'createInvitation', 'revokeInvitation']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    memberService.getMembers.and.returnValue(of([mockMember]));
    invitationService.getInvitations.and.returnValue(of([mockInvitation]));

    await TestBed.configureTestingModule({
      imports: [WorkspaceMembersComponent, NoopAnimationsModule, ReactiveFormsModule],
      providers: [
        { provide: WorkspaceMemberService, useValue: memberService },
        { provide: WorkspaceInvitationService, useValue: invitationService },
        { provide: AuthService, useValue: { currentUser: () => ({ email: 'alice@test.com' }) } },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(WorkspaceMembersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('loadAll — charge membres et invitations', () => {
    expect(component.members.length).toBe(1);
    expect(component.invitations.length).toBe(1);
  });

  it('sendInvitation — form invalide → ne déclenche pas createInvitation', () => {
    component.inviteForm.setValue({ email: '', role: 'LAWYER' });
    component.sendInvitation();
    expect(invitationService.createInvitation).not.toHaveBeenCalled();
  });

  it('sendInvitation — form valide → appelle createInvitation', () => {
    invitationService.createInvitation.and.returnValue(of(mockInvitation));
    invitationService.getInvitations.and.returnValue(of([mockInvitation]));
    component.inviteForm.setValue({ email: 'bob@test.com', role: 'LAWYER' });
    component.sendInvitation();
    expect(invitationService.createInvitation).toHaveBeenCalledWith('bob@test.com', 'LAWYER');
  });

  it('removeMember — retire le membre de la liste', () => {
    memberService.removeMember.and.returnValue(of(undefined as unknown as void));
    component.removeMember(mockMember);
    expect(component.members.length).toBe(0);
  });

  it('revokeInvitation — retire l\'invitation de la liste', () => {
    invitationService.revokeInvitation.and.returnValue(of(undefined as unknown as void));
    component.revokeInvitation(mockInvitation);
    expect(component.invitations.length).toBe(0);
  });

  it('isCurrentUser — renvoie true pour l\'utilisateur connecté', () => {
    expect(component.isCurrentUser(mockMember)).toBeTrue();
  });

  it('roleLabel — renvoie le libellé français', () => {
    expect(component.roleLabel('OWNER')).toBe('Propriétaire');
    expect(component.roleLabel('LAWYER')).toBe('Avocat');
  });
});
