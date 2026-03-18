import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { WorkspaceAdminComponent } from './workspace-admin.component';
import { AdminUsageService } from '../../core/services/admin-usage.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { WorkspaceUsageSummary } from '../../core/models/workspace-usage-summary.model';
import { provideRouter } from '@angular/router';

const mockSummary: WorkspaceUsageSummary = {
  totalTokensInput: 1500,
  totalTokensOutput: 600,
  totalCost: 0.0062,
  byUser: [{ userId: 'u1', userEmail: 'alice@test.com', tokensInput: 1500, tokensOutput: 600, totalCost: 0.0062 }],
  byCaseFile: [{ caseFileId: 'cf1', caseFileTitle: 'Dossier Licenciement', tokensInput: 1500, tokensOutput: 600, totalCost: 0.0062 }]
};

const emptySummary: WorkspaceUsageSummary = {
  totalTokensInput: 0, totalTokensOutput: 0, totalCost: 0,
  byUser: [], byCaseFile: []
};

describe('WorkspaceAdminComponent', () => {
  let component: WorkspaceAdminComponent;
  let fixture: ComponentFixture<WorkspaceAdminComponent>;
  let adminUsageService: jasmine.SpyObj<AdminUsageService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  async function setup(serviceReturn: any) {
    adminUsageService = jasmine.createSpyObj('AdminUsageService', ['getSummary']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    adminUsageService.getSummary.and.returnValue(serviceReturn);

    await TestBed.configureTestingModule({
      imports: [WorkspaceAdminComponent, NoopAnimationsModule],
      providers: [
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

  // T-01 : affiche les totaux quand l'API retourne des données
  it('affiche les totaux avec des données', fakeAsync(async () => {
    await setup(of(mockSummary));
    const cards = fixture.nativeElement.querySelectorAll('.summary-card');
    expect(cards.length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('1,500');
  }));

  // T-02 : affiche les 2 tableaux avec les bonnes colonnes
  it('affiche les 2 tableaux mat-table', fakeAsync(async () => {
    await setup(of(mockSummary));
    const tables = fixture.nativeElement.querySelectorAll('table[mat-table]');
    expect(tables.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Dossier Licenciement');
    expect(fixture.nativeElement.textContent).toContain('alice@test.com');
  }));

  // T-03 : affiche le message accès réservé si 403
  it('affiche le message accès réservé si 403', fakeAsync(async () => {
    await setup(throwError(() => ({ status: 403 })));
    expect(component.accessDenied()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Accès réservé');
  }));

  // T-04 : affiche un snackbar d'erreur si 500
  it('affiche un snackbar si erreur 500', fakeAsync(async () => {
    await setup(throwError(() => ({ status: 500 })));
    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Erreur'), jasmine.any(String), jasmine.any(Object)
    );
  }));

  // T-05 : affiche état vide si listes vides
  it('affiche Aucune donnée si listes vides', fakeAsync(async () => {
    await setup(of(emptySummary));
    const noCells = fixture.nativeElement.querySelectorAll('.no-data');
    expect(noCells.length).toBe(2);
  }));
});
