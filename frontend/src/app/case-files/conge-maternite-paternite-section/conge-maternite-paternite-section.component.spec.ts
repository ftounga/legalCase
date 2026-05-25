import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CongeMaternitePaterniteSectionComponent } from './conge-maternite-paternite-section.component';
import { CongeMaternitePaterniteResponse } from '../../core/models/conge-maternite-paternite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('CongeMaternitePaterniteSectionComponent', () => {
  let component: CongeMaternitePaterniteSectionComponent;
  let fixture: ComponentFixture<CongeMaternitePaterniteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/conge-maternite-paternite';

  /** Fixture IA complète — les 5 champs du sous-objet. */
  const FULL_AI_DATA: TravailExtractedData = {
    congeMaternitePaterniteDetail: {
      congeMaternitePaterniteType: 'MATERNITE',
      congeMaterniteRangEnfant: 1,
      congeMaterniteNaissanceMultiple: false,
      congeMaterniteDateDebut: '2026-06-01',
      congeMaterniteSalaireMensuelBrut: 3000,
    },
  };

  function response(
    overrides: Partial<CongeMaternitePaterniteResponse> = {},
  ): CongeMaternitePaterniteResponse {
    return {
      caseFileId: 'case-1',
      typeConge: 'MATERNITE',
      rangEnfant: 1,
      naissanceMultiple: false,
      salaireMensuelBrutEuros: 3000,
      dateDebutConge: '2026-06-01',
      licenciementPendantConge: false,
      retourPosteDifferent: false,
      dureeCongeJours: 112,
      dateFinCongeTheorique: '2026-09-21',
      ijJournaliereEstimeeEuros: 78.85,
      ijTotaleEstimeeEuros: 8831.2,
      protectionLicenciementJusqua: '2026-11-30',
      alerteLicenciementIllegal: false,
      alerteRetourPosteIllegal: false,
      basesJuridiques: ['L. 1225-17 CT', 'L. 331-3 CSS', 'L. 1225-4 CT'],
      messages: ['Congé maternité — durée estimée 112 jours.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-25T10:00:00Z',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        CongeMaternitePaterniteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CongeMaternitePaterniteSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // F-JU-03 — absorbe les requêtes /jurisprudence-citations émises par
    // ToolJurisprudenceCitationsComponent (imports[] du composant).
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
    const banner = fixture.nativeElement.querySelector('[data-testid="cmp-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 5 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.typeConge()).toBe('MATERNITE');
    expect(component.rangEnfant()).toBe(1);
    expect(component.naissanceMultiple()).toBe(false);
    expect(component.dateDebutConge()).toBe('2026-06-01');
    expect(component.salaireMensuelBrutEuros()).toBe(3000);
    expect(component.provenanceType()).toBe('IA');
    expect(component.provenanceRang()).toBe('IA');
    expect(component.provenanceMultiple()).toBe('IA');
    expect(component.provenanceDate()).toBe('IA');
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.provenanceType()).toBeNull();
  });

  it('getPrefillCount = 5 sur fixture IA FR complète', () => {
    expect(CongeMaternitePaterniteSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(5);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(CongeMaternitePaterniteSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(CongeMaternitePaterniteSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('POST → maternité 1er enfant — durée 112 j + refresh dashboard', () => {
    component.dateDebutConge.set('2026-06-01');
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeConge).toBe('MATERNITE');
    req.flush(response({ dureeCongeJours: 112 }));

    expect(component.result()?.dureeCongeJours).toBe(112);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → maternité 3e enfant — durée 182 j', () => {
    component.dateDebutConge.set('2026-06-01');
    component.rangEnfant.set(3);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response({ dureeCongeJours: 182, rangEnfant: 3 }));
    expect(component.result()?.dureeCongeJours).toBe(182);
  });

  it('POST → paternité simple — durée 25 j', () => {
    component.dateDebutConge.set('2026-06-01');
    component.typeConge.set('PATERNITE');
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response({
      dureeCongeJours: 25, typeConge: 'PATERNITE',
    }));
    expect(component.result()?.dureeCongeJours).toBe(25);
  });

  // ── INVARIANT produit : alertes visuelles distinctes ─────────────────────

  it('Alerte licenciement illégal visible si flag true', () => {
    component.dateDebutConge.set('2026-06-01');
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response({ alerteLicenciementIllegal: true }));
    fixture.detectChanges();
    const alerte = fixture.nativeElement.querySelector('[data-testid="cmp-alert-licenciement"]');
    expect(alerte).toBeTruthy();
  });

  it('Alerte retour poste visible si flag true', () => {
    component.dateDebutConge.set('2026-06-01');
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response({ alerteRetourPosteIllegal: true }));
    fixture.detectChanges();
    const alerte = fixture.nativeElement.querySelector('[data-testid="cmp-alert-retour-poste"]');
    expect(alerte).toBeTruthy();
  });

  it('Alertes absentes si flags false', () => {
    component.dateDebutConge.set('2026-06-01');
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="cmp-alert-licenciement"]')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('[data-testid="cmp-alert-retour-poste"]')).toBeFalsy();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.dateDebutConge.set('2026-06-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false sans date', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true avec date + rang ≥ 1 + salaire ≥ 0', () => {
    component.dateDebutConge.set('2026-06-01');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers — provenance IA effacée au premier change
  // ---------------------------------------------------------------------------

  it('Handler type efface la provenance IA', () => {
    component.provenanceType.set('IA');
    component.onTypeChange('PATERNITE');
    expect(component.typeConge()).toBe('PATERNITE');
    expect(component.provenanceType()).toBeNull();
  });

  it('Handler rang efface la provenance IA', () => {
    component.provenanceRang.set('IA');
    component.onRangChange(3);
    expect(component.rangEnfant()).toBe(3);
    expect(component.provenanceRang()).toBeNull();
  });

  it('Handler date efface la provenance IA', () => {
    component.provenanceDate.set('IA');
    component.onDateChange('2026-07-01');
    expect(component.dateDebutConge()).toBe('2026-07-01');
    expect(component.provenanceDate()).toBeNull();
  });

  it('Handler salaire efface la provenance IA', () => {
    component.provenanceSalaire.set('IA');
    component.onSalaireChange(5000);
    expect(component.salaireMensuelBrutEuros()).toBe(5000);
    expect(component.provenanceSalaire()).toBeNull();
  });
});
