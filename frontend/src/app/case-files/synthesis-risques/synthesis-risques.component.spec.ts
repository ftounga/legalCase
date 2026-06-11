import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { SynthesisRisquesComponent } from './synthesis-risques.component';
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

const makeAnalysis = (
  version: number,
  risques: { texte: string }[],
  riskLevel: string | null = null,
  riskScore: number | null = null,
) => ({
  id: `analysis-${version}`,
  version,
  analysisType: 'STANDARD' as const,
  status: 'DONE',
  timeline: [],
  faits: [],
  pointsJuridiques: [],
  risques: risques.map(r => ({ texte: r.texte, source: null, extrait: null })),
  questionsOuvertes: [],
  piecesManquantes: [],
  riskLevel,
  riskScore,
  modelUsed: null,
  updatedAt: '2026-05-01T10:00:00Z',
});

describe('SynthesisRisquesComponent (F-162 SF-162-05)', () => {
  let fixture: ComponentFixture<SynthesisRisquesComponent>;
  let component: SynthesisRisquesComponent;
  let caseAnalysisService: jest.Mocked<CaseAnalysisService>;
  let queryParamGet: (k: string) => string | null;

  beforeEach(async () => {
    queryParamGet = () => null;
    caseAnalysisService = jasmine.createSpyObj('CaseAnalysisService', ['getVersions', 'getByVersion']) as any;

    const caseFileService = jasmine.createSpyObj('CaseFileService', ['getById']);
    caseFileService.getById.mockReturnValue(of({ id: CASE_FILE_ID, title: 'Dossier test' }));

    await TestBed.configureTestingModule({
      imports: [SynthesisRisquesComponent, NoopAnimationsModule, RouterTestingModule],
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

    fixture = TestBed.createComponent(SynthesisRisquesComponent);
    component = fixture.componentInstance;
  });

  // U-1 : chargement de la dernière version (versions[0]).
  it('U1: loads the most recent version on init', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(2), makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(2, [{ texte: 'r1' }], 'MOYEN', 50)));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 2);
  });

  // U-2 : gravityClass mapping
  it('U2: gravityClass returns the correct class per risk level', () => {
    expect(component.gravityClass('FAIBLE')).toBe('risque-card--faible');
    expect(component.gravityClass('MOYEN')).toBe('risque-card--moyen');
    expect(component.gravityClass('ELEVE')).toBe('risque-card--eleve');
    expect(component.gravityClass(null)).toBe('');
    expect(component.gravityClass(undefined)).toBe('');
    expect(component.gravityClass('UNKNOWN')).toBe('');
  });

  // U-3 : état vide rendu si liste vide.
  it('U3: renders empty state when no risques', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [])));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucun risque détecté');
  });

  // U-4 : query param `version=N` valide → version chargée.
  it('U4: respects the version query param when valid', () => {
    queryParamGet = (k) => (k === 'version' ? '1' : null);
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(2), makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [], 'FAIBLE', 20)));

    fixture.detectChanges();

    expect(caseAnalysisService.getByVersion).toHaveBeenCalledWith(CASE_FILE_ID, 1);
  });

  // U-5 : bandeau global rendu si riskLevel présent.
  it('U5: renders global risk banner when riskLevel is present', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [{ texte: 'r1' }], 'ELEVE', 80)));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Risque global');
    expect(fixture.nativeElement.textContent).toContain('Élevé');
    expect(fixture.nativeElement.textContent).toContain('80');
  });

  // F-270 : réserve de prudence affichée sous le niveau de risque (indicatif, pas un % de succès)
  it('F-270: affiche la réserve de prudence sous le niveau de risque', () => {
    caseAnalysisService.getVersions.mockReturnValue(of([makeVersion(1)]));
    caseAnalysisService.getByVersion.mockReturnValue(of(makeAnalysis(1, [{ texte: 'r1' }], 'ELEVE', 80)));
    fixture.detectChanges();

    const reserve = fixture.nativeElement.querySelector('[data-testid="risk-reserve"]');
    expect(reserve).toBeTruthy();
    // F-270-02 : durcissement — le niveau de risque n'est pas une chance de gagner.
    expect(reserve.textContent).toContain('pas');
    expect(reserve.textContent).toContain('probabilité de gagner');
  });

  // U-6 : riskLabel humanise l'enum
  it('U6: riskLabel humanises enum values', () => {
    expect(component.riskLabel('FAIBLE')).toBe('Faible');
    expect(component.riskLabel('MOYEN')).toBe('Moyen');
    expect(component.riskLabel('ELEVE')).toBe('Élevé');
    expect(component.riskLabel(null)).toBe('');
    expect(component.riskLabel('UNKNOWN')).toBe('UNKNOWN');
  });
});
