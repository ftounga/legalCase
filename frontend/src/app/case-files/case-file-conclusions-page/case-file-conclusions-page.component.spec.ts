import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CaseFileConclusionsPageComponent } from './case-file-conclusions-page.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseFile } from '../../core/models/case-file.model';

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
          useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } },
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
});
