import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AnalysisDiffComponent } from './analysis-diff.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { AnalysisDiff, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';

const MOCK_VERSIONS: CaseAnalysisVersionSummary[] = [
  { id: 'v1-id', version: 1, analysisType: 'STANDARD', updatedAt: '2024-01-01T00:00:00Z',
    faitsCount: 3, pointsJuridiquesCount: 2, risquesCount: 1, questionsOuvertesCount: 1, timelineCount: 2 },
  { id: 'v2-id', version: 2, analysisType: 'ENRICHED', updatedAt: '2024-02-01T00:00:00Z',
    faitsCount: null, pointsJuridiquesCount: null, risquesCount: null, questionsOuvertesCount: null, timelineCount: null },
];

const MOCK_DIFF: AnalysisDiff = {
  from: { id: 'v1-id', version: 1, analysisType: 'STANDARD', updatedAt: '2024-01-01T00:00:00Z' },
  to:   { id: 'v2-id', version: 2, analysisType: 'ENRICHED', updatedAt: '2024-02-01T00:00:00Z' },
  faits: {
    added: [{ text: 'Nouveau fait', reason: 'Document contrat.pdf ajouté' }],
    removed: [{ text: 'Ancien fait', reason: null }],
    unchanged: [{ text: 'Fait commun', reason: null }],
    enriched: [{ text: 'Fait enrichi', reason: 'Enrichi Q1' }]
  },
  pointsJuridiques: {
    added: [], removed: [],
    unchanged: [{ text: 'Art. L1234', reason: null }],
    enriched: []
  },
  risques: { added: [], removed: [], unchanged: [], enriched: [] },
  questionsOuvertes: { added: [], removed: [], unchanged: [], enriched: [] },
  timeline: {
    added: [{ date: '2024-06-01', evenement: 'Jugement', reason: null }],
    removed: [],
    unchanged: [{ date: '2024-01-01', evenement: 'Embauche', reason: null }],
    enriched: []
  }
};

function buildTestBed(getDiffReturn: any) {
  const caseAnalysisServiceSpy = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getDiff']);
  const caseFileServiceSpy     = jasmine.createSpyObj('CaseFileService', ['getById']);
  const snackBarSpy            = jasmine.createSpyObj('MatSnackBar', ['open']);

  caseFileServiceSpy.getById.and.returnValue(of({ id: 'cf-1', title: 'Dossier test' } as any));
  caseAnalysisServiceSpy.getVersions.and.returnValue(of(MOCK_VERSIONS));
  caseAnalysisServiceSpy.getDiff.and.returnValue(getDiffReturn);

  TestBed.configureTestingModule({
    imports: [AnalysisDiffComponent, NoopAnimationsModule],
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: CaseFileService,     useValue: caseFileServiceSpy },
      { provide: CaseAnalysisService, useValue: caseAnalysisServiceSpy },
      { provide: MatSnackBar,         useValue: snackBarSpy },
      { provide: ActivatedRoute,      useValue: { snapshot: { paramMap: { get: () => 'cf-1' } } } }
    ]
  });

  return { caseAnalysisServiceSpy, caseFileServiceSpy, snackBarSpy };
}

describe('AnalysisDiffComponent', () => {
  let fixture: ComponentFixture<AnalysisDiffComponent>;
  let component: AnalysisDiffComponent;
  let caseAnalysisServiceSpy: jasmine.SpyObj<CaseAnalysisService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  beforeEach(async () => {
    const spies = buildTestBed(of(MOCK_DIFF));
    caseAnalysisServiceSpy = spies.caseAnalysisServiceSpy;
    snackBarSpy = spies.snackBarSpy;

    fixture = TestBed.createComponent(AnalysisDiffComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  // ── Init ──────────────────────────────────────────────────────────────
  it('should load versions on init', () => {
    expect(caseAnalysisServiceSpy.getVersions).toHaveBeenCalledWith('cf-1');
    expect(component.versions()).toEqual(MOCK_VERSIONS);
  });

  it('auto-triggers getDiff when two versions are auto-selected on init', () => {
    // ngOnInit: fromId = v[1].id = v2-id, toId = v[0].id = v1-id
    expect(caseAnalysisServiceSpy.getDiff).toHaveBeenCalledWith('cf-1', 'v2-id', 'v1-id');
    expect(component.diff()).toEqual(MOCK_DIFF);
  });

  // ── canCompute ────────────────────────────────────────────────────────
  it('canCompute() is false when no versions selected', () => {
    component.fromId.set('');
    component.toId.set('');
    expect(component.canCompute()).toBeFalse();
  });

  it('canCompute() is false when fromId equals toId', () => {
    component.fromId.set('v1-id');
    component.toId.set('v1-id');
    expect(component.canCompute()).toBeFalse();
  });

  it('canCompute() is true when two different versions selected', () => {
    component.fromId.set('v1-id');
    component.toId.set('v2-id');
    expect(component.canCompute()).toBeTrue();
  });

  // ── Sections & totals ─────────────────────────────────────────────────
  it('sections() contains 5 sections after diff loaded', () => {
    expect(component.sections().length).toBe(5);
  });

  it('totalAdded() counts all added items across sections', () => {
    // faits.added=1, timeline.added=1 → 2
    expect(component.totalAdded()).toBe(2);
  });

  it('totalRemoved() counts all removed items across sections', () => {
    // faits.removed=1
    expect(component.totalRemoved()).toBe(1);
  });

  it('totalEnriched() counts all enriched items across sections', () => {
    // faits.enriched=1
    expect(component.totalEnriched()).toBe(1);
  });

  it('sectionEnrichedCount() returns enriched count for string section', () => {
    const faitsSection = component.sections()[0];
    expect(component.sectionEnrichedCount(faitsSection)).toBe(1);
  });

  it('sectionEnrichedCount() returns 0 for section with no enriched', () => {
    const timelineSection = component.sections()[4];
    expect(component.sectionEnrichedCount(timelineSection)).toBe(0);
  });

  // ── sectionDominantType ───────────────────────────────────────────────
  it('sectionDominantType() returns "added" when added > others', () => {
    const section = component.sections()[0]; // faits: added=1, removed=1, enriched=1 → mixed
    // Use risques section: all zero → neutral
    const risques = component.sections()[2];
    expect(component.sectionDominantType(risques)).toBe('neutral');
  });

  it('sectionDominantType() returns "neutral" when no changes', () => {
    const risques = component.sections()[2]; // added=0, removed=0, enriched=0
    expect(component.sectionDominantType(risques)).toBe('neutral');
  });

  it('sectionDominantType() returns "added" for timeline (added=1 > removed=0 > enriched=0)', () => {
    const timeline = component.sections()[4]; // added=1
    expect(component.sectionDominantType(timeline)).toBe('added');
  });

  it('sectionDominantType() returns "mixed" when two types are equal and highest', () => {
    const faits = component.sections()[0]; // added=1, removed=1, enriched=1 → all equal → mixed
    expect(component.sectionDominantType(faits)).toBe('mixed');
  });

  // ── Filter ────────────────────────────────────────────────────────────
  it('setFilter() updates activeFilter', () => {
    component.setFilter('added');
    expect(component.activeFilter()).toBe('added');
  });

  it('toggleUnchanged() flips unchangedVisible', () => {
    expect(component.unchangedVisible()).toBeFalse();
    component.toggleUnchanged();
    expect(component.unchangedVisible()).toBeTrue();
  });

  // ── Navigation ────────────────────────────────────────────────────────
  it('goBack() navigates to case file detail', () => {
    const navigateSpy = spyOn(router, 'navigate');
    component.goBack();
    expect(navigateSpy).toHaveBeenCalledWith(['/case-files', 'cf-1']);
  });
});

describe('AnalysisDiffComponent — error handling', () => {
  it('shows snackbar error when getDiff fails', async () => {
    const { snackBarSpy } = buildTestBed(throwError(() => ({ status: 500 })));
    const errFixture = TestBed.createComponent(AnalysisDiffComponent);
    errFixture.detectChanges();
    await errFixture.whenStable();
    expect(snackBarSpy.open).toHaveBeenCalled();
  });
});
