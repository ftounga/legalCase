import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MotifGraveBeSectionComponent } from './motif-grave-be-section.component';
import { MotifGraveBeResponse } from '../../core/models/motif-grave-be.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('MotifGraveBeSectionComponent', () => {
  let component: MotifGraveBeSectionComponent;
  let fixture: ComponentFixture<MotifGraveBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/motif-grave-be';

  function response(overrides: Partial<MotifGraveBeResponse> = {}): MotifGraveBeResponse {
    return {
      caseFileId: 'case-1',
      dateConnaissanceFait: '2026-04-01',
      dateNotificationRupture: '2026-04-02',
      dateNotificationMotifs: '2026-04-03',
      anciennetteAnnees: 5,
      salaireMensuelReference: 3000,
      delaiRuptureJoursOuvrables: 1,
      delaiMotifsJoursOuvrables: 1,
      motifGraveProceduralementValide: true,
      indemnitePreavisSiInvalide: 0,
      indemniteManifestementDeraisonnableMin: 0,
      indemniteManifestementDeraisonnableMax: 0,
      formule: 'Délais respectés (1 j ouvrables ≤ 3 + 1 j ouvrables ≤ 3) — motif grave valable.',
      baseJuridique: 'Art. 35 Loi 03/07/1978 + Loi 26/12/2013 + CCT 109',
      messages: ['Le motif grave est procéduralement valable.'],
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
        MotifGraveBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(MotifGraveBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
  });

  it('FRANCE → isBelgium() false, aucun appel HTTP au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
    httpMock.expectNone((r) => r.url.endsWith('/source-explanations'));
  });

  it('charge l\'analyse existante si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.motifGraveProceduralementValide).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(component.dateConnaissanceFait()).toBe('2026-04-01');
    expect(component.dateNotificationRupture()).toBe('2026-04-02');
    expect(component.dateNotificationMotifs()).toBe('2026-04-03');
    expect(component.anciennetteAnnees()).toBe(5);
    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.provenanceDateRupture()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // formValid()
  // ============================================================

  it('formValid false si une date manque', () => {
    component.dateConnaissanceFait.set(null);
    component.dateNotificationRupture.set('2026-04-02');
    component.dateNotificationMotifs.set('2026-04-03');
    component.anciennetteAnnees.set(5);
    component.salaireMensuelReference.set(3000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateNotificationRupture < dateConnaissanceFait', () => {
    component.dateConnaissanceFait.set('2026-04-05');
    component.dateNotificationRupture.set('2026-04-02');
    component.dateNotificationMotifs.set('2026-04-07');
    component.anciennetteAnnees.set(5);
    component.salaireMensuelReference.set(3000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateNotificationMotifs < dateNotificationRupture', () => {
    component.dateConnaissanceFait.set('2026-04-01');
    component.dateNotificationRupture.set('2026-04-05');
    component.dateNotificationMotifs.set('2026-04-03');
    component.anciennetteAnnees.set(5);
    component.salaireMensuelReference.set(3000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si une date est dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateConnaissanceFait.set(futureIso);
    component.dateNotificationRupture.set(futureIso);
    component.dateNotificationMotifs.set(futureIso);
    component.anciennetteAnnees.set(5);
    component.salaireMensuelReference.set(3000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si ancienneté négative ou non-entier', () => {
    component.dateConnaissanceFait.set('2026-04-01');
    component.dateNotificationRupture.set('2026-04-02');
    component.dateNotificationMotifs.set('2026-04-03');
    component.salaireMensuelReference.set(3000);

    component.anciennetteAnnees.set(-1);
    expect(component.formValid()).toBe(false);

    component.anciennetteAnnees.set(2.5);
    expect(component.formValid()).toBe(false);

    component.anciennetteAnnees.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si salaire ≤ 0', () => {
    component.dateConnaissanceFait.set('2026-04-01');
    component.dateNotificationRupture.set('2026-04-02');
    component.dateNotificationMotifs.set('2026-04-03');
    component.anciennetteAnnees.set(5);

    component.salaireMensuelReference.set(0);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelReference.set(-100);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelReference.set(3000);
    expect(component.formValid()).toBe(true);
  });

  // ============================================================
  // calculate() / POST
  // ============================================================

  it('calculate() POST body valide → succès hydrate result + snackbar + refresh', () => {
    component.dateConnaissanceFait.set('2026-04-01');
    component.dateNotificationRupture.set('2026-04-02');
    component.dateNotificationMotifs.set('2026-04-03');
    component.anciennetteAnnees.set(5);
    component.salaireMensuelReference.set(3000);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateConnaissanceFait: '2026-04-01',
      dateNotificationRupture: '2026-04-02',
      dateNotificationMotifs: '2026-04-03',
      anciennetteAnnees: 5,
      salaireMensuelReference: 3000,
    });
    req.flush(response());

    expect(component.result()!.motifGraveProceduralementValide).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Motif grave BE analysé',
      'OK',
      jasmine.any(Object),
    );
  });

  it('calculate() erreur backend → snackbar rouge + calculating reset', () => {
    component.dateConnaissanceFait.set('2026-04-01');
    component.dateNotificationRupture.set('2026-04-02');
    component.dateNotificationMotifs.set('2026-04-03');
    component.anciennetteAnnees.set(5);
    component.salaireMensuelReference.set(3000);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP POST)', () => {
    component.dateConnaissanceFait.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ============================================================
  // Banner / labels (résultat valide vs invalide, pas de rouge)
  // ============================================================

  it('bannerClass valide=true → motif-grave-banner--info (navy)', () => {
    expect(component.bannerClass(true)).toContain('motif-grave-banner--info');
    expect(component.bannerClass(true)).not.toContain('danger');
  });

  it('bannerClass valide=false → motif-grave-banner--warning (or) — pas de rouge', () => {
    expect(component.bannerClass(false)).toContain('motif-grave-banner--warning');
    expect(component.bannerClass(false)).not.toContain('danger');
  });

  it('bannerIcon : valide=check_circle, invalide=warning', () => {
    expect(component.bannerIcon(true)).toBe('check_circle');
    expect(component.bannerIcon(false)).toBe('warning');
  });

  it('bannerTitle : libellés humains pour les 2 cas', () => {
    expect(component.bannerTitle(true)).toBe('Motif grave procéduralement valable');
    expect(component.bannerTitle(false)).toBe('Motif grave procéduralement invalide');
  });

  it('résultat invalide → fourchette CCT 109 remplie (min, max)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      delaiRuptureJoursOuvrables: 5,
      delaiMotifsJoursOuvrables: 2,
      motifGraveProceduralementValide: false,
      indemnitePreavisSiInvalide: 10381.06,
      indemniteManifestementDeraisonnableMin: 2078.52,
      indemniteManifestementDeraisonnableMax: 11778.29,
    }));
    flushSourceExplanations();
    expect(component.result()!.indemniteManifestementDeraisonnableMin).toBe(2078.52);
    expect(component.result()!.indemniteManifestementDeraisonnableMax).toBe(11778.29);
    expect(component.result()!.indemnitePreavisSiInvalide).toBe(10381.06);
  });

  // ============================================================
  // Pré-fill IA + handlers
  // ============================================================

  it('prefillFromAi : dateLicenciement → dateNotificationRupture + badge IA', () => {
    const ai: TravailExtractedData = { dateLicenciement: '2026-04-02' };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.dateNotificationRupture()).toBe('2026-04-02');
    expect(component.provenanceDateRupture()).toBe('IA');
  });

  it('prefillFromAi : salaireBrutMensuel → salaireMensuelReference + badge IA', () => {
    const ai: TravailExtractedData = { salaireBrutMensuel: 2500 };
    component.aiData = ai;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.salaireMensuelReference()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('prefillFromAi : aiData absent → aucun pré-fill, pas d\'erreur', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.dateNotificationRupture()).toBeNull();
    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceDateRupture()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onDateRuptureChange efface provenanceDateRupture', () => {
    component.provenanceDateRupture.set('IA');
    component.onDateRuptureChange('2026-04-10');
    expect(component.provenanceDateRupture()).toBeNull();
    expect(component.dateNotificationRupture()).toBe('2026-04-10');
  });

  it('onSalaireChange efface provenanceSalaire', () => {
    component.provenanceSalaire.set('IA');
    component.onSalaireChange(4000);
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.salaireMensuelReference()).toBe(4000);
  });

  // ============================================================
  // Coherence alerts (F-IA-03)
  // ============================================================

  it('coherenceAlerts : DATE_RUPTURE si aiData.dateLicenciement !== saisie', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = { dateLicenciement: '2026-04-02' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Avocat modifie la date pré-remplie
    component.onDateRuptureChange('2026-04-15');
    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_RUPTURE).toBeDefined();
    expect(alerts.DATE_RUPTURE!.expectedDisplay).toBe('2026-04-02');
    expect(alerts.DATE_RUPTURE!.severity).toBe('WARNING');
  });

  it('coherenceAlerts : pas d\'alerte DATE_RUPTURE si aiData.dateLicenciement === saisie', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = { dateLicenciement: '2026-04-02' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Pas de changement — prefill a mis 2026-04-02 et l'avocat n'a rien modifié.
    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_RUPTURE).toBeUndefined();
  });

  it('coherenceAlerts : SALAIRE si divergence > 10 % entre aiData et saisie', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // Avocat remplace la valeur pré-remplie par une valeur divergente.
    component.onSalaireChange(4000); // +33 %
    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts : pas d\'alerte SALAIRE si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(3100); // ~3 %
    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeUndefined();
  });

  it('coherenceAlerts retourne {} en mode résultat (showForm=false)', () => {
    component.aiData = { salaireBrutMensuel: 3000, dateLicenciement: '2026-04-02' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    component.onDateRuptureChange('2026-04-15');
    component.showForm.set(false); // mode résultat
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('alertsSummary compte le nombre d\'alertes + blockers (jamais CRITICAL)', () => {
    component.aiData = { salaireBrutMensuel: 3000, dateLicenciement: '2026-04-02' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onDateRuptureChange('2026-04-15');
    component.onSalaireChange(5000);
    const summary = component.alertsSummary();
    expect(summary.total).toBe(2);
    expect(summary.blockers).toBe(0); // WARNING pas CRITICAL → motif grave = qualification juridique
  });

  // ============================================================
  // Divers
  // ============================================================

  it('editMode() → showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('toggleCollapse() inverse l\'état collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('salaireEstDeduit reflète aiData.salaireEstDeduit', () => {
    component.aiData = { salaireEstDeduit: true };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.salaireEstDeduit()).toBe(true);
  });

  it('explanationFor renvoie un tableau (fallback [] si sourceKey absent)', () => {
    expect(component.explanationFor('SALAIRE')).toEqual([]);
    expect(component.explanationFor('DATE_RUPTURE')).toEqual([]);
  });
});
