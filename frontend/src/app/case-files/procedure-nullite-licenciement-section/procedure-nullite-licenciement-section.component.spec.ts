import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProcedureNulliteLicenciementSectionComponent } from './procedure-nullite-licenciement-section.component';
import { ProcedureNulliteLicenciementResponse } from '../../core/models/procedure-nullite-licenciement.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('ProcedureNulliteLicenciementSectionComponent', () => {
  let component: ProcedureNulliteLicenciementSectionComponent;
  let fixture: ComponentFixture<ProcedureNulliteLicenciementSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/procedure-nullite-licenciement';

  function response(verdict: ProcedureNulliteLicenciementResponse['verdict'])
      : ProcedureNulliteLicenciementResponse {
    return {
      caseFileId: 'case-1',
      verdict,
      scoreNullite: verdict === 'NULLITE_AVEREE' ? 70 : verdict === 'NULLITE_PROBABLE' ? 45 : 0,
      vicesDetectes: verdict === 'PROCEDURE_REGULIERE' ? [] : [
        {
          code: 'MOTIVATION_INSUFFISANTE',
          libelle: 'Motivation de la lettre insuffisante',
          fondement: 'Art. L.1232-6 et L.1235-2 C. trav.',
          gravite: verdict === 'NULLITE_AVEREE' ? 'AVERE' : 'PROBABLE',
          explication: 'Une lettre insuffisamment motivée fragilise le licenciement.',
        },
      ],
      basesJuridiques: ['Art. L.1232-2 C. trav.', 'Art. L.1232-6 C. trav.'],
      messages: ['Vérifier la disponibilité de la lettre de convocation au dossier.'],
      calculatedAt: '2026-05-17T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        ProcedureNulliteLicenciementSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProcedureNulliteLicenciementSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Chargement (GET)
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

  it('GET 200 → result rechargé + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('NULLITE_PROBABLE'));
    expect(component.result()!.verdict).toBe('NULLITE_PROBABLE');
    expect(component.showForm()).toBe(false);
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme au contrat + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.convocationEnvoyee.set(true);
    component.dateConvocationPresentee.set('2025-01-06');
    component.entretienTenu.set(true);
    component.dateEntretienPrealable.set('2025-01-13');
    component.dateNotificationLicenciement.set('2025-01-17');
    component.lettreLicenciementEcrite.set(true);
    component.lettreMotivee.set(true);
    component.motivationSuffisante.set(false);
    component.motivationCommentaire.set('Motif vague');
    component.licenciementCollectif.set(false);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      convocationEnvoyee: true,
      dateConvocationPresentee: '2025-01-06',
      dateEntretienPrealable: '2025-01-13',
      entretienTenu: true,
      dateNotificationLicenciement: '2025-01-17',
      lettreLicenciementEcrite: true,
      lettreMotivee: true,
      motivationSuffisante: false,
      motivationCommentaire: 'Motif vague',
      licenciementPourMotifGrave: false,
      licenciementCollectif: false,
      procedureCseRespectee: null,
      conventionCollectiveApplicable: false,
      conventionCollectiveRespectee: true,
      conventionCollectiveCommentaire: null,
    });
    req.flush(response('NULLITE_PROBABLE'));

    expect(component.result()!.verdict).toBe('NULLITE_PROBABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse de nullité de procédure calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — procedureCseRespectee envoyé si licenciement collectif', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.licenciementCollectif.set(true);
    component.procedureCseRespectee.set(false);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.licenciementCollectif).toBe(true);
    expect(req.request.body.procedureCseRespectee).toBe(false);
    req.flush(response('NULLITE_PROBABLE'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

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

  // ---------------------------------------------------------------------------
  // Verdicts — 3 niveaux (rouge réservé à NULLITE_AVEREE)
  // ---------------------------------------------------------------------------

  it('verdict NULLITE_AVEREE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('NULLITE_AVEREE')).toContain('--danger');
    expect(component.verdictBannerLabel('NULLITE_AVEREE')).toContain('avérée');
    expect(component.verdictBannerIcon('NULLITE_AVEREE')).toBe('error');
  });

  it('verdict NULLITE_PROBABLE → banner or --medium', () => {
    expect(component.verdictBannerClass('NULLITE_PROBABLE')).toContain('--medium');
    expect(component.verdictBannerClass('NULLITE_PROBABLE')).not.toContain('--danger');
    expect(component.verdictBannerLabel('NULLITE_PROBABLE')).toContain('probable');
    expect(component.verdictBannerIcon('NULLITE_PROBABLE')).toBe('warning');
  });

  it('verdict PROCEDURE_REGULIERE → banner navy --available', () => {
    expect(component.verdictBannerClass('PROCEDURE_REGULIERE')).toContain('--available');
    expect(component.verdictBannerClass('PROCEDURE_REGULIERE')).not.toContain('--danger');
    expect(component.verdictBannerLabel('PROCEDURE_REGULIERE')).toContain('régulière');
    expect(component.verdictBannerIcon('PROCEDURE_REGULIERE')).toBe('check_circle');
  });

  it('GET 200 NULLITE_AVEREE → vice avéré rendu', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('NULLITE_AVEREE'));
    expect(component.result()!.verdict).toBe('NULLITE_AVEREE');
    expect(component.result()!.vicesDetectes[0].gravite).toBe('AVERE');
    expect(component.viceGraviteClass('AVERE')).toContain('--avere');
    expect(component.viceGraviteLabel('AVERE')).toBe('Avéré');
  });

  // ---------------------------------------------------------------------------
  // Bannière BELGIQUE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="pnl-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('France uniquement');
  });

  it('pas de bannière BE si workspace FRANCE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="pnl-country-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // getPrefillCount = 0 (PREFILL_COUNT_ALWAYS_ZERO)
  // ---------------------------------------------------------------------------

  it('getPrefillCount() retourne 0 (V1 — aucun flag procédural extrait)', () => {
    expect(ProcedureNulliteLicenciementSectionComponent.getPrefillCount({})).toBe(0);
    expect(ProcedureNulliteLicenciementSectionComponent.getPrefillCount({
      aiData: { salaireBrutMensuel: 2500 },
    })).toBe(0);
    expect(ProcedureNulliteLicenciementSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.ENTRETIEN_TENU présent si F-96 diverge de la saisie', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Entretien préalable',
      statut: 'NON_COMPLIANT', critereCode: 'DT36_ENTRETIEN_TENU', expectedValue: 'false',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // saisie avocat : entretien tenu = true ; F-96 attend false → alerte.
    component.entretienTenu.set(true);
    const alert = component.coherenceAlerts().ENTRETIEN_TENU;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
    expect(alert!.expectedDisplay).toBe('Entretien non tenu');
  });

  it('coherenceAlerts vide en standaloneMode', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Entretien préalable',
      statut: 'NON_COMPLIANT', critereCode: 'DT36_ENTRETIEN_TENU', expectedValue: 'false',
    }];
    component.standaloneMode = true;
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.entretienTenu.set(true);
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Entretien préalable',
      statut: 'NON_COMPLIANT', critereCode: 'DT36_ENTRETIEN_TENU', expectedValue: 'false',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.entretienTenu.set(true);
    expect(component.coherenceAlerts().ENTRETIEN_TENU).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().ENTRETIEN_TENU).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Toggle / editMode / handlers
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

  it('onEntretienTenuChange(false) reset dateEntretienPrealable', () => {
    component.entretienTenu.set(true);
    component.dateEntretienPrealable.set('2025-01-13');
    component.onEntretienTenuChange(false);
    expect(component.entretienTenu()).toBe(false);
    expect(component.dateEntretienPrealable()).toBeNull();
  });

  it('onLettreEcriteChange(false) reset motivation', () => {
    component.lettreMotivee.set(true);
    component.motivationSuffisante.set(true);
    component.onLettreEcriteChange(false);
    expect(component.lettreLicenciementEcrite()).toBe(false);
    expect(component.lettreMotivee()).toBe(false);
    expect(component.motivationSuffisante()).toBe(false);
  });

  it('onLicenciementCollectifChange(false) reset procedureCseRespectee', () => {
    component.procedureCseRespectee.set(false);
    component.onLicenciementCollectifChange(false);
    expect(component.licenciementCollectif()).toBe(false);
    expect(component.procedureCseRespectee()).toBe(true);
  });
});
