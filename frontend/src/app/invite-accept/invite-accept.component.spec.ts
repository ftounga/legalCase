import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { InviteAcceptComponent, PENDING_INVITATION_TOKEN_KEY } from './invite-accept.component';
import { AuthService } from '../core/services/auth.service';
import { WorkspaceInvitationService } from '../core/services/workspace-invitation.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('InviteAcceptComponent', () => {
  let component: InviteAcceptComponent;
  let authService: jasmine.SpyObj<AuthService>;
  let invitationService: jasmine.SpyObj<WorkspaceInvitationService>;
  let router: jasmine.SpyObj<Router>;

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
    authService = jasmine.createSpyObj('AuthService', ['loadCurrentUser', 'loginWithGoogle']);
    invitationService = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.resetTestingModule();
  });

  it('pas de token → state error', () => {
    authService.loadCurrentUser.and.returnValue(of(null));
    createComponent(null);
    expect(component.state).toBe('error');
  });

  it('token présent, user non connecté → stocke token et appelle loginWithGoogle', () => {
    authService.loadCurrentUser.and.returnValue(of(null));
    spyOn(localStorage, 'setItem');
    createComponent('tok123');
    expect(localStorage.setItem).toHaveBeenCalledWith(PENDING_INVITATION_TOKEN_KEY, 'tok123');
    expect(authService.loginWithGoogle).toHaveBeenCalled();
  });

  it('token présent, user connecté → accepte invitation → state success', () => {
    const user = { id: 'u1', email: 'alice@test.com', firstName: null, lastName: null, provider: 'GOOGLE', isSuperAdmin: false };
    authService.loadCurrentUser.and.returnValue(of(user));
    invitationService.acceptInvitation.and.returnValue(of(undefined as unknown as void));
    createComponent('tok123');
    expect(component.state).toBe('success');
  });

  it('token présent, user connecté, erreur 409 → state error avec message expiré', () => {
    const user = { id: 'u1', email: 'alice@test.com', firstName: null, lastName: null, provider: 'GOOGLE', isSuperAdmin: false };
    authService.loadCurrentUser.and.returnValue(of(user));
    invitationService.acceptInvitation.and.returnValue(throwError(() => ({ status: 409 })));
    createComponent('tok123');
    expect(component.state).toBe('error');
    expect(component.errorMessage).toContain('expiré');
  });
});
