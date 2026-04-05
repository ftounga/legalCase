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
    expect(component.loading()).toBe(true);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.loading()).toBe(false);
  });
});

describe('DashboardComponent — KPI bar', () => {
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await buildTestBed(fullSummary).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  // DASH-UI-06 : barre KPI avec 4 cartes
  it('DASH-UI-06: affiche 4 cartes KPI', () => {
    const cards = fixture.nativeElement.querySelectorAll('.kpi-card');
    expect(cards.length).toBe(4);
  });

  // DASH-UI-07 : KPI délais urgents en rouge si > 0
  it('DASH-UI-07: kpi-card délais urgents a la classe kpi-alert-red si count > 0', () => {
    const red = fixture.nativeElement.querySelector('.kpi-alert-red');
    expect(red).not.toBeNull();
  });
});

describe('DashboardComponent — contenu', () => {
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    await buildTestBed(fullSummary).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  // DASH-UI-02 : dossiers ouverts affichés
  it('DASH-UI-02: affiche les dossiers ouverts avec le titre', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Dossier Martin');
  });

  // DASH-UI-08 : délai urgent rouge si ≤ 3j
  it('DASH-UI-08: carte délai J-2 a la classe deadline-critical', () => {
    const card = fixture.nativeElement.querySelector('.deadline-critical');
    expect(card).not.toBeNull();
  });

  // DASH-UI-03b : délai à J+6 a la classe deadline-warn
  it('DASH-UI-03b: carte délai J-6 a la classe deadline-warn', () => {
    const card = fixture.nativeElement.querySelector('.deadline-warn');
    expect(card).not.toBeNull();
  });

  // DASH-UI-09 : dossier a border-left coloré via --domain-color
  it('DASH-UI-09: carte dossier porte la variable CSS --domain-color', () => {
    const card = fixture.nativeElement.querySelector('.case-card') as HTMLElement;
    expect(card).not.toBeNull();
    expect(card.style.getPropertyValue('--domain-color')).toBeTruthy();
  });

  // DASH-UI-05 : activité récente affichée
  it('DASH-UI-05: affiche l\'activité récente', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Analyse Standard');
  });
});

describe('DashboardComponent — sections vides', () => {
  let fixture: ComponentFixture<DashboardComponent>;

  // DASH-UI-11 : empty state délais urgents
  it('DASH-UI-11: affiche empty state si aucun délai urgent', async () => {
    await buildTestBed(emptySummary).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Aucun délai dans les 7 prochains jours');
  });
});

describe('DashboardComponent — erreur', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;

  beforeEach(async () => {
    await buildTestBed(null, true).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('DASH-UI-04: affiche le bouton Réessayer en cas d\'erreur', () => {
    expect(component.error()).toBe(true);
    const btn = fixture.nativeElement.querySelector('.error-state button');
    expect(btn).not.toBeNull();
    expect(btn.textContent).toContain('Réessayer');
  });

  // DASH-UI-10 : retry() re-déclenche l'appel
  it('DASH-UI-10: retry() re-appelle getSummary et résout l\'erreur', async () => {
    const svc = TestBed.inject(DashboardService) as any;
    svc.getSummary.mockReturnValue(of(emptySummary));
    component.retry();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(svc.getSummary).toHaveBeenCalledTimes(2);
    expect(component.error()).toBe(false);
  });
});
