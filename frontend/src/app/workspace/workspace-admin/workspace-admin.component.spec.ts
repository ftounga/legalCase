import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { WorkspaceAdminComponent } from './workspace-admin.component';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { AdminUsageService } from '../../core/services/admin-usage.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Workspace } from '../../core/models/workspace.model';
import { WorkspaceMember } from '../../core/models/workspace-member.model';
import { WorkspaceUsageSummary } from '../../core/models/workspace-usage-summary.model';
import { provideRouter } from '@angular/router';

const mockUsageSummary: WorkspaceUsageSummary = {
  totalTokensInput: 0, totalTokensOutput: 0, totalCost: 0,
  byUser: [], byCaseFile: [],
  monthlyTokensUsed: 0, monthlyTokensBudget: 0
};

const mockWorkspace: Workspace = {
  id: 'ws-1', name: 'Cabinet Alpha', slug: 'alpha',
  planCode: 'STARTER', status: 'ACTIVE'
};

const mockWorkspaceFreeWithExpiry: Workspace = {
  id: 'ws-2', name: 'Cabinet Beta', slug: 'beta',
  planCode: 'FREE', status: 'ACTIVE',
  expiresAt: '2026-04-01T00:00:00Z'
};

const mockMembers: WorkspaceMember[] = [
  { userId: 'u1', email: 'alice@test.com', firstName: null, lastName: null, memberRole: 'OWNER', createdAt: '2026-01-01T00:00:00Z' },
  { userId: 'u2', email: 'bob@test.com', firstName: null, lastName: null, memberRole: 'LAWYER', createdAt: '2026-01-01T00:00:00Z' }
];

describe('WorkspaceAdminComponent', () => {
  let component: WorkspaceAdminComponent;
  let fixture: ComponentFixture<WorkspaceAdminComponent>;
  let workspaceService: jasmine.SpyObj<WorkspaceService>;
  let memberService: jasmine.SpyObj<WorkspaceMemberService>;
  let adminUsageService: jasmine.SpyObj<AdminUsageService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  async function setup(wsReturn: any, membersReturn: any) {
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    memberService = jasmine.createSpyObj('WorkspaceMemberService', ['getMembers']);
    adminUsageService = jasmine.createSpyObj('AdminUsageService', ['getSummary']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    workspaceService.getCurrentWorkspace.and.returnValue(wsReturn);
    memberService.getMembers.and.returnValue(membersReturn);
    adminUsageService.getSummary.and.returnValue(of(mockUsageSummary));

    await TestBed.configureTestingModule({
      imports: [WorkspaceAdminComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: WorkspaceMemberService, useValue: memberService },
        { provide: AdminUsageService, useValue: adminUsageService },
        { provide: MatSnackBar, useValue: snackBar },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // T-01 : chargement nominal — plan et membres affichés
  it('affiche le plan et les membres au chargement nominal', fakeAsync(async () => {
    await setup(of(mockWorkspace), of(mockMembers));

    expect(component.loading()).toBeFalse();
    expect(component.workspace()).toEqual(mockWorkspace);
    expect(component.members().length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('STARTER');
    expect(fixture.nativeElement.textContent).toContain('alice@test.com');
    expect(fixture.nativeElement.textContent).toContain('bob@test.com');
  }));

  // T-02 : 403 → accessDenied = true, message affiché
  it('affiche le message accès refusé si 403', fakeAsync(async () => {
    await setup(throwError(() => ({ status: 403 })), of(mockMembers));

    expect(component.accessDenied()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Accès réservé');
  }));

  // T-03 : erreur réseau → snackbar erreur
  it('affiche un snackbar si erreur réseau', fakeAsync(async () => {
    await setup(throwError(() => ({ status: 500 })), of(mockMembers));

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Erreur'), jasmine.any(String), jasmine.any(Object)
    );
    expect(component.accessDenied()).toBeFalse();
  }));

  // T-04 : plan FREE avec date d'expiration → date affichée
  it('affiche la date d\'expiration trial si plan FREE avec expiresAt', fakeAsync(async () => {
    await setup(of(mockWorkspaceFreeWithExpiry), of(mockMembers));

    expect(component.isTrial(mockWorkspaceFreeWithExpiry)).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Fin d\'essai');
  }));

  // T-05 : bouton "Voir le journal complet" présent, section journal absente
  it('affiche le bouton lien vers le journal et non le tableau', fakeAsync(async () => {
    await setup(of(mockWorkspace), of(mockMembers));

    expect(fixture.nativeElement.textContent).toContain('Voir le journal complet');
    expect(fixture.nativeElement.querySelector('table.audit-table')).toBeNull();
  }));
});
