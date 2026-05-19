import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from '../core/services/dashboard.service';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DashboardSummary, DashboardActivityDay } from '../core/models/dashboard.model';

function week(counts: number[]): DashboardActivityDay[] {
  return counts.map((c, i) => ({
    date: new Date(Date.now() - (6 - i) * 86400000).toISOString().split('T')[0],
    analysesCount: c,
  }));
}

const emptySummary: DashboardSummary = {
  openCases: [],
  openCasesCount: 0,
  urgentDeadlines: [],
  staleChecks: [],
  recentAnalyses: [],
  userFirstName: null,
  casesOpenedThisWeek: 0,
  weeklyActivity: week([0, 0, 0, 0, 0, 0, 0]),
};

const fullSummary: DashboardSummary = {
  openCases: [
    { id: 'cf1', title: 'Dossier Martin', legalDomain: 'DROIT_DU_TRAVAIL', status: 'ACTIVE' },
  ],
  openCasesCount: 3,
  urgentDeadlines: [
    { id: 'd1', label: 'Recours prudhomme', dueDate: new Date(Date.now() + 2 * 86400000).toISOString().split('T')[0], caseFileId: 'cf1', caseFileTitle: 'Dossier Martin' },
    { id: 'd2', label: 'Délai réponse', dueDate: new Date(Date.now() + 6 * 86400000).toISOString().split('T')[0], caseFileId: 'cf1', caseFileTitle: 'Dossier Martin' },
  ],
  staleChecks: [
    { caseFileId: 'cf1', caseFileTitle: 'Dossier Martin', nonCompliantCount: 2 },
  ],
  recentAnalyses: [
    { id: 'a1', caseFileId: 'cf1', caseFileTitle: 'Dossier Martin', analysisType: 'STANDARD', createdAt: new Date().toISOString() },
  ],
  userFirstName: 'Marie',
  casesOpenedThisWeek: 2,
  weeklyActivity: week([0, 1, 0, 3, 2, 0, 4]),
};

function buildTestBed(summary: DashboardSummary | null, shouldError = false) {
  const dashboardServiceSpy = {
    getSummary: jest.fn().mockReturnValue(shouldError ? throwError(() => new Error('error')) : of(summary!)),
  };

  return TestBed.configureTestingModule({
    imports: [DashboardComponent],
    providers: [
      provideAnimationsAsync(),
      provideRouter([]),
      { provide: DashboardService, useValue: dashboardServiceSpy },
    ],
  });
}

describe('DashboardComponent — chargement', () => {
  // DASH-UI-01 : spinner visible pendant le chargement
  it('DASH-UI-01: affiche le spinner pendant le chargement', async () => {
    await buildTestBed(emptySummary).compileComponents();
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;
    expect(component.loading()).toBe(true);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(component.loading()).toBe(false);
  });
});

describe('DashboardComponent — hero (F-249)', () => {
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

  // DASH-UI-06 : le hero porte 4 compteurs KPI
  it('DASH-UI-06: affiche 4 compteurs KPI dans le hero', () => {
    const kpis = fixture.nativeElement.querySelectorAll('.hero .kpi');
    expect(kpis.length).toBe(4);
  });

  // DASH-UI-07 : KPI délais urgents en rouge si > 0
  it('DASH-UI-07: un KPI porte la classe kpi-alert-red si délais urgents > 0', () => {
    expect(fixture.nativeElement.querySelector('.kpi-alert-red')).not.toBeNull();
  });

  // DASH-F249-01 : accueil personnalisé avec le prénom
  it('DASH-F249-01: accueil personnalisé « Bonjour Maître {prénom} »', () => {
    expect(component.greeting()).toBe('Bonjour Maître Marie');
    expect(fixture.nativeElement.textContent).toContain('Bonjour Maître Marie');
  });

  // DASH-F249-02 : headline calculée — délai critique prioritaire
  it('DASH-F249-02: headline = niveau critical quand un délai est à ≤ 3 jours', () => {
    const head = component.headline();
    expect(head?.level).toBe('critical');
    expect(head?.text).toContain('1 critique');
    expect(head?.anchor).toBe('deadlines');
  });

  // DASH-F249-03 : sparkline calculé à partir de weeklyActivity
  it('DASH-F249-03: le sparkline totalise les analyses de la semaine', () => {
    expect(component.sparkline()?.total).toBe(10);
    expect(fixture.nativeElement.querySelector('.spark-line')).not.toBeNull();
  });

  // DASH-F249-04 : delta « dossiers cette semaine » affiché si > 0
  it('DASH-F249-04: affiche le delta « +N cette semaine »', () => {
    expect(fixture.nativeElement.textContent).toContain('+2 cette semaine');
  });

  // DASH-F249-05 : clic KPI déclenche scrollTo vers la section
  it('DASH-F249-05: cliquer un KPI appelle scrollTo avec la bonne ancre', () => {
    const spy = jest.spyOn(component, 'scrollTo');
    const kpi = fixture.nativeElement.querySelector('.hero .kpi') as HTMLButtonElement;
    kpi.click();
    expect(spy).toHaveBeenCalledWith('cases');
  });
});

describe('DashboardComponent — headline & accueil par état', () => {
  let component: DashboardComponent;

  beforeEach(async () => {
    await buildTestBed(emptySummary).compileComponents();
    component = TestBed.createComponent(DashboardComponent).componentInstance;
  });

  // DASH-F249-06 : prénom absent → accueil générique sans « Maître »
  it('DASH-F249-06: userFirstName null → « Bonjour » sans « Maître »', () => {
    component.summary.set(emptySummary);
    expect(component.greeting()).toBe('Bonjour');
  });

  // DASH-F249-07 : headline = ok quand tout est à jour
  it('DASH-F249-07: aucun délai ni alerte → headline niveau ok', () => {
    component.summary.set(emptySummary);
    const head = component.headline();
    expect(head?.level).toBe('ok');
    expect(head?.anchor).toBeNull();
  });

  // DASH-F249-08 : headline = warn (checklist) quand seules des alertes existent
  it('DASH-F249-08: alertes checklist sans délai → headline niveau warn → checks', () => {
    component.summary.set({ ...emptySummary, staleChecks: [{ caseFileId: 'c', caseFileTitle: 'X', nonCompliantCount: 1 }] });
    const head = component.headline();
    expect(head?.level).toBe('warn');
    expect(head?.anchor).toBe('checks');
  });

  // DASH-F249-09 : weeklyActivity tout à zéro → pas de sparkline cassé
  it('DASH-F249-09: semaine sans analyse → sparkline total 0', () => {
    component.summary.set(emptySummary);
    expect(component.sparkline()?.total).toBe(0);
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
    expect(fixture.nativeElement.textContent).toContain('Dossier Martin');
  });

  // DASH-UI-08 : délai urgent rouge si ≤ 3j
  it('DASH-UI-08: carte délai J-2 a la classe deadline-critical', () => {
    expect(fixture.nativeElement.querySelector('.deadline-critical')).not.toBeNull();
  });

  // DASH-UI-03b : délai à J+6 a la classe deadline-warn
  it('DASH-UI-03b: carte délai J-6 a la classe deadline-warn', () => {
    expect(fixture.nativeElement.querySelector('.deadline-warn')).not.toBeNull();
  });

  // DASH-UI-09 : dossier a border-left coloré via --domain-color
  it('DASH-UI-09: carte dossier porte la variable CSS --domain-color', () => {
    const card = fixture.nativeElement.querySelector('.case-card') as HTMLElement;
    expect(card).not.toBeNull();
    expect(card.style.getPropertyValue('--domain-color')).toBeTruthy();
  });

  // DASH-UI-05 : activité récente affichée
  it('DASH-UI-05: affiche l\'activité récente', () => {
    expect(fixture.nativeElement.textContent).toContain('Analyse Standard');
  });
});

describe('DashboardComponent — sections vides', () => {
  // DASH-UI-11 : empty state délais urgents
  it('DASH-UI-11: affiche empty state si aucun délai urgent', async () => {
    await buildTestBed(emptySummary).compileComponents();
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aucun délai dans les 7 prochains jours');
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
    const svc = TestBed.inject(DashboardService) as unknown as { getSummary: jest.Mock };
    svc.getSummary.mockReturnValue(of(emptySummary));
    component.retry();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(svc.getSummary).toHaveBeenCalledTimes(2);
    expect(component.error()).toBe(false);
  });
});
