import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { SuperAdminComponent } from './super-admin.component';
import { SuperAdminService } from '../core/services/super-admin.service';
import { AuthService } from '../core/services/auth.service';
import { SuperAdminWorkspace, SuperAdminUsage, SuperAdminUser } from '../core/models/super-admin.model';

const mockWorkspaces: SuperAdminWorkspace[] = [
  { id: 'ws-1', name: 'Cabinet Alpha', slug: 'alpha', planCode: 'STARTER', status: 'ACTIVE', expiresAt: null, memberCount: 2, createdAt: '2026-01-01T00:00:00Z' }
];
const mockUsage: SuperAdminUsage[] = [
  { workspaceId: 'ws-1', workspaceName: 'Cabinet Alpha', totalTokensInput: 1000, totalTokensOutput: 500, totalCost: 0.012 }
];
const mockUsers: SuperAdminUser[] = [
  { id: 'u-1', email: 'alice@example.com', firstName: 'Alice', lastName: 'Dupont', workspaceCount: 1 }
];

describe('SuperAdminComponent', () => {
  let component: SuperAdminComponent;
  let fixture: ComponentFixture<SuperAdminComponent>;
  let superAdminService: jasmine.SpyObj<SuperAdminService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let router: Router;

  async function setup(isSuperAdmin: boolean, wsReturn: any, usageReturn: any, usersReturn: any) {
    superAdminService = jasmine.createSpyObj('SuperAdminService', ['listWorkspaces', 'getUsage', 'listUsers', 'deleteWorkspace', 'deleteUser']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);

    superAdminService.listWorkspaces.and.returnValue(wsReturn);
    superAdminService.getUsage.and.returnValue(usageReturn);
    superAdminService.listUsers.and.returnValue(usersReturn);

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
    await setup(true, of(mockWorkspaces), of(mockUsage), of(mockUsers));

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
      of(mockUsers)
    );

    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));

  // T-03 : non super-admin → redirection au ngOnInit
  it('redirige vers /case-files si isSuperAdmin = false', fakeAsync(async () => {
    await setup(false, of(mockWorkspaces), of(mockUsage), of(mockUsers));

    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));

  // T-04 : usage fusionné dans les lignes workspace
  it('fusionne les données de consommation dans les lignes workspace', fakeAsync(async () => {
    await setup(true, of(mockWorkspaces), of(mockUsage), of(mockUsers));

    expect(component.workspaceRows()[0].totalTokensInput).toBe(1000);
    expect(component.workspaceRows()[0].totalCost).toBe(0.012);
  }));
});
