import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { BelgianCohabitantUeBeSectionComponent } from './belgian-40bis-section.component';
import { Belgian40bisResponse } from '../../core/models/belgian-40bis.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('BelgianCohabitantUeBeSectionComponent', () => {
  let component: BelgianCohabitantUeBeSectionComponent;
  let fixture: ComponentFixture<BelgianCohabitantUeBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/belgian-40bis';

  function response(overrides: Partial<Belgian40bisResponse> = {}): Belgian40bisResponse {
    return {
      caseFileId: 'case-1',
      lienFamilial: 'CONJOINT',
      regroupantCitoyenUe: true,
      regroupantActiviteCategorie: 'TRAVAILLEUR',
      ressourcesSuffisantes: true,
      assuranceMaladieUe: true,
      logementSuffisant: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-04-10',
      country: 'BELGIQUE',
      lienValide: true,
      regroupantValide: true,
      ressourcesOk: true,
      assuranceOk: true,
      logementOk: true,
      pasMenace: true,
      scoreGlobal: 100,
      verdictProbabilite: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstruction: '2026-10-10',
      formule: 'Regroupement familial 40bis (citoyen UE) : probabilité ELEVEE (score 100/100)',
      baseJuridique: 'Loi 15/12/1980 art. 40bis + AR 08/10/1981 art. 52',
      messages: ['Carte F délivrée si demande acceptée.'],
      ...overrides,
    };
  }

  function flushSourceExplanations(): void {
    httpMock.match((r) => r.url.endsWith('/source-explanations')).forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        BelgianCohabitantUeBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(BelgianCohabitantUeBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ============================================================
  // mount + gate pays
  // ============================================================

  it('mount : composant créé, défaut workspaceCountry=BELGIQUE', () => {
    expect(component).toBeTruthy();
    expect(component.workspaceCountry).toBe('BELGIQUE');
    expect(component.isBelgium()).toBe(true);
  });

  it('gate FRANCE : aucun appel HTTP au ngOnInit, isBelgium()=false', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
    httpMock.expectNone((r) => r.url.endsWith('/source-explanations'));
  });

  it('gate BELGIQUE : GET déclenché au ngOnInit', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
  });

  it('charge l\'analyse existante si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.verdictProbabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(component.lienFamilial()).toBe('CONJOINT');
    expect(component.regroupantActiviteCategorie()).toBe('TRAVAILLEUR');
    expect(component.dateDepotDemande()).toBe('2026-04-10');
    // Persisted values = avocat — pas de badge IA.
    expect(component.provenanceDateDepot()).toBeNull();
    expect(component.provenanceRegroupantCitoyenUe()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // formValid()
  // ============================================================

  it('formValid false si lienFamilial absent', () => {
    component.lienFamilial.set(null);
    component.regroupantActiviteCategorie.set('TRAVAILLEUR');
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si regroupantActiviteCategorie absent', () => {
    component.lienFamilial.set('CONJOINT');
    component.regroupantActiviteCategorie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid true quand les 2 enums sont sélectionnés (booléens libres)', () => {
    component.lienFamilial.set('CONJOINT');
    component.regroupantActiviteCategorie.set('TRAVAILLEUR');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateDepotDemande dans le futur', () => {
    component.lienFamilial.set('CONJOINT');
    component.regroupantActiviteCategorie.set('TRAVAILLEUR');
    const future = new Date();
    future.setDate(future.getDate() + 10);
    component.dateDepotDemande.set(future.toISOString().slice(0, 10));
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // submit() / POST
  // ============================================================

  it('submit() POST body valide → succès hydrate result + snackbar + refresh', () => {
    const refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    (component as any).dashboardRefresh = refreshSpy;

    component.lienFamilial.set('CONJOINT');
    component.regroupantActiviteCategorie.set('TRAVAILLEUR');
    component.regroupantCitoyenUe.set(true);
    component.ressourcesSuffisantes.set(true);
    component.assuranceMaladieUe.set(true);
    component.logementSuffisant.set(true);
    component.menaceOrdrePublic.set(false);
    component.dateDepotDemande.set('2026-04-10');
    component.submit();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      lienFamilial: 'CONJOINT',
      regroupantCitoyenUe: true,
      regroupantActiviteCategorie: 'TRAVAILLEUR',
      ressourcesSuffisantes: true,
      assuranceMaladieUe: true,
      logementSuffisant: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-04-10',
    });
    req.flush(response());

    expect(component.result()!.scoreGlobal).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(component.submitting()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Regroupement 40bis analysé', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('submit() erreur backend → snackbar rouge + submitting reset', () => {
    component.lienFamilial.set('CONJOINT');
    component.regroupantActiviteCategorie.set('TRAVAILLEUR');
    component.submit();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String), 'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }));
    expect(component.submitting()).toBe(false);
  });

  it('submit() ignoré si form invalide (pas d\'appel HTTP POST)', () => {
    component.lienFamilial.set(null);
    component.submit();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ============================================================
  // Pré-fill IA + handlers
  // ============================================================

  it('prefillFromAi : aiData.dateDepotProcedure → dateDepotDemande + badge IA', () => {
    const ai: ImmigrationExtractedData = { dateDepotProcedure: '2026-03-15' };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.dateDepotDemande()).toBe('2026-03-15');
    expect(component.provenanceDateDepot()).toBe('IA');
  });

  it('prefillFromAi : aiData.nationaliteUe=true → regroupantCitoyenUe=true + badge IA', () => {
    const ai: ImmigrationExtractedData = { nationaliteUe: true };
    component.regroupantCitoyenUe.set(false); // valeur initiale différente
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.regroupantCitoyenUe()).toBe(true);
    expect(component.provenanceRegroupantCitoyenUe()).toBe('IA');
  });

  it('prefillFromAi : aiData absent → no-op gracieux', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.dateDepotDemande()).toBeNull();
    expect(component.provenanceDateDepot()).toBeNull();
    expect(component.provenanceRegroupantCitoyenUe()).toBeNull();
  });

  it('onDateDepotDemandeChange efface provenanceDateDepot', () => {
    component.provenanceDateDepot.set('IA');
    component.onDateDepotDemandeChange('2026-04-20');
    expect(component.provenanceDateDepot()).toBeNull();
    expect(component.dateDepotDemande()).toBe('2026-04-20');
  });

  it('onRegroupantCitoyenUeChange efface provenanceRegroupantCitoyenUe', () => {
    component.provenanceRegroupantCitoyenUe.set('IA');
    component.onRegroupantCitoyenUeChange(false);
    expect(component.provenanceRegroupantCitoyenUe()).toBeNull();
    expect(component.regroupantCitoyenUe()).toBe(false);
  });

  // ============================================================
  // Coherence alerts (F-IA-03)
  // ============================================================

  it('coherenceAlerts retourne {} en mode résultat (showForm=false)', () => {
    component.lienFamilial.set('CONJOINT');
    component.showForm.set(false);
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('coherenceAlerts : sans pièce manquante câblée → pas d\'alerte LIEN_FAMILIAL', () => {
    component.lienFamilial.set('CONJOINT');
    expect(component.coherenceAlerts().LIEN_FAMILIAL).toBeUndefined();
  });

  it('alertsSummary : 0 si pas de divergence détectée', () => {
    component.lienFamilial.set('CONJOINT');
    const summary = component.alertsSummary();
    expect(summary.total).toBe(0);
    expect(summary.blockers).toBe(0);
  });

  // ============================================================
  // Banner / labels
  // ============================================================

  it('bannerClass : ELEVEE → info, MOYENNE → warning, FAIBLE → danger', () => {
    expect(component.bannerClass('ELEVEE')).toContain('b40bis-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('b40bis-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('b40bis-banner--danger');
  });

  it('bannerIcon : ELEVEE=check_circle, MOYENNE=warning, FAIBLE=error', () => {
    expect(component.bannerIcon('ELEVEE')).toBe('check_circle');
    expect(component.bannerIcon('MOYENNE')).toBe('warning');
    expect(component.bannerIcon('FAIBLE')).toBe('error');
  });

  it('verdictLabel : libellés humains pour les 3 verdicts', () => {
    expect(component.verdictLabel('ELEVEE')).toBe('Probabilité élevée');
    expect(component.verdictLabel('MOYENNE')).toBe('Probabilité moyenne');
    expect(component.verdictLabel('FAIBLE')).toBe('Probabilité faible');
  });

  it('lienFamilialLabel : libellé pour CONJOINT', () => {
    expect(component.lienFamilialLabel('CONJOINT')).toBe('Conjoint');
    expect(component.lienFamilialLabel('PARTENAIRE_ENREGISTRE')).toContain('Partenaire');
    expect(component.lienFamilialLabel(null)).toBe('');
  });

  it('résultat FAIBLE → criteresNonRemplis présents', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response({
      verdictProbabilite: 'FAIBLE',
      scoreGlobal: 30,
      lienValide: false,
      regroupantValide: false,
      ressourcesOk: false,
      assuranceOk: false,
      logementOk: true,
      pasMenace: true,
      criteresNonRemplis: [
        'Lien familial non reconnu',
        'Le regroupant doit être citoyen UE',
        'Ressources suffisantes non démontrées',
        'Assurance maladie couvrant l\'UE non démontrée',
      ],
    }));
    flushSourceExplanations();
    expect(component.result()!.criteresNonRemplis.length).toBe(4);
    expect(component.result()!.verdictProbabilite).toBe('FAIBLE');
  });

  // ============================================================
  // Divers
  // ============================================================

  it('toggleCollapse() inverse l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode() → showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('explanationFor renvoie un tableau (fallback [] si sourceKey absent)', () => {
    expect(component.explanationFor('LIEN_FAMILIAL')).toEqual([]);
  });

  it('ngOnChanges aiData re-prefill si form encore vierge + aucun result', () => {
    // Premier ngOnInit sans aiData.
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.dateDepotDemande()).toBeNull();

    // L'analyse IA arrive après coup.
    const newAi: ImmigrationExtractedData = { dateDepotProcedure: '2026-02-01' };
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(undefined, newAi, false),
    });
    expect(component.dateDepotDemande()).toBe('2026-02-01');
    expect(component.provenanceDateDepot()).toBe('IA');
  });
});
