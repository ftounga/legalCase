import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ShellComponent } from './shell.component';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PENDING_INVITATION_TOKEN_KEY } from '../../invite-accept/invite-accept.component';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { Workspace } from '../../core/models/workspace.model';

const ws1: Workspace = { id: 'ws-1', name: 'Cabinet Alpha', slug: 'alpha', planCode: 'FREE', status: 'ACTIVE', primary: true };
const ws2: Workspace = { id: 'ws-2', name: 'Cabinet Beta', slug: 'beta', planCode: 'FREE', status: 'ACTIVE', primary: false };

describe('ShellComponent — invitation pendante', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;
  let workspaceService: jasmine.SpyObj<WorkspaceService>;
  let invitationService: jasmine.SpyObj<WorkspaceInvitationService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    workspaceService.listWorkspaces.and.returnValue(of([ws1]));
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
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));

    fixture.detectChanges();
    tick();

    expect(component.ready()).toBeTrue();
    expect(component.workspace()).toEqual(ws1);
    expect(invitationService.acceptInvitation).not.toHaveBeenCalled();
  }));

  // T-02 : avec token pending + succès → ready false pendant acceptation, true après reload workspace
  it('avec token pending, ready reste false puis passe à true après reload', fakeAsync(() => {
    localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, 'tok-abc');
    invitationService.acceptInvitation.and.returnValue(of(void 0));
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));

    expect(component.ready()).toBeFalse();
    fixture.detectChanges();

    expect(invitationService.acceptInvitation).toHaveBeenCalledWith('tok-abc');
    expect(localStorage.getItem(PENDING_INVITATION_TOKEN_KEY)).toBeNull();

    tick();

    expect(component.ready()).toBeTrue();
    expect(component.workspace()).toEqual(ws1);
  }));

  // T-03 : token pending + erreur acceptation → ready passe à true (fail-open)
  it('token pending + erreur acceptation : ready passe à true (fail-open)', fakeAsync(() => {
    localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, 'tok-expired');
    invitationService.acceptInvitation.and.returnValue(throwError(() => ({ status: 409 })));
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));

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
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));

    fixture.detectChanges();
    tick();

    expect(component.workspace()).toEqual(ws1);
    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Invitation acceptée'), jasmine.any(String), jasmine.any(Object)
    );
  }));
});

describe('ShellComponent — workspace switcher', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;
  let workspaceService: jasmine.SpyObj<WorkspaceService>;
  let router: Router;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    const authServiceStub = { currentUser: signal(null), logout: () => {} };
    const invitationServiceStub = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);

    await TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationServiceStub },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
    localStorage.clear();
  });

  // T-05 : 1 workspace → workspaces.length = 1
  it('un seul workspace → signal workspaces contient 1 élément', fakeAsync(() => {
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));
    workspaceService.listWorkspaces.and.returnValue(of([ws1]));

    fixture.detectChanges();
    tick();

    expect(component.workspaces().length).toBe(1);
  }));

  // T-06 : 2 workspaces → workspaces.length = 2
  it('deux workspaces → signal workspaces contient 2 éléments', fakeAsync(() => {
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));
    workspaceService.listWorkspaces.and.returnValue(of([ws1, ws2]));

    fixture.detectChanges();
    tick();

    expect(component.workspaces().length).toBe(2);
  }));

  // T-07 : switchTo → switchWorkspace appelé + workspace rechargé + navigate /case-files
  it('switchTo → appel switchWorkspace, workspace mis à jour, navigate /case-files', fakeAsync(() => {
    const navigateSpy = spyOn(router, 'navigate');
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));
    workspaceService.listWorkspaces.and.returnValue(of([ws1, ws2]));
    const switched = { ...ws2, primary: true };
    workspaceService.switchWorkspace.and.returnValue(of(switched));

    fixture.detectChanges();
    tick();

    component.switchTo(ws2);
    tick();

    expect(workspaceService.switchWorkspace).toHaveBeenCalledWith('ws-2');
    expect(component.workspace()).toEqual(switched);
    expect(navigateSpy).toHaveBeenCalledWith(['/case-files']);
  }));

  // T-08 : erreur switch → snackbar erreur, workspace inchangé
  it('erreur switch → snackbar erreur, workspace inchangé', fakeAsync(() => {
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));
    workspaceService.listWorkspaces.and.returnValue(of([ws1, ws2]));
    workspaceService.switchWorkspace.and.returnValue(throwError(() => ({ status: 403 })));

    fixture.detectChanges();
    tick();

    component.switchTo(ws2);
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Erreur'), jasmine.any(String), jasmine.any(Object)
    );
    expect(component.workspace()).toEqual(ws1);
  }));
});

describe('ShellComponent — domainColor()', () => {
  let component: ShellComponent;

  beforeEach(async () => {
    const workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));
    workspaceService.listWorkspaces.and.returnValue(of([ws1]));
    const authServiceStub = { currentUser: signal(null), logout: () => {} };
    const invitationServiceStub = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);

    await TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationServiceStub },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) }
      ]
    }).compileComponents();

    component = TestBed.createComponent(ShellComponent).componentInstance;
  });

  it('DROIT_DU_TRAVAIL → #27AE60', () => {
    expect(component.domainColor('DROIT_DU_TRAVAIL')).toBe('#27AE60');
  });

  it('DROIT_IMMIGRATION → #1A3A5C', () => {
    expect(component.domainColor('DROIT_IMMIGRATION')).toBe('#1A3A5C');
  });

  it('DROIT_FAMILLE → #C9973A', () => {
    expect(component.domainColor('DROIT_FAMILLE')).toBe('#C9973A');
  });

  it('domaine inconnu → couleur par défaut #1A3A5C', () => {
    expect(component.domainColor(undefined)).toBe('#1A3A5C');
  });
});

describe('ShellComponent — lien super-admin', () => {
  let fixture: ComponentFixture<ShellComponent>;

  async function setupWithSuperAdmin(isSuperAdmin: boolean) {
    const workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    workspaceService.getCurrentWorkspace.and.returnValue(of(ws1));
    workspaceService.listWorkspaces.and.returnValue(of([ws1]));

    const authServiceStub = {
      currentUser: signal<any>({ id: 'u-1', email: 'user@test.com', isSuperAdmin }),
      logout: () => {}
    };
    const invitationServiceStub = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);

    await TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationServiceStub },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // T-09 : lien super-admin visible si isSuperAdmin = true
  it('affiche le lien Super-admin si isSuperAdmin = true', fakeAsync(async () => {
    await setupWithSuperAdmin(true);
    expect(fixture.nativeElement.textContent).toContain('Super-admin');
  }));

  // T-10 : lien super-admin absent si isSuperAdmin = false
  it('n\'affiche pas le lien Super-admin si isSuperAdmin = false', fakeAsync(async () => {
    await setupWithSuperAdmin(false);
    expect(fixture.nativeElement.textContent).not.toContain('Super-admin');
  }));
});
