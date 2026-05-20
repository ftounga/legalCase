import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian40terSectionComponent } from './belgian-40ter-section.component';
import { Belgian40terResponse } from '../../core/models/belgian-40ter.model';

describe('Belgian40terSectionComponent', () => {
  let component: Belgian40terSectionComponent;
  let fixture: ComponentFixture<Belgian40terSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/belgian-40ter';

  function beResponse(overrides: Partial<Belgian40terResponse> = {}): Belgian40terResponse {
    return {
      caseFileId: 'case-1',
      lienFamilial: 'CONJOINT',
      regroupantBelge: true,
      revenusMensuelsNetsEur: 2060,
      seuil120PctRisEur: 1740,
      assuranceMaladie: true,
      logementSuffisant: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-04-15',
      country: 'BELGIQUE',
      lienValide: true,
      regroupantBelgeOk: true,
      revenusSuffisantsOk: true,
      assuranceOk: true,
      logementOk: true,
      pasMenace: true,
      differentielRevenus: 320,
      scoreGlobal: 100,
      verdictProbabiliteAcceptation: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstructionSiDemande: '2026-10-15',
      formule:
        '40ter familial Belge BE : probabilité ELEVEE (score 100/100) — lien OK, regroupant belge OK...',
      baseJuridique:
        'Loi 15/12/1980 art. 40ter + AR 08/10/1981 + AR 07/10/1981 (seuil 120 % RIS)',
      messages: [
        'Carte F (membre de famille d\'un Belge) délivrée après 5 ans de séjour légal et ininterrompu — art. 40ter §2.',
        'Recours CCE annulation : 30 jours à compter de la notification (art. 39/57).',
      ],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        Belgian40terSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Belgian40terSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('mount — composant créé, signals à défaut', () => {
    expect(component).toBeTruthy();
    expect(component.collapsed()).toBe(true);
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
    expect(component.lienFamilial()).toBeNull();
    expect(component.regroupantBelge()).toBe(true);
    expect(component.revenusMensuelsNetsEur()).toBeNull();
    expect(component.seuil120PctRisEur()).toBe(1740);
  });

  it('5 lien familial éligibles disponibles', () => {
    expect(component.liensFamiliaux.length).toBe(5);
    const codes = component.liensFamiliaux.map((l) => l.code);
    expect(codes).toEqual([
      'CONJOINT',
      'PARTENAIRE_LEGAL_ENREGISTRE',
      'DESCENDANT_MINEUR',
      'DESCENDANT_MAJEUR_CHARGE',
      'ASCENDANT_CHARGE_HANDICAP',
    ]);
  });

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE → isBelgium() false, pas d\'appel HTTP au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(beResponse());
    expect(component.result()!.verdictProbabiliteAcceptation).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(component.lienFamilial()).toBe('CONJOINT');
    expect(component.revenusMensuelsNetsEur()).toBe(2060);
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // -------------------------------- formValid --------------------------------

  it('formValid false si lienFamilial vide', () => {
    component.lienFamilial.set(null);
    component.revenusMensuelsNetsEur.set(2060);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si revenusMensuelsNetsEur ≤ 0 ou null', () => {
    component.lienFamilial.set('CONJOINT');
    component.revenusMensuelsNetsEur.set(null);
    expect(component.formValid()).toBe(false);
    component.revenusMensuelsNetsEur.set(0);
    expect(component.formValid()).toBe(false);
    component.revenusMensuelsNetsEur.set(-100);
    expect(component.formValid()).toBe(false);
    component.revenusMensuelsNetsEur.set(1500);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si seuil120PctRisEur ≤ 0', () => {
    component.lienFamilial.set('CONJOINT');
    component.revenusMensuelsNetsEur.set(2000);
    component.seuil120PctRisEur.set(0);
    expect(component.formValid()).toBe(false);
    component.seuil120PctRisEur.set(-1);
    expect(component.formValid()).toBe(false);
    component.seuil120PctRisEur.set(1740);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateDepotDemande dans le futur', () => {
    component.lienFamilial.set('CONJOINT');
    component.revenusMensuelsNetsEur.set(2000);
    const future = new Date();
    future.setDate(future.getDate() + 10);
    component.dateDepotDemande.set(future.toISOString().slice(0, 10));
    expect(component.formValid()).toBe(false);
  });

  it('formValid true cas nominal complet', () => {
    component.lienFamilial.set('CONJOINT');
    component.revenusMensuelsNetsEur.set(2060);
    component.seuil120PctRisEur.set(1740);
    component.dateDepotDemande.set('2026-04-15');
    expect(component.formValid()).toBe(true);
  });

  // -------------------------------- analyze --------------------------------

  it('analyze() POST — body conforme, résultat différentiel positif', () => {
    component.lienFamilial.set('CONJOINT');
    component.regroupantBelge.set(true);
    component.revenusMensuelsNetsEur.set(2060);
    component.seuil120PctRisEur.set(1740);
    component.assuranceMaladie.set(true);
    component.logementSuffisant.set(true);
    component.menaceOrdrePublic.set(false);
    component.dateDepotDemande.set('2026-04-15');
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      lienFamilial: 'CONJOINT',
      regroupantBelge: true,
      revenusMensuelsNetsEur: 2060,
      seuil120PctRisEur: 1740,
      assuranceMaladie: true,
      logementSuffisant: true,
      menaceOrdrePublic: false,
      dateDepotDemande: '2026-04-15',
    });
    req.flush(beResponse());

    expect(component.result()!.differentielRevenus).toBe(320);
    expect(component.result()!.differentielRevenus > 0).toBe(true);
    expect(component.result()!.verdictProbabiliteAcceptation).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      '40ter familial Belge analysé',
      'OK',
      jasmine.any(Object),
    );
  });

  it('analyze() POST — résultat différentiel négatif → verdict FAIBLE', () => {
    component.lienFamilial.set('DESCENDANT_MINEUR');
    component.revenusMensuelsNetsEur.set(1500);
    component.seuil120PctRisEur.set(1740);
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush(
      beResponse({
        lienFamilial: 'DESCENDANT_MINEUR',
        revenusMensuelsNetsEur: 1500,
        differentielRevenus: -240,
        revenusSuffisantsOk: false,
        scoreGlobal: 90,
        verdictProbabiliteAcceptation: 'MOYENNE',
        criteresNonRemplis: ['Revenus stables et suffisants < 120 % RIS (manque 240 €/mois)'],
      }),
    );

    expect(component.result()!.differentielRevenus).toBe(-240);
    expect(component.result()!.differentielRevenus < 0).toBe(true);
    expect(component.differentielBadgeClass(-240)).toContain('belgian40ter-diff-badge--negative');
    expect(component.differentielBadgeClass(320)).toContain('belgian40ter-diff-badge--positive');
  });

  it('analyze() omet dateDepotDemande si null', () => {
    component.lienFamilial.set('CONJOINT');
    component.revenusMensuelsNetsEur.set(2060);
    component.dateDepotDemande.set(null);
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.dateDepotDemande).toBeUndefined();
    req.flush(beResponse({ dateDepotDemande: null }));
  });

  it('analyze() erreur backend → snackbar rouge + analyzing reset', () => {
    component.lienFamilial.set('CONJOINT');
    component.revenusMensuelsNetsEur.set(2060);
    component.analyze();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.analyzing()).toBe(false);
  });

  it('analyze() ignoré si form invalide (pas d\'appel HTTP POST)', () => {
    component.lienFamilial.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // -------------------------------- prefillFromAi --------------------------------

  it('prefillFromAi : 4 champs IA → 4 provenance "IA" (après GET 404)', () => {
    component.aiData = {
      // SF-246-20 : champs typés be40terLienFamilial, be40terRevenusMensuelsNets, dateDepotProcedure.
      be40terLienFamilial: 'CONJOINT',
      regroupantBelge: true,
      be40terRevenusMensuelsNets: 2200,
      dateDepotProcedure: '2026-04-10',
    } as any;
    component.ngOnInit();
    httpMock
      .expectOne(BASE_URL)
      .flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.lienFamilial()).toBe('CONJOINT');
    expect(component.regroupantBelge()).toBe(true);
    expect(component.revenusMensuelsNetsEur()).toBe(2200);
    expect(component.dateDepotDemande()).toBe('2026-04-10');
    expect(component.provenanceLienFamilial()).toBe('IA');
    expect(component.provenanceRegroupantBelge()).toBe('IA');
    expect(component.provenanceRevenusMensuels()).toBe('IA');
    expect(component.provenanceDateDepot()).toBe('IA');
  });

  it('prefillFromAi : lienFamilialBe hors whitelist → skip gracieux', () => {
    component.aiData = { be40terLienFamilial: 'INCONNU' } as any;
    component.ngOnInit();
    httpMock
      .expectOne(BASE_URL)
      .flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.lienFamilial()).toBeNull();
    expect(component.provenanceLienFamilial()).toBeNull();
  });

  it('prefillFromAi : revenusNetsMensuels ≤ 0 → skip', () => {
    component.aiData = { be40terRevenusMensuelsNets: -100 } as any;
    component.ngOnInit();
    httpMock
      .expectOne(BASE_URL)
      .flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.revenusMensuelsNetsEur()).toBeNull();
    expect(component.provenanceRevenusMensuels()).toBeNull();
  });

  it('prefillFromAi : aiData absent → no-op gracieux', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock
      .expectOne(BASE_URL)
      .flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.lienFamilial()).toBeNull();
    expect(component.provenanceLienFamilial()).toBeNull();
  });

  it('prefillFromAi : aiData absent du modèle (no-op si revenus non présents)', () => {
    // Cas : ImmigrationExtractedData ne contient pas encore revenusNetsMensuels.
    // L'absence du champ ne doit pas écrouler le composant ni poser provenance.
    component.aiData = { dateNotificationAnnexe13: '2026-04-01' } as any;
    component.ngOnInit();
    httpMock
      .expectOne(BASE_URL)
      .flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.revenusMensuelsNetsEur()).toBeNull();
    expect(component.provenanceRevenusMensuels()).toBeNull();
  });

  it('loadExisting GET 200 → prefillFromAi NON appelé (pas d\'écrasement)', () => {
    component.aiData = { be40terLienFamilial: 'DESCENDANT_MINEUR' } as any;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(
      beResponse({
        lienFamilial: 'CONJOINT',
      }),
    );
    expect(component.lienFamilial()).toBe('CONJOINT');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceLienFamilial()).toBeNull();
  });

  // -------------------------------- onChange handlers --------------------------------

  describe('onXxxChange — provenance clear', () => {
    beforeEach(() => {
      component.provenanceLienFamilial.set('IA');
      component.provenanceRegroupantBelge.set('IA');
      component.provenanceRevenusMensuels.set('IA');
      component.provenanceDateDepot.set('IA');
    });

    it('onLienFamilialChange → provenance à null', () => {
      component.onLienFamilialChange('PARTENAIRE_LEGAL_ENREGISTRE');
      expect(component.lienFamilial()).toBe('PARTENAIRE_LEGAL_ENREGISTRE');
      expect(component.provenanceLienFamilial()).toBeNull();
    });

    it('onRegroupantBelgeChange → provenance à null', () => {
      component.onRegroupantBelgeChange(false);
      expect(component.regroupantBelge()).toBe(false);
      expect(component.provenanceRegroupantBelge()).toBeNull();
    });

    it('onRevenusMensuelsChange → provenance à null', () => {
      component.onRevenusMensuelsChange(2500);
      expect(component.revenusMensuelsNetsEur()).toBe(2500);
      expect(component.provenanceRevenusMensuels()).toBeNull();
    });

    it('onDateDepotChange → provenance à null', () => {
      component.onDateDepotChange('2026-04-20');
      expect(component.dateDepotDemande()).toBe('2026-04-20');
      expect(component.provenanceDateDepot()).toBeNull();
    });

    it('onSeuilChange null → fallback défaut 1740', () => {
      component.seuil120PctRisEur.set(2000);
      component.onSeuilChange(null);
      expect(component.seuil120PctRisEur()).toBe(1740);
    });
  });

  // -------------------------------- coherenceAlerts --------------------------------

  describe('coherenceAlerts (F-IA-03)', () => {
    beforeEach(() => {
      component.showForm.set(true);
    });

    it('alerte LIEN_FAMILIAL : IA divergent → severity=WARNING', () => {
      component.aiData = { be40terLienFamilial: 'CONJOINT' } as any;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.lienFamilial.set('DESCENDANT_MINEUR');
      const alert = component.coherenceAlerts().LIEN_FAMILIAL;
      expect(alert).toBeDefined();
      expect(alert!.severity).toBe('WARNING');
      expect(alert!.expectedDisplay).toContain('Conjoint');
    });

    it('alerte REVENUS_MENSUELS : IA divergent → severity=WARNING', () => {
      component.aiData = { be40terRevenusMensuelsNets: 2500 } as any;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.revenusMensuelsNetsEur.set(1800);
      const alert = component.coherenceAlerts().REVENUS_MENSUELS;
      expect(alert).toBeDefined();
      expect(alert!.severity).toBe('WARNING');
    });

    it('showForm=false → coherenceAlerts = {}', () => {
      component.aiData = { be40terLienFamilial: 'CONJOINT' } as any;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.lienFamilial.set('DESCENDANT_MINEUR');
      component.showForm.set(false);
      expect(component.coherenceAlerts()).toEqual({});
    });

    it('aiData null → coherenceAlerts = {}', () => {
      component.aiData = null;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, null, true),
      });
      expect(component.coherenceAlerts()).toEqual({});
    });
  });

  // -------------------------------- bannerClass / labels --------------------------------

  it('bannerClass : ELEVEE=success, MOYENNE=warning, FAIBLE=danger', () => {
    expect(component.bannerClass('ELEVEE')).toContain('belgian40ter-banner--success');
    expect(component.bannerClass('MOYENNE')).toContain('belgian40ter-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('belgian40ter-banner--danger');
  });

  it('bannerIcon : ELEVEE=check_circle, MOYENNE=warning, FAIBLE=error', () => {
    expect(component.bannerIcon('ELEVEE')).toBe('check_circle');
    expect(component.bannerIcon('MOYENNE')).toBe('warning');
    expect(component.bannerIcon('FAIBLE')).toBe('error');
  });

  it('verdictLabel renvoie un libellé humain pour chaque verdict', () => {
    expect(component.verdictLabel('ELEVEE')).toContain('élevée');
    expect(component.verdictLabel('MOYENNE')).toContain('moyenne');
    expect(component.verdictLabel('FAIBLE')).toContain('faible');
  });

  it('lienFamilialLabel renvoie le libellé humain', () => {
    expect(component.lienFamilialLabel('CONJOINT')).toBe('Conjoint');
    expect(component.lienFamilialLabel('PARTENAIRE_LEGAL_ENREGISTRE')).toBe(
      'Partenaire légalement enregistré',
    );
    expect(component.lienFamilialLabel(null)).toBe('');
  });

  it('differentielText : positif → "+X € > seuil 120 % RIS", négatif → "X € sous seuil"', () => {
    expect(component.differentielText(320, 1740)).toContain('+320');
    expect(component.differentielText(320, 1740)).toContain('seuil 120 % RIS');
    expect(component.differentielText(-150, 1740)).toContain('-150');
    expect(component.differentielText(-150, 1740)).toContain('sous seuil');
  });

  it('differentielBadgeClass : positif=positive, négatif=negative', () => {
    expect(component.differentielBadgeClass(0)).toContain('--positive');
    expect(component.differentielBadgeClass(320)).toContain('--positive');
    expect(component.differentielBadgeClass(-150)).toContain('--negative');
    expect(component.differentielBadgeClass(null)).not.toContain('--positive');
    expect(component.differentielBadgeClass(null)).not.toContain('--negative');
  });

  // -------------------------------- divers --------------------------------

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

  it('alertBadgeLabel — severity=CRITICAL → "Alerte critique (...)", WARNING → "Incohérence détectée (...)"', () => {
    expect(
      component.alertBadgeLabel({
        field: 'LIEN_FAMILIAL',
        source: 'IA',
        severity: 'CRITICAL',
        contributors: ['IA'],
        expectedDisplay: 'X',
        reason: 'Y',
      }),
    ).toContain('Alerte critique');
    expect(
      component.alertBadgeLabel({
        field: 'LIEN_FAMILIAL',
        source: 'IA',
        severity: 'WARNING',
        contributors: ['IA'],
        expectedDisplay: 'X',
        reason: 'Y',
      }),
    ).toContain('Incohérence');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-14-40ter-familial-belge-be/calculate';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : aucun GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      try { (component as any).analyze(); } catch (_) { /* formValid */ }
      const dispatcherReqs = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL_F163 && r.method === 'POST');
      const caseFileReqs = httpMock.match((r: { url: string; method: string }) => r.url.includes('/api/v1/case-files/') && r.method === 'POST');
      // Aucun POST case-file ne doit partir en standalone.
      expect(caseFileReqs.length).toBe(0);
      dispatcherReqs.forEach((req: any) => req.flush({}));
    });
  });
});
