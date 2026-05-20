import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SuccessionBeAcceptationRenonciationSectionComponent } from './succession-be-acceptation-renonciation-section.component';
import { SuccessionBeAcceptationRenonciationResponse } from '../../core/models/succession-be-acceptation-renonciation.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('SuccessionBeAcceptationRenonciationSectionComponent — SF-217-13', () => {
  let component: SuccessionBeAcceptationRenonciationSectionComponent;
  let fixture: ComponentFixture<SuccessionBeAcceptationRenonciationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/succession-be-acceptation-renonciation';

  function response(
    verdict: SuccessionBeAcceptationRenonciationResponse['verdict'],
  ): SuccessionBeAcceptationRenonciationResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse.
      dateDeces: '2026-02-01',
      qualiteHeritier: 'ENFANT',
      etatPatrimoineSuccessoral: 'DOUTEUX',
      actesAccomplis: ['AUCUN'],
      volonteClient: 'INDECIS',
      miseEnDemeureCreancier: false,
      dateMiseEnDemeureCreancier: null,
      commentaire: null,
      // Champs calculés.
      verdict,
      optionRecommandee: verdict === 'OPTION_RECOMMANDEE_RENONCIATION'
        ? 'RENONCER'
        : verdict === 'OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE'
          ? 'ACCEPTER_BENEFICE_INVENTAIRE'
          : 'ACCEPTER',
      dateLimiteOption: '2026-06-01',
      joursRestants: verdict === 'DELAI_CRITIQUE' ? 15 : 90,
      delaiStatut: verdict === 'DELAI_DEPASSE' ? 'DEPASSE'
        : verdict === 'DELAI_CRITIQUE' ? 'CRITIQUE' : 'OK',
      actionsConcretes: ['Faire dresser un inventaire devant notaire'],
      risques: verdict === 'ACCEPTATION_TACITE_PROBABLE' ? [
        {
          code: 'ACTE_HERITIER_VALANT_ACCEPTATION_TACITE',
          libelle: 'Acte d\'héritier déjà accompli (vente d\'un bien)',
          fondement: 'CC art. 779 ancien — à vérifier',
          severite: 'HIGH',
        },
      ] : [],
      basesJuridiques: ['CC art. 774+ nouveau (à vérifier)'],
      messages: ['Délai d\'option = 4 mois à compter du décès.'],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        SuccessionBeAcceptationRenonciationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SuccessionBeAcceptationRenonciationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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
    httpMock.expectOne(BASE_URL).flush(response('OPTION_LIBRE_DELAI_OK'));
    expect(component.result()!.verdict).toBe('OPTION_LIBRE_DELAI_OK');
    expect(component.showForm()).toBe(false);
  });

  it("GET 200 → formulaire ré-hydraté depuis le snapshot d'inputs", () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE'));
    expect(component.dateDeces()).toBe('2026-02-01');
    expect(component.qualiteHeritier()).toBe('ENFANT');
    expect(component.etatPatrimoineSuccessoral()).toBe('DOUTEUX');
    expect(component.volonteClient()).toBe('INDECIS');
    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.dateDeces()).toBe('2026-02-01');
  });

  // ---------------------------------------------------------------------------
  // Toggle actes
  // ---------------------------------------------------------------------------

  it("toggleActe ajoute / retire un acte de la liste", () => {
    expect(component.isActeChecked('VENTE_BIEN_SUCCESSION')).toBe(false);
    component.toggleActe('VENTE_BIEN_SUCCESSION', true);
    expect(component.isActeChecked('VENTE_BIEN_SUCCESSION')).toBe(true);
    component.toggleActe('VENTE_BIEN_SUCCESSION', false);
    expect(component.isActeChecked('VENTE_BIEN_SUCCESSION')).toBe(false);
  });

  it("onMiseEnDemeureChange(false) efface la date associée", () => {
    component.miseEnDemeureCreancier.set(true);
    component.dateMiseEnDemeureCreancier.set('2026-03-15');
    component.onMiseEnDemeureChange(false);
    expect(component.miseEnDemeureCreancier()).toBe(false);
    expect(component.dateMiseEnDemeureCreancier()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire
  // ---------------------------------------------------------------------------

  it('formValid faux sans champ requis', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.dateDeces.set('2026-02-01');
    component.qualiteHeritier.set('ENFANT');
    component.etatPatrimoineSuccessoral.set('SOLVABLE');
    component.volonteClient.set('ACCEPTER');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si tous les champs requis sont remplis', () => {
    component.dateDeces.set('2026-02-01');
    component.qualiteHeritier.set('ENFANT');
    component.etatPatrimoineSuccessoral.set('SOLVABLE');
    component.volonteClient.set('ACCEPTER');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si mise en demeure activée sans date', () => {
    component.dateDeces.set('2026-02-01');
    component.qualiteHeritier.set('ENFANT');
    component.etatPatrimoineSuccessoral.set('SOLVABLE');
    component.volonteClient.set('ACCEPTER');
    component.miseEnDemeureCreancier.set(true);
    expect(component.formValid()).toBe(false);
    component.dateMiseEnDemeureCreancier.set('2026-03-15');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dateDeces.set('2026-02-01');
    component.qualiteHeritier.set('ENFANT');
    component.etatPatrimoineSuccessoral.set('DOUTEUX');
    component.volonteClient.set('INDECIS');
    component.toggleActe('PRISE_POSSESSION_BIENS', true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateDeces).toBe('2026-02-01');
    expect(req.request.body.qualiteHeritier).toBe('ENFANT');
    expect(req.request.body.etatPatrimoineSuccessoral).toBe('DOUTEUX');
    expect(req.request.body.volonteClient).toBe('INDECIS');
    expect(req.request.body.actesAccomplis).toContain('PRISE_POSSESSION_BIENS');
    expect(req.request.body.miseEnDemeureCreancier).toBe(false);
    expect(req.request.body.dateMiseEnDemeureCreancier).toBeNull();
    req.flush(response('OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE'));

    expect(component.result()!.verdict).toBe('OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — date de mise en demeure transmise si activée', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateDeces.set('2026-02-01');
    component.qualiteHeritier.set('ENFANT');
    component.etatPatrimoineSuccessoral.set('SOLVABLE');
    component.volonteClient.set('ACCEPTER');
    component.miseEnDemeureCreancier.set(true);
    component.dateMiseEnDemeureCreancier.set('2026-03-15');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.miseEnDemeureCreancier).toBe(true);
    expect(req.request.body.dateMiseEnDemeureCreancier).toBe('2026-03-15');
    req.flush(response('DELAI_CRITIQUE'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateDeces.set('2026-02-01');
    component.qualiteHeritier.set('ENFANT');
    component.etatPatrimoineSuccessoral.set('SOLVABLE');
    component.volonteClient.set('ACCEPTER');
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
  // Verdicts — rouge pour ACCEPTATION_TACITE_PROBABLE / DELAI_CRITIQUE / DELAI_DEPASSE
  // ---------------------------------------------------------------------------

  it('verdict ACCEPTATION_TACITE_PROBABLE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('ACCEPTATION_TACITE_PROBABLE')).toContain('--danger');
    expect(component.verdictBannerIcon('ACCEPTATION_TACITE_PROBABLE')).toBe('error');
  });

  it('verdict DELAI_CRITIQUE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('DELAI_CRITIQUE')).toContain('--danger');
  });

  it('verdict DELAI_DEPASSE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('DELAI_DEPASSE')).toContain('--danger');
  });

  it('verdict OPTION_LIBRE_DELAI_OK → banner vert --ok (pas rouge)', () => {
    expect(component.verdictBannerClass('OPTION_LIBRE_DELAI_OK')).toContain('--ok');
    expect(component.verdictBannerClass('OPTION_LIBRE_DELAI_OK')).not.toContain('--danger');
  });

  it('verdict OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE → banner or --medium', () => {
    expect(component.verdictBannerClass('OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE')).toContain('--medium');
    expect(component.verdictBannerClass('OPTION_RECOMMANDEE_BENEFICE_INVENTAIRE')).not.toContain('--danger');
  });

  it('verdict QUALIFICATION_INCOMPLETE → banner navy --info', () => {
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).not.toContain('--danger');
  });

  // ---------------------------------------------------------------------------
  // Statut du délai — rouge pour CRITIQUE / DEPASSE
  // ---------------------------------------------------------------------------

  it('delaiStatut OK → classe --ok', () => {
    expect(component.delaiStatutClass('OK')).toContain('--ok');
  });

  it('delaiStatut CRITIQUE → classe --danger', () => {
    expect(component.delaiStatutClass('CRITIQUE')).toContain('--danger');
  });

  it('delaiStatut DEPASSE → classe --danger', () => {
    expect(component.delaiStatutClass('DEPASSE')).toContain('--danger');
  });

  // ---------------------------------------------------------------------------
  // Bannière FRANCE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="sba-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('droit belge');
  });

  // ---------------------------------------------------------------------------
  // PREFILL_COUNT_ALWAYS_ZERO = true
  // ---------------------------------------------------------------------------

  it('PREFILL_COUNT_ALWAYS_ZERO === true', () => {
    expect(SuccessionBeAcceptationRenonciationSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('getPrefillCount() retourne 0 sur input vide', () => {
    expect(SuccessionBeAcceptationRenonciationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() retourne 0 même avec aiData rempli (V1)', () => {
    expect(
      SuccessionBeAcceptationRenonciationSectionComponent.getPrefillCount({
        aiData: { dateMariageBeDetectee: '2015-06-20' },
      }),
    ).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts F-IA-03 — date du décès
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DATE_DECES présent si F-96 diverge', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date du décès',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_DECES', expectedValue: '2026-03-15',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateDeces.set('2026-02-01');
    const alert = component.coherenceAlerts().DATE_DECES;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
  });

  it('coherenceAlerts vide en standaloneMode', () => {
    component.standaloneMode = true;
    component.ngOnInit();
    const reqs = httpMock.match(BASE_URL);
    reqs.forEach(r => r.flush({}, { status: 404, statusText: 'Not Found' }));
    component.dateDeces.set('2026-02-01');
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date du décès',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_DECES', expectedValue: '2026-03-15',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateDeces.set('2026-02-01');
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
