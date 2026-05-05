import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { SynthesisTimelineComponent } from './synthesis-timeline.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';

const CASE_FILE_ID = 'cf-1';

const makeVersion = (version: number) => ({
  id: `analysis-${version}`,
  version,
  analysisType: 'STANDARD' as const,
  updatedAt: '2026-05-01T10:00:00Z',
  faitsCount: null,
  pointsJuridiquesCount: null,
  risquesCount: null,
  questionsOuvertesCount: null,
  timelineCount: null,
});

const makeAnalysis = (version: number, timeline: { date: string; evenement: string }[]) => ({
  id: `analysis-${version}`,
  version,
  analysisType: 'STANDARD' as const,
  status: 'DONE',
  timeline,
  faits: [],
  pointsJuridiques: [],
  risques: [],
  questionsOuvertes: [],
  piecesManquantes: [],
  riskLevel: null,
  riskScore: null,
  modelUsed: null,
  updatedAt: '2026-05-01T10:00:00Z',
});

describe('SynthesisTimelineComponent (F-162 SF-162-02)', () => {
  let fixture: ComponentFixture<SynthesisTimelineComponent>;
  let component: SynthesisTimelineComponent;
  let caseAnalysisService: jest.Mocked<CaseAnalysisService>;
  let queryParamGet: (k: string) => string | null;

  beforeEach(async () => {
    queryParamGet = () => null;
    caseAnalysisService = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getByVersion']) as any;

    const caseFileService = jasmine.createSpyObj('CaseFileService', ['getById']);
    caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'Dossier test' }));

    await TestBed.configureTestingModule({
      imports: [SynthesisTimelineComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => CASE_FILE_ID },
              queryParamMap: { get: (k: string) => queryParamGet(k) },
            },
          },
        },
        { provide: CaseFileService, useValue: caseFileService },
        { provide: CaseAnalysisService, useValue: caseAnalysisService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SynthesisTimelineComponent);
    component = fixture.componentInstance;
  });

  // U-1 : chargement de la dernière version (versions[0] retournée par le backend).
  it('U1: loads the most recent version on init', () => {
    const versions = [makeVersion(3), makeVersion(2), makeVersion(1)];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(3, [
      { date: '2026-01-01', evenement: 'evt1' },
    ])));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 3);
    expect(component.synthesis()?.version).toBe(3);
  });

  // U-2 : ordre des événements préservé (pas de re-tri côté composant).
  it('U2: preserves the order of timeline events as received', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    const timeline = [
      { date: '2026-03-15', evenement: 'B' },
      { date: '2026-01-10', evenement: 'A' },
      { date: '2026-05-20', evenement: 'C' },
    ];
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, timeline)));
    fixture.detectChanges();

    const dates = component.synthesis()?.timeline.map(e => e.date);
    expect(dates).toEqual(['2026-03-15', '2026-01-10', '2026-05-20']);
  });

  // U-3 : timeline vide → état vide rendu.
  it('U3: renders empty state when timeline is empty', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [])));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun événement chronologique détecté');
  });

  // U-4 : query param `version=N` valide → cette version est chargée.
  it('U4: respects the version query param when valid', () => {
    queryParamGet = (k) => (k === 'version' ? '2' : null);
    const versions = [makeVersion(3), makeVersion(2), makeVersion(1)];
    caseAnalysisService.getVersions.mockReturnValue(of(versions));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(2, [])));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 2);
  });

  // U-5 : aucune version DONE → loading false, état "rien à afficher" géré côté template.
  it('U5: stops loading when there is no DONE version', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([]));
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.synthesis()).toBeNull();
  });

  // U-6 : erreur backend sur getVersions → loading false, pas de crash.
  it('U6: stops loading on getVersions error', () => {
    caseAnalysisService.getVersions.mockReturnValue(throwError(() => new Error('boom')));
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
  });
});
