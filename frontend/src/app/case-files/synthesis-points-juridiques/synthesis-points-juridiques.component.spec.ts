import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { SynthesisPointsJuridiquesComponent } from './synthesis-points-juridiques.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { JurisprudenceCitationService } from '../../core/services/jurisprudence-citation.service';

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
  let citationService: jest.Mocked<JurisprudenceCitationService>;
  let queryParamGet: (k: string) => string | null;

  beforeEach(async () => {
    queryParamGet = () => null;
    caseAnalysisService = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getByVersion']) as any;

    const caseFileService = jasmine.createSpyObj('CaseFileService', ['getById']);
    caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'Dossier test' }));

    const workspaceService = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace']);
    workspaceService.getCurrentWorkspace.mockReturnValue(of({ id: 'ws-1', country: 'FRANCE' }));

    citationService = jasmine.createSpyObj('JurisprudenceCitationService', [
      'list', 'create', 'update', 'delete',
    ]) as any;
    citationService.list.mockReturnValue(of({ citations: [] }));

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
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: JurisprudenceCitationService, useValue: citationService },
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

  // U-7 : F-241 SF-241-01 — chaque point juridique rend le composant deeplinks jurispru.
  it('U7: injects <app-jurisprudence-deeplinks> for each point card', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(
      of(makeAnalysis(1, [
        { texte: 'Licenciement pour faute grave non motivé' },
        { texte: 'Préavis non respecté par l\'employeur' },
      ])),
    );
    fixture.detectChanges();

    const deeplinkComponents = fixture.nativeElement.querySelectorAll('app-jurisprudence-deeplinks');
    expect(deeplinkComponents.length).toBe(2);
  });

  // U-8 : F-242 SF-242-02 — les citations sont chargées à l'init et réparties par index de point.
  it('U8: loads jurisprudence citations on init and groups them by point index', () => {
    citationService.list.mockReturnValue(
      of({
        citations: [
          {
            id: 'c1', pointJuridiqueIndex: 0, pointJuridiqueTexte: 'p0',
            reference: 'Réf 1', portee: null,
            createdAt: '2026-05-18T09:00:00Z', updatedAt: '2026-05-18T09:00:00Z',
          },
          {
            id: 'c2', pointJuridiqueIndex: 1, pointJuridiqueTexte: 'p1',
            reference: 'Réf 2', portee: 'portée',
            createdAt: '2026-05-18T09:00:00Z', updatedAt: '2026-05-18T09:00:00Z',
          },
          {
            id: 'c3', pointJuridiqueIndex: 0, pointJuridiqueTexte: 'p0',
            reference: 'Réf 3', portee: null,
            createdAt: '2026-05-18T09:00:00Z', updatedAt: '2026-05-18T09:00:00Z',
          },
        ],
      }),
    );
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(
      of(makeAnalysis(1, [{ texte: 'point A' }, { texte: 'point B' }])),
    );

    fixture.detectChanges();

    expect(citationService.list).toHaveBeenCalledWith(CASE_FILE_ID);
    expect(component.citationsForPoint(0).map(c => c.id)).toEqual(['c1', 'c3']);
    expect(component.citationsForPoint(1).map(c => c.id)).toEqual(['c2']);
    expect(component.citationsForPoint(2)).toEqual([]);
  });

  // U-9 : F-242 SF-242-02 — un point juridique rend la zone « Jurisprudence à l'appui ».
  it('U9: injects <app-jurisprudence-appui> for each point card', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(
      of(makeAnalysis(1, [{ texte: 'point A' }, { texte: 'point B' }])),
    );
    fixture.detectChanges();

    const appuiComponents = fixture.nativeElement.querySelectorAll('app-jurisprudence-appui');
    expect(appuiComponents.length).toBe(2);
  });

  // U-10 : F-242 SF-242-02 — un échec du chargement des citations laisse la map vide.
  it('U10: keeps citations empty when the citation list fails', () => {
    citationService.list.mockReturnValue(throwError(() => new Error('boom')));
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [{ texte: 'point' }])));

    fixture.detectChanges();

    expect(component.citationsForPoint(0)).toEqual([]);
  });
});
