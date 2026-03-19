import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ShellComponent } from './shell.component';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PENDING_INVITATION_TOKEN_KEY } from '../../invite-accept/invite-accept.component';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { Workspace } from '../../core/models/workspace.model';

const mockWorkspace: Workspace = { id: 'ws-new', name: 'Nouveau Workspace', plan: 'FREE', trialEndsAt: null } as any;

describe('ShellComponent — invitation pendante', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;
  let workspaceService: jasmine.SpyObj<WorkspaceService>;
  let invitationService: jasmine.SpyObj<WorkspaceInvitationService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    invitationService = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    const authServiceStub = { currentUser: signal(null), logout: () => {} };

    await TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationService },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
    localStorage.clear();
  });

  afterEach(() => localStorage.clear());

  // T-01 : sans token pending → ready passe à true après getCurrentWorkspace
  it('sans token pending, ready passe à true après chargement workspace', fakeAsync(() => {
    workspaceService.getCurrentWorkspace.and.returnValue(of(mockWorkspace));

    fixture.detectChanges(); // ngOnInit
    tick();

    expect(component.ready()).toBeTrue();
    expect(component.workspace()).toEqual(mockWorkspace);
    expect(invitationService.acceptInvitation).not.toHaveBeenCalled();
  }));

  // T-02 : avec token pending + succès → ready false pendant acceptation, true après reload workspace
  it('avec token pending, ready reste false pendant acceptation puis passe à true après reload', fakeAsync(() => {
    localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, 'tok-abc');
    invitationService.acceptInvitation.and.returnValue(of(void 0));
    workspaceService.getCurrentWorkspace.and.returnValue(of(mockWorkspace));

    expect(component.ready()).toBeFalse();
    fixture.detectChanges(); // ngOnInit

    // Avant tick : acceptInvitation vient d'être appelé mais workspace pas encore rechargé
    expect(invitationService.acceptInvitation).toHaveBeenCalledWith('tok-abc');
    expect(localStorage.getItem(PENDING_INVITATION_TOKEN_KEY)).toBeNull();

    tick();

    expect(component.ready()).toBeTrue();
    expect(component.workspace()).toEqual(mockWorkspace);
  }));

  // T-03 : token pending + erreur acceptation → ready passe à true malgré l'erreur (fail-open)
  it('token pending + erreur acceptation : ready passe à true (fail-open)', fakeAsync(() => {
    localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, 'tok-expired');
    invitationService.acceptInvitation.and.returnValue(throwError(() => ({ status: 409 })));
    workspaceService.getCurrentWorkspace.and.returnValue(of(mockWorkspace));

    fixture.detectChanges();
    tick();

    expect(component.ready()).toBeTrue();
    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('invalide'), jasmine.any(String), jasmine.any(Object)
    );
  }));

  // T-04 : token pending + succès → workspace rechargé avec la nouvelle valeur
  it('token pending + succès : workspace mis à jour avec la valeur rechargée', fakeAsync(() => {
    localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, 'tok-valid');
    invitationService.acceptInvitation.and.returnValue(of(void 0));
    workspaceService.getCurrentWorkspace.and.returnValue(of(mockWorkspace));

    fixture.detectChanges();
    tick();

    expect(component.workspace()).toEqual(mockWorkspace);
    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Invitation acceptée'), jasmine.any(String), jasmine.any(Object)
    );
  }));
});
