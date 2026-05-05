import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { SynthesisPointsJuridiquesComponent } from './synthesis-points-juridiques.component';
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

const makeAnalysis = (version: number, points: { texte: string; source?: string | null }[]) => ({
  id: `analysis-${version}`,
  version,
  analysisType: 'STANDARD' as const,
  status: 'DONE',
  timeline: [],
  faits: [],
  pointsJuridiques: points.map(p => ({ texte: p.texte, source: p.source ?? null, extrait: null })),
  risques: [],
  questionsOuvertes: [],
  piecesManquantes: [],
  riskLevel: null,
  riskScore: null,
  modelUsed: null,
  updatedAt: '2026-05-01T10:00:00Z',
});

describe('SynthesisPointsJuridiquesComponent (F-162 SF-162-04)', () => {
  let fixture: ComponentFixture<SynthesisPointsJuridiquesComponent>;
  let component: SynthesisPointsJuridiquesComponent;
  let caseAnalysisService: jest.Mocked<CaseAnalysisService>;
  let queryParamGet: (k: string) => string | null;

  beforeEach(async () => {
    queryParamGet = () => null;
    caseAnalysisService = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getByVersion']) as any;

    const caseFileService = jasmine.createSpyObj('CaseFileService', ['getById']);
    caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'Dossier test' }));

    await TestBed.configureTestingModule({
      imports: [SynthesisPointsJuridiquesComponent, NoopAnimationsModule, RouterTestingModule],
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

    fixture = TestBed.createComponent(SynthesisPointsJuridiquesComponent);
    component = fixture.componentInstance;
  });

  // U-1 : chargement de la dernière version (versions[0]).
  it('U1: loads the most recent version on init', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(2), makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(2, [{ texte: 'point' }])));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 2);
  });

  // U-2 : isLong > 200 chars
  it('U2: isLong returns true when text exceeds 200 chars', () => {
    expect(component.isLong('a'.repeat(201))).toBe(true);
    expect(component.isLong('a'.repeat(200))).toBe(false);
    expect(component.isLong('court')).toBe(false);
  });

  // U-3 : toggle ajoute / retire l'index dans expandedIds.
  it('U3: toggle adds and removes index from expandedIds', () => {
    component.toggle(0);
    expect(component.expandedIds().has(0)).toBe(true);

    component.toggle(0);
    expect(component.expandedIds().has(0)).toBe(false);

    component.toggle(1);
    component.toggle(2);
    expect(component.expandedIds().has(1)).toBe(true);
    expect(component.expandedIds().has(2)).toBe(true);
  });

  // U-4 : état vide rendu si liste vide.
  it('U4: renders empty state when no points', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [])));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun point juridique détecté');
  });

  // U-5 : query param `version=N` valide → version chargée.
  it('U5: respects the version query param when valid', () => {
    queryParamGet = (k) => (k === 'version' ? '1' : null);
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(2), makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [])));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 1);
  });

  // U-6 : preview tronque à 200 chars + ellipse.
  it('U6: preview truncates at 200 chars with ellipsis', () => {
    const long = 'a'.repeat(250);
    const preview = component.preview(long);
    expect(preview.endsWith('…')).toBe(true);
    expect(preview.length).toBeLessThanOrEqual(201);
    expect(component.preview('court')).toBe('court');
  });
});
