import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SuccessionBeDevolutionReserveSectionComponent } from './succession-be-devolution-reserve-section.component';
import { SuccessionBeDevolutionReserveResponse } from '../../core/models/succession-be-devolution-reserve.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('SuccessionBeDevolutionReserveSectionComponent — SF-217-13', () => {
  let component: SuccessionBeDevolutionReserveSectionComponent;
  let fixture: ComponentFixture<SuccessionBeDevolutionReserveSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/succession-be-devolution-reserve';

  function response(
    verdict: SuccessionBeDevolutionReserveResponse['verdict'],
  ): SuccessionBeDevolutionReserveResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse (ré-édition du formulaire).
      dateDeces: '2024-06-01',
      etatCivilDefunt: 'MARIE',
      regimeMatrimonialDefunt: 'COMMUNAUTE_LEGALE',
      nombreEnfantsVivants: 2,
      nombreEnfantsPredecedesAvecDescendants: 0,
      presenceParentsVivants: false,
      presenceFreresSoeursOuDescendants: false,
      masseSuccessoraleEur: 400000,
      libertesConsentiesEur: verdict === 'QUOTITE_DEPASSEE' ? 250000 : 50000,
      commentaire: null,
      // Champs calculés.
      verdict,
      heritiers: [
        {
          code: 'ENFANT',
          libelle: 'Enfant n°1',
          quotePartReserveFraction: '1/4',
          quotePartReserveEur: 100000,
          quotePartGlobaleFraction: '1/2',
          quotePartGlobaleEur: 200000,
          fondement: 'CC art. 913 nouveau (à vérifier)',
        },
      ],
      reserveGlobaleEur: 200000,
      reserveGlobaleFraction: '1/2',
      quotiteDisponibleEur: 200000,
      quotiteDisponibleFraction: '1/2',
      depassementQuotiteEur: verdict === 'QUOTITE_DEPASSEE' ? 50000 : 0,
      basesJuridiques: ['CC art. 913 nouveau (à vérifier)'],
      messages: ['Réforme du 31/07/2017 applicable.'],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        SuccessionBeDevolutionReserveSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SuccessionBeDevolutionReserveSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Chargement (GET)
  // ---------------------------------------------------------------------------

  it('mount BELGIQUE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it("mount FRANCE → pas d'appel HTTP", () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('GET 200 → result rechargé + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('RESERVE_RESPECTEE'));
    expect(component.result()!.verdict).toBe('RESERVE_RESPECTEE');
    expect(component.showForm()).toBe(false);
  });

  it("GET 200 → formulaire ré-hydraté depuis le snapshot d'inputs", () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('RESERVE_RESPECTEE'));
    expect(component.dateDeces()).toBe('2024-06-01');
    expect(component.etatCivilDefunt()).toBe('MARIE');
    expect(component.regimeMatrimonialDefunt()).toBe('COMMUNAUTE_LEGALE');
    expect(component.nombreEnfantsVivants()).toBe(2);
    expect(component.masseSuccessoraleEur()).toBe(400000);
    component.editMode();
    expect(component.showForm()).toBe(true);
    // Champs conservés après bascule en édition.
    expect(component.dateDeces()).toBe('2024-06-01');
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire
  // ---------------------------------------------------------------------------

  it('formValid faux sans date du décès', () => {
    component.etatCivilDefunt.set('CELIBATAIRE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux sans état civil', () => {
    component.dateDeces.set('2024-06-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si MARIE sans régime matrimonial', () => {
    component.dateDeces.set('2024-06-01');
    component.etatCivilDefunt.set('MARIE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si MARIE avec régime matrimonial', () => {
    component.dateDeces.set('2024-06-01');
    component.etatCivilDefunt.set('MARIE');
    component.regimeMatrimonialDefunt.set('COMMUNAUTE_LEGALE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid vrai si non marié sans régime', () => {
    component.dateDeces.set('2024-06-01');
    component.etatCivilDefunt.set('CELIBATAIRE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.dateDeces.set('2024-06-01');
    component.etatCivilDefunt.set('CELIBATAIRE');
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateDeces.set('2024-06-01');
    component.etatCivilDefunt.set('MARIE');
    component.regimeMatrimonialDefunt.set('COMMUNAUTE_LEGALE');
    component.nombreEnfantsVivants.set(2);
    component.masseSuccessoraleEur.set(400000);
    component.libertesConsentiesEur.set(50000);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateDeces).toBe('2024-06-01');
    expect(req.request.body.etatCivilDefunt).toBe('MARIE');
    expect(req.request.body.regimeMatrimonialDefunt).toBe('COMMUNAUTE_LEGALE');
    expect(req.request.body.nombreEnfantsVivants).toBe(2);
    expect(req.request.body.masseSuccessoraleEur).toBe(400000);
    req.flush(response('RESERVE_RESPECTEE'));

    expect(component.result()!.verdict).toBe('RESERVE_RESPECTEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — régime null si non marié', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateDeces.set('2024-06-01');
    component.etatCivilDefunt.set('CELIBATAIRE');
    component.regimeMatrimonialDefunt.set('COMMUNAUTE_LEGALE'); // sera ignoré
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.regimeMatrimonialDefunt).toBeNull();
    req.flush(response('DEVOLUTION_ETABLIE'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateDeces.set('2024-06-01');
    component.etatCivilDefunt.set('CELIBATAIRE');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(component.showForm()).toBe(true);
  });

  it("calculate() no-op si formulaire invalide (pas d'appel POST)", () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Verdicts — rouge réservé à QUOTITE_DEPASSEE
  // ---------------------------------------------------------------------------

  it('verdict QUOTITE_DEPASSEE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('QUOTITE_DEPASSEE')).toContain('--danger');
    expect(component.verdictBannerIcon('QUOTITE_DEPASSEE')).toBe('error');
  });

  it('verdict RESERVE_RESPECTEE → banner vert --ok (pas rouge)', () => {
    expect(component.verdictBannerClass('RESERVE_RESPECTEE')).toContain('--ok');
    expect(component.verdictBannerClass('RESERVE_RESPECTEE')).not.toContain('--danger');
  });

  it('verdict DEVOLUTION_ETABLIE → banner navy --info (pas rouge)', () => {
    expect(component.verdictBannerClass('DEVOLUTION_ETABLIE')).toContain('--info');
    expect(component.verdictBannerClass('DEVOLUTION_ETABLIE')).not.toContain('--danger');
  });

  it('verdict QUALIFICATION_INCOMPLETE → banner navy --info (pas rouge)', () => {
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).not.toContain('--danger');
  });

  // ---------------------------------------------------------------------------
  // Bannière FRANCE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="sbd-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('droit belge');
  });

  // ---------------------------------------------------------------------------
  // PREFILL_COUNT_ALWAYS_ZERO = true
  // ---------------------------------------------------------------------------

  it('PREFILL_COUNT_ALWAYS_ZERO === true', () => {
    expect(SuccessionBeDevolutionReserveSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('getPrefillCount() retourne 0 sur input vide', () => {
    expect(SuccessionBeDevolutionReserveSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() retourne 0 même avec aiData rempli (V1)', () => {
    expect(
      SuccessionBeDevolutionReserveSectionComponent.getPrefillCount({
        aiData: { dateMariageBeDetectee: '2015-06-20' },
      }),
    ).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts F-IA-03 — date du décès
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DATE_DECES présent si F-96 diverge de la saisie', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date du décès',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_DECES', expectedValue: '2024-07-01',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateDeces.set('2024-06-01');
    const alert = component.coherenceAlerts().DATE_DECES;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
  });

  it('coherenceAlerts vide en standaloneMode', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date du décès',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_DECES', expectedValue: '2024-07-01',
    }];
    component.standaloneMode = true;
    component.procedureChecks = checks;
    component.ngOnInit();
    // Pas d'appel HTTP attendu si workspaceCountry est BELGIQUE en standalone ? Si load est appelé, on flush.
    const reqs = httpMock.match(BASE_URL);
    reqs.forEach(r => r.flush({}, { status: 404, statusText: 'Not Found' }));

    component.dateDeces.set('2024-06-01');
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date du décès',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_DECES', expectedValue: '2024-07-01',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateDeces.set('2024-06-01');
    expect(component.coherenceAlerts().DATE_DECES).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_DECES).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Toggle / editMode
  // ---------------------------------------------------------------------------

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode ré-affiche le form après calcul', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});
