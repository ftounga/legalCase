import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CaseFileConclusionsPageComponent } from './case-file-conclusions-page.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseFile } from '../../core/models/case-file.model';
import { ContradictoireService } from '../../core/services/contradictoire.service';
import { ContradictoireRound, ContradictoireTimeline } from '../../core/models/contradictoire.model';

/**
 * F-267 / SF-267-01 — page dédiée « Projet de conclusions ».
 *
 * On vérifie le wrapper de page : lecture du `:id` de route, rendu de la
 * section autonome avec le bon `caseFileId`, en-tête (titre dossier + retour),
 * et la navigation `viewToolsRequested` → /case-files/:id?section=decision.
 *
 * La section `conclusions-section` est autonome et émet ses propres requêtes
 * HTTP au montage : on les absorbe via `HttpClientTestingModule` sans les
 * asserter (couvertes par son propre spec). On utilise un vrai Router
 * (`provideRouter`) car `RouterLink` en dépend, et on espionne `navigate`.
 */
describe('CaseFileConclusionsPageComponent (F-267 SF-267-01)', () => {
  let fixture: ComponentFixture<CaseFileConclusionsPageComponent>;
  let component: CaseFileConclusionsPageComponent;
  let getByIdSpy: jest.SpyInstance;
  let httpMock: HttpTestingController;
  let navigateSpy: jest.SpyInstance;

  const CASE_ID = 'cf-42';

  const mockCaseFile = {
    id: CASE_ID,
    title: 'Dossier Lemaire',
    legalDomain: 'DROIT_DU_TRAVAIL',
  } as unknown as CaseFile;

  function setup(
    getByIdReturn = of(mockCaseFile),
    id: string | null = CASE_ID,
    queryParams: Record<string, string> = {},
  ): void {
    // On garde le VRAI CaseFileService (la section conclusions autonome appelle
    // d'autres méthodes — `getDecisionToolsVisibility`… — que HttpClientTesting
    // absorbe) et on n'espionne QUE `getById` utilisé par la page.
    TestBed.configureTestingModule({
      imports: [CaseFileConclusionsPageComponent, HttpClientTestingModule],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(id ? { id } : {}),
              // SF-282-04 — la page lit `?version=`/`?roundId=` au montage.
              queryParamMap: convertToParamMap(queryParams),
            },
          },
        },
      ],
    });

    const router = TestBed.inject(Router);
    navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    const caseFileService = TestBed.inject(CaseFileService);
    getByIdSpy = jest.spyOn(caseFileService, 'getById').mockReturnValue(getByIdReturn);

    fixture = TestBed.createComponent(CaseFileConclusionsPageComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => {
    // Absorbe les requêtes émises au montage par la section conclusions
    // autonome (état initial / versions / visibilité…) sans les asserter.
    if (httpMock) {
      // Le flush d'une requête peut en déclencher d'autres (versions → contenu) :
      // on draine en boucle jusqu'à ne plus avoir de requête ouverte.
      for (let i = 0; i < 10; i++) {
        const pending = httpMock.match(() => true);
        if (pending.length === 0) break;
        pending.forEach(r => r.flush([], { status: 200, statusText: 'OK' }));
      }
      httpMock.verify();
    }
  });

  it('lit le :id de route et le passe en caseFileId à la section conclusions', () => {
    setup();
    expect(component.caseFileId()).toBe(CASE_ID);
    const section = fixture.nativeElement.querySelector('app-conclusions-section');
    expect(section).not.toBeNull();
    expect(section.getAttribute('ng-reflect-case-file-id')).toBe(CASE_ID);
  });

  it('charge le dossier et affiche son titre dans l\'en-tête', () => {
    setup();
    expect(getByIdSpy).toHaveBeenCalledWith(CASE_ID);
    expect(component.caseFile()?.title).toBe('Dossier Lemaire');
    const subtitle = fixture.nativeElement.querySelector('.conclusions-page__subtitle');
    expect(subtitle.textContent).toContain('Dossier Lemaire');
  });

  it('le bouton « Retour au dossier » pointe vers /case-files/:id', () => {
    setup();
    const back = fixture.nativeElement.querySelector('[data-testid="back-to-case-file"]');
    expect(back).not.toBeNull();
    expect(back.getAttribute('href')).toBe(`/case-files/${CASE_ID}`);
  });

  it('rend la feuille centrée contenant la section conclusions', () => {
    setup();
    const sheet = fixture.nativeElement.querySelector('.conclusions-page__sheet');
    expect(sheet).not.toBeNull();
    expect(sheet.querySelector('app-conclusions-section')).not.toBeNull();
  });

  it('onViewToolsRequested navigue vers le dossier sur l\'onglet Décision', () => {
    setup();
    component.onViewToolsRequested();
    expect(navigateSpy).toHaveBeenCalledWith(
      ['/case-files', CASE_ID],
      { queryParams: { section: 'decision' } },
    );
  });

  it('fail-open : si le chargement du dossier échoue, la section reste rendue', () => {
    setup(throwError(() => new Error('404')));
    expect(component.loading()).toBe(false);
    expect(component.caseFile()).toBeNull();
    expect(fixture.nativeElement.querySelector('app-conclusions-section')).not.toBeNull();
  });

  // SF-282-04 (Part B) — auto-rattachement du round contradictoire à la version générée.
  describe('SF-282-04 — auto-rattachement (Part B)', () => {
    function roundFixture(over: Partial<ContradictoireRound> = {}): ContradictoireRound {
      return {
        id: 'r1',
        roundNumber: 3,
        party: 'OURS',
        label: null,
        datedAt: '2026-06-01',
        responseDueAt: null,
        sourceDocumentId: null,
        sourceConclusionId: null,
        sourceLabel: null,
        createdAt: '2026-06-01T00:00:00Z',
        updatedAt: '2026-06-01T00:00:00Z',
        ...over,
      };
    }

    function timelineWith(round: ContradictoireRound): ContradictoireTimeline {
      return {
        rounds: [round],
        summary: { currentRoundNumber: 3, awaitingParty: 'OURS', nextDeadline: null },
      };
    }

    it('rattache la version générée au round ciblé quand il est sans source', () => {
      setup(of(mockCaseFile), CASE_ID, { roundId: 'r1' });
      const svc = TestBed.inject(ContradictoireService);
      const timelineSpy = jest
        .spyOn(svc, 'timeline')
        .mockReturnValue(of(timelineWith(roundFixture())));
      const updateSpy = jest
        .spyOn(svc, 'update')
        .mockReturnValue(of(roundFixture({ sourceConclusionId: 'v-new' })));

      component.onGenerationCompleted('v-new');

      expect(timelineSpy).toHaveBeenCalledWith(CASE_ID);
      expect(updateSpy).toHaveBeenCalledWith(
        CASE_ID,
        'r1',
        expect.objectContaining({ sourceConclusionId: 'v-new', sourceDocumentId: null }),
      );
    });

    it('n\'écrase pas un round qui a déjà une source', () => {
      setup(of(mockCaseFile), CASE_ID, { roundId: 'r1' });
      const svc = TestBed.inject(ContradictoireService);
      jest
        .spyOn(svc, 'timeline')
        .mockReturnValue(of(timelineWith(roundFixture({ sourceConclusionId: 'v-old' }))));
      const updateSpy = jest.spyOn(svc, 'update');

      component.onGenerationCompleted('v-new');

      expect(updateSpy).not.toHaveBeenCalled();
    });

    it('ne fait rien sans roundId en query param', () => {
      setup(of(mockCaseFile), CASE_ID, {});
      const svc = TestBed.inject(ContradictoireService);
      const timelineSpy = jest.spyOn(svc, 'timeline');

      component.onGenerationCompleted('v-new');

      expect(timelineSpy).not.toHaveBeenCalled();
    });
  });
});
