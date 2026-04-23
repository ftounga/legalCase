import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ShellComponent } from './shell.component';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { ReferentialService } from '../../core/services/referential.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { PENDING_INVITATION_TOKEN_KEY } from '../../invite-accept/invite-accept.component';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Workspace } from '../../core/models/workspace.model';
import { BreakpointObserver } from '@angular/cdk/layout';

const referentialServiceStub = {
  getPendingAlertsCount: () => of({ count: 0 })
};

const ws1: Workspace = { id: 'ws-1', name: 'Cabinet Alpha', slug: 'alpha', planCode: 'FREE', status: 'ACTIVE', primary: true };
const ws2: Workspace = { id: 'ws-2', name: 'Cabinet Beta', slug: 'beta', planCode: 'FREE', status: 'ACTIVE', primary: false };

describe('ShellComponent — invitation pendante', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;
  let workspaceService: jest.Mocked<WorkspaceService>;
  let invitationService: jest.Mocked<WorkspaceInvitationService>;
  let snackBar: jest.Mocked<MatSnackBar>;

  beforeEach(async () => {
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    workspaceService.listWorkspaces.mockReturnValue(of([ws1]));
    invitationService = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    const authServiceStub = { currentUser: signal(null), logout: () => {} };

    await TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: ReferentialService, useValue: referentialServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
    localStorage.clear();
  });

  afterEach(() => localStorage.clear());

  // T-01 : sans token pending → ready passe à true après getCurrentWorkspace
  it('sans token pending, ready passe à true après chargement workspace', fakeAsync(() => {
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));

    fixture.detectChanges();
    tick();

    expect(component.ready()).toBe(true);
    expect(component.workspace()).toEqual(ws1);
    expect(invitationService.acceptInvitation).not.toHaveBeenCalled();
  }));

  // T-02 : avec token pending + succès → ready false pendant acceptation, true après reload workspace
  it('avec token pending, ready reste false puis passe à true après reload', fakeAsync(() => {
    localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, 'tok-abc');
    invitationService.acceptInvitation.mockReturnValue(of(void 0));
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));

    expect(component.ready()).toBe(false);
    fixture.detectChanges();

    expect(invitationService.acceptInvitation).toHaveBeenCalledWith('tok-abc');
    expect(localStorage.getItem(PENDING_INVITATION_TOKEN_KEY)).toBeNull();

    tick();

    expect(component.ready()).toBe(true);
    expect(component.workspace()).toEqual(ws1);
  }));

  // T-03 : token pending + erreur acceptation → ready passe à true (fail-open)
  it('token pending + erreur acceptation : ready passe à true (fail-open)', fakeAsync(() => {
    localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, 'tok-expired');
    invitationService.acceptInvitation.mockReturnValue(throwError(() => ({ status: 409 })));
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));

    fixture.detectChanges();
    tick();

    expect(component.ready()).toBe(true);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('invalide'), expect.any(String), expect.any(Object)
    );
  }));

  // T-04 : token pending + succès → workspace rechargé avec la nouvelle valeur
  it('token pending + succès : workspace mis à jour avec la valeur rechargée', fakeAsync(() => {
    localStorage.setItem(PENDING_INVITATION_TOKEN_KEY, 'tok-valid');
    invitationService.acceptInvitation.mockReturnValue(of(void 0));
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));

    fixture.detectChanges();
    tick();

    expect(component.workspace()).toEqual(ws1);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Invitation acceptée'), expect.any(String), expect.any(Object)
    );
  }));
});

describe('ShellComponent — workspace switcher', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;
  let workspaceService: jest.Mocked<WorkspaceService>;
  let router: Router;
  let snackBar: jest.Mocked<MatSnackBar>;

  beforeEach(async () => {
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    const authServiceStub = { currentUser: signal(null), logout: () => {} };
    const invitationServiceStub = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);

    await TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationServiceStub },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: ReferentialService, useValue: referentialServiceStub }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
    localStorage.clear();
  });

  // T-05 : 1 workspace → workspaces.length = 1
  it('un seul workspace → signal workspaces contient 1 élément', fakeAsync(() => {
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1]));

    fixture.detectChanges();
    tick();

    expect(component.workspaces().length).toBe(1);
  }));

  // T-06 : 2 workspaces → workspaces.length = 2
  it('deux workspaces → signal workspaces contient 2 éléments', fakeAsync(() => {
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1, ws2]));

    fixture.detectChanges();
    tick();

    expect(component.workspaces().length).toBe(2);
  }));

  // T-07 : switchTo → switchWorkspace appelé + workspace rechargé + navigate /case-files
  it('switchTo → appel switchWorkspace, workspace mis à jour, navigate /case-files', fakeAsync(() => {
    const navigateSpy = spyOn(router, 'navigate');
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1, ws2]));
    const switched = { ...ws2, primary: true };
    workspaceService.switchWorkspace.mockReturnValue(of(switched));

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
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1, ws2]));
    workspaceService.switchWorkspace.mockReturnValue(throwError(() => ({ status: 403 })));

    fixture.detectChanges();
    tick();

    component.switchTo(ws2);
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Erreur'), expect.any(String), expect.any(Object)
    );
    expect(component.workspace()).toEqual(ws1);
  }));

  // F-154 SF-154-01 — T-09 : openCreateWorkspaceDialog → switch + reload + navigate /case-files
  it('openCreateWorkspaceDialog → succès création → switch vers nouveau workspace + navigate', fakeAsync(() => {
    const navigateSpy = spyOn(router, 'navigate');
    const newWs: any = { id: 'ws-new', name: 'Cabinet Immigration FR',
      legalDomain: 'DROIT_IMMIGRATION', country: 'FRANCE', primary: true };
    const afterClosedSubject = new (require('rxjs').Subject)();
    const dialog = TestBed.inject(MatDialog);
    jest.spyOn(dialog, 'open').mockReturnValue({ afterClosed: () => afterClosedSubject.asObservable() } as any);

    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1]));
    workspaceService.switchWorkspace.mockReturnValue(of(newWs));

    fixture.detectChanges();
    tick();

    component.openCreateWorkspaceDialog();
    afterClosedSubject.next(newWs);
    afterClosedSubject.complete();
    tick();

    expect(workspaceService.switchWorkspace).toHaveBeenCalledWith('ws-new');
    expect(component.workspace()).toEqual(newWs);
    expect(navigateSpy).toHaveBeenCalledWith(['/case-files']);
  }));

  // T-10 : dialog fermé sans résultat (annulation) → pas de switch
  it('openCreateWorkspaceDialog → dialog annulé (null) → pas de switch ni navigate', fakeAsync(() => {
    const navigateSpy = spyOn(router, 'navigate');
    const afterClosedSubject = new (require('rxjs').Subject)();
    const dialog = TestBed.inject(MatDialog);
    jest.spyOn(dialog, 'open').mockReturnValue({ afterClosed: () => afterClosedSubject.asObservable() } as any);

    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1]));

    fixture.detectChanges();
    tick();

    component.openCreateWorkspaceDialog();
    afterClosedSubject.next(null);
    afterClosedSubject.complete();
    tick();

    expect(workspaceService.switchWorkspace).not.toHaveBeenCalled();
    expect(navigateSpy).not.toHaveBeenCalled();
  }));
});

describe('ShellComponent — domainColor()', () => {
  let component: ShellComponent;

  beforeEach(async () => {
    const workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1]));
    const authServiceStub = { currentUser: signal(null), logout: () => {} };
    const invitationServiceStub = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);

    await TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationServiceStub },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: ReferentialService, useValue: referentialServiceStub }
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

  function setupWithSuperAdmin(isSuperAdmin: boolean) {
    const workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1]));

    const authServiceStub = {
      currentUser: signal<any>({ id: 'u-1', email: 'user@test.com', isSuperAdmin }),
      logout: () => {}
    };
    const invitationServiceStub = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);

    TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationServiceStub },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: ReferentialService, useValue: referentialServiceStub }
      ]
    });

    fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // T-09 : lien super-admin visible si isSuperAdmin = true
  it('affiche le lien Super-admin si isSuperAdmin = true', fakeAsync(async () => {
    setupWithSuperAdmin(true);
    expect(fixture.nativeElement.textContent).toContain('Super-admin');
  }));

  // T-10 : lien super-admin absent si isSuperAdmin = false
  it('n\'affiche pas le lien Super-admin si isSuperAdmin = false', fakeAsync(async () => {
    setupWithSuperAdmin(false);
    expect(fixture.nativeElement.textContent).not.toContain('Super-admin');
  }));
});

describe('ShellComponent — badge alertes référentiels', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;

  function setup(pendingCount: number) {
    const workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1]));
    const authServiceStub = { currentUser: signal(null), logout: () => {} };
    const invitationServiceStub = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);
    const referentialMock = { getPendingAlertsCount: () => of({ count: pendingCount }) };

    TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationServiceStub },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: ReferentialService, useValue: referentialMock }
      ]
    });

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  // T-BADGE-01 : pendingAlertsCount = 0 si API retourne 0
  it('pendingAlertsCount = 0 si aucune alerte', fakeAsync(() => {
    setup(0);
    tick();
    expect(component.pendingAlertsCount()).toBe(0);
  }));

  // T-BADGE-02 : pendingAlertsCount = 3 si API retourne 3
  it('pendingAlertsCount = 3 si 3 alertes en attente', fakeAsync(() => {
    setup(3);
    tick();
    expect(component.pendingAlertsCount()).toBe(3);
  }));
});

describe('ShellComponent — responsive mobile', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let component: ShellComponent;
  let breakpointObserver: jest.Mocked<BreakpointObserver>;

  function setup(mobileMatches: boolean) {
    breakpointObserver = jasmine.createSpyObj('BreakpointObserver', ['observe']);
    breakpointObserver.observe.mockReturnValue(of({ matches: mobileMatches, breakpoints: {} }));

    const workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace', 'listWorkspaces', 'switchWorkspace', 'notifyWorkspaceSwitched']);
    workspaceService.getCurrentWorkspace.mockReturnValue(of(ws1));
    workspaceService.listWorkspaces.mockReturnValue(of([ws1]));

    const authServiceStub = { currentUser: signal(null), logout: () => {} };
    const invitationServiceStub = jasmine.createSpyObj('WorkspaceInvitationService', ['acceptInvitation']);

    TestBed.configureTestingModule({
      imports: [ShellComponent, RouterModule.forRoot([]), NoopAnimationsModule],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceStub },
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceInvitationService, useValue: invitationServiceStub },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: BreakpointObserver, useValue: breakpointObserver },
        { provide: ReferentialService, useValue: referentialServiceStub }
      ]
    });

    fixture = TestBed.createComponent(ShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  // T-11 : isMobile = true si BreakpointObserver émet matches = true
  it('isMobile = true si le breakpoint mobile correspond', fakeAsync(async () => {
    setup(true);
    tick();
    expect(component.isMobile()).toBe(true);
  }));

  // T-12 : sidenavOpen = false au init quand isMobile = true
  it('sidenavOpen = false au init quand mobile', fakeAsync(async () => {
    setup(true);
    tick();
    expect(component.sidenavOpen()).toBe(false);
  }));

  // T-13 : onNavClick() ferme la sidenav sur mobile
  it('onNavClick ferme la sidenav sur mobile', fakeAsync(async () => {
    setup(true);
    tick();
    component.sidenavOpen.set(true);
    component.onNavClick();
    expect(component.sidenavOpen()).toBe(false);
  }));

  // T-14 : onNavClick() ne ferme pas la sidenav sur desktop
  it('onNavClick ne ferme pas la sidenav sur desktop', fakeAsync(async () => {
    setup(false);
    tick();
    component.sidenavOpen.set(true);
    component.onNavClick();
    expect(component.sidenavOpen()).toBe(true);
  }));
});
