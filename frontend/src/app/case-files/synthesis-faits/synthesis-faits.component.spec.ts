import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { SynthesisFaitsComponent } from './synthesis-faits.component';
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

const makeAnalysis = (version: number, faits: { texte: string; source?: string | null; extrait?: string | null }[]) => ({
  id: `analysis-${version}`,
  version,
  analysisType: 'STANDARD' as const,
  status: 'DONE',
  timeline: [],
  faits: faits.map(f => ({ texte: f.texte, source: f.source ?? null, extrait: f.extrait ?? null })),
  pointsJuridiques: [],
  risques: [],
  questionsOuvertes: [],
  piecesManquantes: [],
  riskLevel: null,
  riskScore: null,
  modelUsed: null,
  updatedAt: '2026-05-01T10:00:00Z',
});

describe('SynthesisFaitsComponent (F-162 SF-162-03)', () => {
  let fixture: ComponentFixture<SynthesisFaitsComponent>;
  let component: SynthesisFaitsComponent;
  let caseAnalysisService: jest.Mocked<CaseAnalysisService>;
  let queryParamGet: (k: string) => string | null;

  beforeEach(async () => {
    queryParamGet = () => null;
    caseAnalysisService = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getByVersion']) as any;

    const caseFileService = jasmine.createSpyObj('CaseFileService', ['getById']);
    caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'Dossier test' }));

    await TestBed.configureTestingModule({
      imports: [SynthesisFaitsComponent, NoopAnimationsModule, RouterTestingModule],
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

    fixture = TestBed.createComponent(SynthesisFaitsComponent);
    component = fixture.componentInstance;
  });

  // U-1 : chargement de la dernière version (versions[0]).
  it('U1: loads the most recent version on init', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(2), makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(2, [{ texte: 'fait' }])));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 2);
  });

  // U-2 : groupement par thème — préfixe avant ":" devient un groupe.
  it('U2: groups faits by theme detected before the colon', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [
      { texte: 'Procédure : convocation envoyée' },
      { texte: 'Procédure : entretien tenu' },
      { texte: 'Salaire : 2 500 € brut' },
    ])));
    fixture.detectChanges();

    const groups = component.groupedFaits();
    expect(groups.length).toBe(2);
    expect(groups.find(g => g.theme === 'Procédure')?.items.length).toBe(2);
    expect(groups.find(g => g.theme === 'Salaire')?.items.length).toBe(1);
  });

  // U-3 : faits sans préfixe → tous dans "Autres".
  it('U3: faits without prefix fall under "Autres"', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [
      { texte: 'le salarié a été embauché en 2020' },
      { texte: 'pas de préfixe ici non plus' },
    ])));
    fixture.detectChanges();

    const groups = component.groupedFaits();
    expect(groups.length).toBe(1);
    expect(groups[0].theme).toBe('Autres');
    expect(groups[0].items.length).toBe(2);
  });

  // U-4 : état vide rendu si liste de faits vide.
  it('U4: renders empty state when no faits', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [])));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun fait détecté');
  });

  // U-5 : query param `version=N` valide → version chargée.
  it('U5: respects the version query param when valid', () => {
    queryParamGet = (k) => (k === 'version' ? '1' : null);
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(2), makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [])));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 1);
  });

  // U-6 : detectTheme — heuristique pure, testée directement.
  it('U6: detectTheme handles edge cases', () => {
    expect(SynthesisFaitsComponent.detectTheme('Procédure : ...')).toBe('Procédure');
    expect(SynthesisFaitsComponent.detectTheme('Conditions de travail : ...')).toBe('Conditions de travail');
    expect(SynthesisFaitsComponent.detectTheme('le texte sans majuscule')).toBe('Autres');
    expect(SynthesisFaitsComponent.detectTheme(': commence par un colon')).toBe('Autres');
    expect(SynthesisFaitsComponent.detectTheme('Préfixe trop long supérieur à soixante caractères donc ignoré pour éviter les longues phrases : suite')).toBe('Autres');
  });
});
