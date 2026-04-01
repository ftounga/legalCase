import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { InviteAcceptComponent, PENDING_INVITATION_TOKEN_KEY } from './invite-accept.component';
import { AuthService } from '../core/services/auth.service';
import { WorkspaceInvitationService } from '../core/services/workspace-invitation.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('InviteAcceptComponent', () => {
  let component: InviteAcceptComponent;
  let authService: jest.Mocked<AuthService>;
  let invitationService: jest.Mocked<WorkspaceInvitationService>;
  let router: jest.Mocked<Router>;

  function createComponent(token: string | null): void {
    TestBed.configureTestingModule({
      imports: [InviteAcceptComponent, NoopAnimationsModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => token } } } },
        { provide: AuthService, useValue: authService },
        { provide: WorkspaceInvitationService, useValue: invitationService },
        { provide: Router, useValue: router }
      ]
    });
    const fixture = TestBed.createComponent(InviteAcceptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', ['loadCurrentUser']);
    invitationService = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.resetTestingModule();
  });

  it('pas de token → state error', () => {
    authService.loadCurrentUser.mockReturnValue(of(null));
    createComponent(null);
    expect(component.state).toBe('error');
  });

  it('token présent, user non connecté → stocke token et redirige vers /login', () => {
    authService.loadCurrentUser.mockReturnValue(of(null));
    localStorage.removeItem(PENDING_INVITATION_TOKEN_KEY);
    createComponent('tok123');
    expect(localStorage.getItem(PENDING_INVITATION_TOKEN_KEY)).toBe('tok123');
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('token présent, user connecté → accepte invitation → state success', () => {
    const user = { id: 'u1', email: 'alice@test.com', firstName: null, lastName: null, provider: 'GOOGLE', isSuperAdmin: false };
    authService.loadCurrentUser.mockReturnValue(of(user));
    invitationService.acceptInvitation.mockReturnValue(of(undefined as unknown as void));
    createComponent('tok123');
    expect(component.state).toBe('success');
  });

  it('token présent, user connecté, erreur 409 → state error avec message expiré', () => {
    const user = { id: 'u1', email: 'alice@test.com', firstName: null, lastName: null, provider: 'GOOGLE', isSuperAdmin: false };
    authService.loadCurrentUser.mockReturnValue(of(user));
    invitationService.acceptInvitation.mockReturnValue(throwError(() => ({ status: 409 })));
    createComponent('tok123');
    expect(component.state).toBe('error');
    expect(component.errorMessage).toContain('expiré');
  });
});
