import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from '../core/services/dashboard.service';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DashboardSummary } from '../core/models/dashboard.model';

const emptySummary: DashboardSummary = {
  openCases: [],
  openCasesCount: 0,
  urgentDeadlines: [],
  staleChecks: [],
  recentAnalyses: [],
};

const fullSummary: DashboardSummary = {
  openCases: [
    { id: 'cf1', title: 'Dossier Martin', legalDomain: 'DROIT_DU_TRAVAIL', status: 'ACTIVE' }
  ],
  openCasesCount: 3,
  urgentDeadlines: [
    { id: 'd1', label: 'Recours prudhomme', dueDate: new Date(Date.now() + 2 * 86400000).toISOString().split('T')[0], caseFileId: 'cf1', caseFileTitle: 'Dossier Martin' },
    { id: 'd2', label: 'Délai réponse', dueDate: new Date(Date.now() + 6 * 86400000).toISOString().split('T')[0], caseFileId: 'cf1', caseFileTitle: 'Dossier Martin' },
  ],
  staleChecks: [
    { caseFileId: 'cf1', caseFileTitle: 'Dossier Martin', nonCompliantCount: 2 }
  ],
  recentAnalyses: [
    { id: 'a1', caseFileId: 'cf1', caseFileTitle: 'Dossier Martin', analysisType: 'STANDARD', createdAt: new Date().toISOString() }
  ],
};

function buildTestBed(summary: DashboardSummary | null, shouldError = false) {
  const dashboardServiceSpy = {
    getSummary: jest.fn().mockReturnValue(shouldError ? throwError(() => new Error('error')) : of(summary!))
  };

  return TestBed.configureTestingModule({
    imports: [DashboardComponent],
    providers: [
      provideAnimationsAsync(),
      provideRouter([]),
      { provide: DashboardService, useValue: dashboardServiceSpy },
    ]
  });
}

describe('DashboardComponent — chargement', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;

  // DASH-UI-01 : spinner visible pendant le chargement
  it('DASH-UI-01: affiche le spinner pendant le chargement', async () => {
    await buildTestBed(emptySummary).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    // Avant detectChanges : loading = true
    expect(component.loading()).toBe(true);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.loading()).toBe(false);
  });
});

describe('DashboardComponent — contenu', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;

  beforeEach(async () => {
    await buildTestBed(fullSummary).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  // DASH-UI-02 : dossiers ouverts affichés
  it('DASH-UI-02: affiche les dossiers ouverts avec le titre', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Dossier Martin');
  });

  // DASH-UI-03 : délai urgent rouge si ≤ 3j
  it('DASH-UI-03: badge J-2 a la classe urgent-critical', () => {
    const badge = fixture.nativeElement.querySelector('.urgent-critical');
    expect(badge).not.toBeNull();
  });

  // DASH-UI-04-inverse : délai à J+6 a la classe urgent-warn
  it('DASH-UI-03b: badge J-6 a la classe urgent-warn', () => {
    const warnBadge = fixture.nativeElement.querySelector('.urgent-warn');
    expect(warnBadge).not.toBeNull();
  });

  // DASH-UI-05 : activité récente affichée
  it('DASH-UI-05: affiche l\'activité récente', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Analyse Standard');
  });
});

describe('DashboardComponent — sections vides', () => {
  let fixture: ComponentFixture<DashboardComponent>;

  // DASH-UI-04 : message "Aucun délai urgent" si liste vide
  it('DASH-UI-04: affiche "Aucun délai urgent" si liste vide', async () => {
    await buildTestBed(emptySummary).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Aucun délai urgent');
  });
});
