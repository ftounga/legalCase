import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FauteInexcusableFrSectionComponent } from './faute-inexcusable-fr-section.component';
import {
  FauteInexcusableFrEvaluation,
  FauteInexcusableFrResponse,
} from '../../core/models/faute-inexcusable-fr.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('FauteInexcusableFrSectionComponent', () => {
  let component: FauteInexcusableFrSectionComponent;
  let fixture: ComponentFixture<FauteInexcusableFrSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/faute-inexcusable-employeur';

  /** Fixture IA complète — les 4 champs `fauteInexcusable*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    fauteInexcusableConscienceDanger: true,
    fauteInexcusableSignalementPrior: true,
    fauteInexcusableMesuresPrevention: false,
    fauteInexcusableTauxIpp: 30,
  };

  function response(
    verdict: FauteInexcusableFrEvaluation,
    majoration: number | null = null,
  ): FauteInexcusableFrResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire après recharge).
      conscienceDangerEmployeurEtablie: verdict === 'FAUTE_INEXCUSABLE_PROBABLE',
      signalementDangerPrior: true,
      mesuresPreventionPrises: verdict === 'FAUTE_INEXCUSABLE_PEU_PROBABLE',
      documentUniqueEvalue: true,
      formationSecuriteProdiguee: true,
      tauxIpp: 25,
      renteMensuelleEuros: 800,
      salaireMensuelBrutEuros: 3500,
      // Sorties calculées.
      evaluationFauteInexcusable: verdict,
      scoreFauteInexcusable: verdict === 'FAUTE_INEXCUSABLE_PROBABLE' ? 80
                            : verdict === 'FAUTE_INEXCUSABLE_POSSIBLE' ? 50
                            : 15,
      facteursFauteInexcusable: [
        {
          code: 'DT91_CONSCIENCE_DANGER',
          libelle: 'Conscience du danger',
          fondement: 'Cass. ass. plén. 24/06/2005',
          poids: 35,
          explication: 'Test explication.',
        },
      ],
      majorationRenteEstimeeEuros: majoration,
      alerteProcedurePolesSocial: 'Action devant le pôle social du TJ — non devant le CPH (...)',
      basesJuridiques: ['L. 4121-1 CT', 'L. 452-2 CSS'],
      messages: ['Analyse calculée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-24T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        FauteInexcusableFrSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FauteInexcusableFrSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Chargement (GET) et gate pays
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount BELGIQUE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('BELGIQUE → bannière info pays affichée', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="fie-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 4 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.conscienceDangerEmployeurEtablie()).toBe(true);
    expect(component.signalementDangerPrior()).toBe(true);
    expect(component.mesuresPreventionPrises()).toBe(false);
    expect(component.tauxIpp()).toBe(30);
    expect(component.provenanceConscienceDanger()).toBe('IA');
    expect(component.provenanceTauxIpp()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    // Valeurs par défaut conservées.
    expect(component.conscienceDangerEmployeurEtablie()).toBe(false);
    expect(component.tauxIpp()).toBe(0);
    expect(component.provenanceConscienceDanger()).toBeNull();
  });

  it('getPrefillCount = 4 sur fixture IA FR complète', () => {
    expect(FauteInexcusableFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(FauteInexcusableFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(FauteInexcusableFrSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Bannière alerte procédure pôle social — TOUJOURS visible (invariant AC2)
  // ---------------------------------------------------------------------------

  it('bannière procédure pôle social visible sans condition (FRANCE)', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="fie-procedure-banner"]');
    expect(banner).toBeTruthy();
    expect(banner.textContent).toContain('pôle social');
    expect(banner.textContent).toContain('non devant le CPH');
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + majoration rente
  // ---------------------------------------------------------------------------

  it('POST → verdict PROBABLE + refresh dashboard', () => {
    component.conscienceDangerEmployeurEtablie.set(true);
    component.mesuresPreventionPrises.set(false);
    component.tauxIpp.set(25);
    component.salaireMensuelBrutEuros.set(3500);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.conscienceDangerEmployeurEtablie).toBe(true);
    req.flush(response('FAUTE_INEXCUSABLE_PROBABLE', 200));

    expect(component.result()?.evaluationFauteInexcusable).toBe('FAUTE_INEXCUSABLE_PROBABLE');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict POSSIBLE', () => {
    component.tauxIpp.set(10);
    component.salaireMensuelBrutEuros.set(3500);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('FAUTE_INEXCUSABLE_POSSIBLE'));
    expect(component.result()?.evaluationFauteInexcusable).toBe('FAUTE_INEXCUSABLE_POSSIBLE');
  });

  it('POST → verdict PEU_PROBABLE — pas de majoration affichée', () => {
    component.tauxIpp.set(10);
    component.salaireMensuelBrutEuros.set(3500);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('FAUTE_INEXCUSABLE_PEU_PROBABLE', null));
    expect(component.result()?.evaluationFauteInexcusable).toBe('FAUTE_INEXCUSABLE_PEU_PROBABLE');
    expect(component.result()?.majorationRenteEstimeeEuros).toBeNull();
  });

  it('Majoration rente affichée si calculée par backend', () => {
    component.collapsed.set(false);
    fixture.detectChanges(); // ngOnInit déclenche un GET
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.tauxIpp.set(50);
    component.salaireMensuelBrutEuros.set(3500);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('FAUTE_INEXCUSABLE_PROBABLE', 400));
    fixture.detectChanges();

    expect(component.result()?.majorationRenteEstimeeEuros).toBe(400);
    const majorationBlock = fixture.nativeElement.querySelector('[data-testid="fie-figure-majoration"]');
    expect(majorationBlock).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si IPP hors plage [0,100]', () => {
    component.tauxIpp.set(150);
    component.salaireMensuelBrutEuros.set(3500);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si salaire négatif', () => {
    component.tauxIpp.set(20);
    component.salaireMensuelBrutEuros.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true avec valeurs valides', () => {
    component.tauxIpp.set(20);
    component.salaireMensuelBrutEuros.set(3500);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('FAUTE_INEXCUSABLE_PROBABLE')).toContain('--probable');
    expect(component.verdictBannerClass('FAUTE_INEXCUSABLE_POSSIBLE')).toContain('--possible');
    expect(component.verdictBannerClass('FAUTE_INEXCUSABLE_PEU_PROBABLE')).toContain('--peu-probable');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('FAUTE_INEXCUSABLE_PROBABLE')).toBe('Faute inexcusable probable');
    expect(component.verdictBannerLabel('FAUTE_INEXCUSABLE_POSSIBLE')).toBe('Faute inexcusable possible');
    expect(component.verdictBannerLabel('FAUTE_INEXCUSABLE_PEU_PROBABLE')).toBe('Faute inexcusable peu probable');
  });

  it('formatEuros formate selon la locale FR', () => {
    expect(component.formatEuros(400)).toContain('400');
    expect(component.formatEuros(null)).toBe('—');
  });

  it('Handler IPP clamp les valeurs > 100 à 100', () => {
    component.onTauxIppChange(150);
    expect(component.tauxIpp()).toBe(100);
  });

  it('Handler IPP rejette les valeurs négatives (ramène à 0)', () => {
    component.onTauxIppChange(-5);
    expect(component.tauxIpp()).toBe(0);
  });
});
