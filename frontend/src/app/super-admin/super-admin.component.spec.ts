import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { SuperAdminComponent } from './super-admin.component';
import { SuperAdminService } from '../core/services/super-admin.service';
import { AuthService } from '../core/services/auth.service';
import { PageResponse, SuperAdminWorkspace, SuperAdminUsage, SuperAdminUser, SuperAdminMetrics } from '../core/models/super-admin.model';

const mockWorkspaces: SuperAdminWorkspace[] = [
  { id: 'ws-1', name: 'Cabinet Alpha', slug: 'alpha', planCode: 'STARTER', status: 'ACTIVE', expiresAt: null, memberCount: 2, createdAt: '2026-01-01T00:00:00Z' }
];
const mockUsage: SuperAdminUsage[] = [
  { workspaceId: 'ws-1', workspaceName: 'Cabinet Alpha', totalTokensInput: 1000, totalTokensOutput: 500, totalCost: 0.012 }
];
const mockUsers: SuperAdminUser[] = [
  { id: 'u-1', email: 'alice@example.com', firstName: 'Alice', lastName: 'Dupont', workspaceCount: 1 }
];
const mockMetrics: SuperAdminMetrics = {
  totalWorkspaces: 10, activeWorkspaces30d: 7, inactiveWorkspaces30d: 3,
  trialWorkspaces: 6, paidWorkspaces: 4, conversionRatePct: 40.0,
  analysesLast7Days: 12, analysesLast30Days: 45, newWorkspacesLast30Days: 2
};

function pageOf<T>(content: T[], total?: number): PageResponse<T> {
  return { content, totalElements: total ?? content.length, totalPages: 1, size: 20, number: 0 };
}

describe('SuperAdminComponent', () => {
  let component: SuperAdminComponent;
  let fixture: ComponentFixture<SuperAdminComponent>;
  let superAdminService: jasmine.SpyObj<SuperAdminService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let router: Router;

  async function setup(isSuperAdmin: boolean, wsReturn: any, usageReturn: any, usersReturn: any, metricsReturn: any = of(mockMetrics)) {
    superAdminService = jasmine.createSpyObj('SuperAdminService', ['listWorkspaces', 'getUsage', 'listUsers', 'deleteWorkspace', 'deleteUser', 'getMetrics']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);

    superAdminService.listWorkspaces.and.returnValue(wsReturn);
    superAdminService.getUsage.and.returnValue(usageReturn);
    superAdminService.listUsers.and.returnValue(usersReturn);
    superAdminService.getMetrics.and.returnValue(metricsReturn);

    const currentUser = signal<any>({ id: 'u-sa', email: 'sa@test.com', isSuperAdmin });
    const authService = { currentUser };

    await TestBed.configureTestingModule({
      imports: [SuperAdminComponent, NoopAnimationsModule],
      providers: [
        { provide: SuperAdminService, useValue: superAdminService },
        { provide: AuthService, useValue: authService },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        provideRouter([{ path: 'case-files', component: SuperAdminComponent }])
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(SuperAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // T-01 : chargement nominal — tableaux affichés
  it('affiche les tableaux workspaces et utilisateurs au chargement nominal', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));

    expect(component.loading()).toBeFalse();
    expect(component.workspaceRows().length).toBe(1);
    expect(component.users().length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Cabinet Alpha');
    expect(fixture.nativeElement.textContent).toContain('alice@example.com');
  }));

  // T-02 : 403 → redirection vers /case-files
  it('redirige vers /case-files si 403 au chargement', fakeAsync(async () => {
    await setup(true,
      throwError(() => ({ status: 403 })),
      of(mockUsage),
      of(pageOf(mockUsers))
    );

    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));

  // T-03 : non super-admin → redirection au ngOnInit
  it('redirige vers /case-files si isSuperAdmin = false', fakeAsync(async () => {
    await setup(false, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));

    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));

  // T-04 : usage fusionné dans les lignes workspace
  it('fusionne les données de consommation dans les lignes workspace', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));

    expect(component.workspaceRows()[0].totalTokensInput).toBe(1000);
    expect(component.workspaceRows()[0].totalCost).toBe(0.012);
  }));

  // T-05 : métriques chargées et signal renseigné
  it('charge les métriques et renseigne le signal', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));

    expect(component.metrics()).not.toBeNull();
    expect(component.metrics()!.totalWorkspaces).toBe(10);
    expect(component.metrics()!.conversionRatePct).toBe(40.0);
  }));

  // T-06 : section métriques présente dans le DOM
  it('affiche la section métriques dans le DOM', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));

    expect(fixture.nativeElement.textContent).toContain('Métriques plateforme');
    expect(fixture.nativeElement.textContent).toContain('Actifs (30j)');
    expect(fixture.nativeElement.textContent).toContain('Taux conversion');
  }));

  // T-07 : changement de page workspaces → listWorkspaces appelé avec page=1
  it('appelle listWorkspaces avec page=1 lors du changement de page workspaces', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces, 25)), of(mockUsage), of(pageOf(mockUsers)));
    superAdminService.listWorkspaces.and.returnValue(of(pageOf(mockWorkspaces, 25)));

    const event: PageEvent = { pageIndex: 1, pageSize: 20, length: 25 };
    component.onWsPageChange(event);
    tick();

    expect(superAdminService.listWorkspaces).toHaveBeenCalledWith(1, 20);
    expect(component.wsPage()).toBe(1);
  }));

  // T-08 : changement de taille utilisateurs → listUsers appelé avec size=5
  it('appelle listUsers avec size=5 lors du changement de taille', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers, 10)));
    superAdminService.listUsers.and.returnValue(of(pageOf(mockUsers, 10)));

    const event: PageEvent = { pageIndex: 0, pageSize: 5, length: 10 };
    component.onUsersPageChange(event);
    tick();

    expect(superAdminService.listUsers).toHaveBeenCalledWith(0, 5);
    expect(component.usersSize()).toBe(5);
  }));

  // T-09 : totalElements renseigné dans les signals
  it('renseigne wsTotalElements et usersTotalElements depuis la réponse backend', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces, 42)), of(mockUsage), of(pageOf(mockUsers, 17)));

    expect(component.wsTotalElements()).toBe(42);
    expect(component.usersTotalElements()).toBe(17);
  }));

  // T-H1 : section outils présente dans le DOM
  it('affiche la section Outils & monitoring', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));
    tick();
    fixture.detectChanges();

    const section = fixture.nativeElement.querySelector('.tools-section');
    expect(section).toBeTruthy();
  }));

  // T-H2 : les 7 outils sont rendus
  it('affiche les 7 outils avec leur data-testid', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));
    tick();
    fixture.detectChanges();

    const tools = ['tool-ga4', 'tool-sentry', 'tool-stripe', 'tool-brevo', 'tool-n8n', 'tool-aws', 'tool-rabbitmq'];
    tools.forEach(id => {
      const el = fixture.nativeElement.querySelector(`[data-testid="${id}"]`);
      expect(el).withContext(`tool ${id} manquant`).toBeTruthy();
    });
  }));

  // T-H3 : chaque lien a target="_blank" et rel="noopener noreferrer"
  it('chaque lien outil a target="_blank" et rel="noopener noreferrer"', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));
    tick();
    fixture.detectChanges();

    const links: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('.tool-card');
    expect(links.length).toBe(7);
    links.forEach(link => {
      expect(link.target).withContext(`target manquant sur ${link.href}`).toBe('_blank');
      expect(link.rel).withContext(`rel manquant sur ${link.href}`).toBe('noopener noreferrer');
    });
  }));

  // T-10 : erreur sur changement de page workspaces → snackbar, tableau inchangé
  it('affiche un snackbar si erreur lors du changement de page workspaces', fakeAsync(async () => {
    await setup(true, of(pageOf(mockWorkspaces)), of(mockUsage), of(pageOf(mockUsers)));
    superAdminService.listWorkspaces.and.returnValue(throwError(() => ({ status: 500 })));

    const rowsBefore = component.workspaceRows();
    const event: PageEvent = { pageIndex: 1, pageSize: 20, length: 100 };
    component.onWsPageChange(event);
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('workspaces'), jasmine.any(String), jasmine.any(Object)
    );
    expect(component.workspaceRows()).toEqual(rowsBefore);
  }));
});
