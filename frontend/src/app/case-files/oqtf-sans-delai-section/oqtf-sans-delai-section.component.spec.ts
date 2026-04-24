import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { OqtfSansDelaiSectionComponent } from './oqtf-sans-delai-section.component';
import { OqtfSansDelaiResponse } from '../../core/models/oqtf-sans-delai.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('OqtfSansDelaiSectionComponent', () => {
  let component: OqtfSansDelaiSectionComponent;
  let fixture: ComponentFixture<OqtfSansDelaiSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/oqtf-sans-delai';

  function frResponse(overrides: Partial<OqtfSansDelaiResponse> = {}): OqtfSansDelaiResponse {
    return {
      caseFileId: 'case-1',
      dateHeureNotificationOqtf: '2026-04-23T10:00',
      motifSansDelai: 'RISQUE_FUITE',
      placementCra: false,
      recoursForme: false,
      dateHeureRecours: null,
      country: 'FRANCE',
      dateHeureExpirationDelaiRecours: '2026-04-25T10:00',
      heuresRestantes: 32,
      statutDelaiRecours: 'DISPONIBLE',
      dateHeureAudiencePrevisionnelle: null,
      dateDecisionPrevisionnelle: null,
      refereDisponibles: ['REFERE_LIBERTE_L521_2'],
      formule: 'Notification + 48 heures',
      baseJuridique: 'Art. L.614-6, L.614-7, L.521-2 CJA',
      messages: ['Urgence absolue : 48h pour former le recours'],
      ...overrides,
    };
  }

  function aiData(overrides: Partial<ImmigrationExtractedData> = {}): ImmigrationExtractedData {
    return {
      dateHeureNotificationOqtfSansDelai: null,
      placementCraDetected: null,
      motifOqtfCode: null,
      recoursFormeDetected: null,
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        OqtfSansDelaiSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(OqtfSansDelaiSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ------------------------------------------------------------------
  // Tests existants (préservés SF-IM-08-04) — 19 tests.
  // ------------------------------------------------------------------

  it('FRANCE → 4 motifs sans délai disponibles', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.motifs.length).toBe(4);
    const codes = component.motifs.map(m => m.code);
    expect(codes).toEqual([
      'RISQUE_FUITE',
      'TROUBLE_ORDRE_PUBLIC',
      'OQTF_PRECEDENTE_INEXECUTEE',
      'AUTRE',
    ]);
  });

  it('FRANCE → isFrance() true, GET appelé au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → isFrance() false, pas d\'appel HTTP au ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone(r => r.url === BASE_URL);
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ placementCra: true }));
    expect(component.result()!.statutDelaiRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.motifSansDelai()).toBe('RISQUE_FUITE');
    expect(component.placementCra()).toBe(true);
    expect(component.recoursForme()).toBe(false);
    expect(component.dateHeureRecours()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si date ou motif manquants', () => {
    component.dateHeureNotificationOqtf.set(null);
    component.motifSansDelai.set('RISQUE_FUITE');
    expect(component.formValid()).toBe(false);

    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set(null);
    expect(component.formValid()).toBe(false);

    component.motifSansDelai.set('RISQUE_FUITE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateHeureNotificationOqtf dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const pad = (n: number) => n.toString().padStart(2, '0');
    const futureIso = `${future.getFullYear()}-${pad(future.getMonth() + 1)}-${pad(future.getDate())}T${pad(future.getHours())}:${pad(future.getMinutes())}`;
    component.dateHeureNotificationOqtf.set(futureIso);
    component.motifSansDelai.set('RISQUE_FUITE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid : si recoursForme=true, dateHeureRecours requise et >= notification', () => {
    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set('RISQUE_FUITE');
    component.recoursForme.set(true);
    component.dateHeureRecours.set(null);
    expect(component.formValid()).toBe(false);

    component.dateHeureRecours.set('2026-04-22T10:00'); // avant notification
    expect(component.formValid()).toBe(false);

    component.dateHeureRecours.set('2026-04-23T12:00');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST recoursForme=false → body sans dateHeureRecours + snackbar succès', () => {
    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set('RISQUE_FUITE');
    component.placementCra.set(true);
    component.recoursForme.set(false);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateHeureNotificationOqtf: '2026-04-23T10:00',
      motifSansDelai: 'RISQUE_FUITE',
      placementCra: true,
      recoursForme: false,
    });
    req.flush(frResponse({ placementCra: true }));

    expect(component.result()!.statutDelaiRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('OQTF sans délai analysée', 'OK', jasmine.any(Object));
  });

  it('analyze() POST recoursForme=true inclut dateHeureRecours + bannière succès verte', () => {
    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set('RISQUE_FUITE');
    component.placementCra.set(false);
    component.recoursForme.set(true);
    component.dateHeureRecours.set('2026-04-23T20:00');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateHeureNotificationOqtf: '2026-04-23T10:00',
      motifSansDelai: 'RISQUE_FUITE',
      placementCra: false,
      recoursForme: true,
      dateHeureRecours: '2026-04-23T20:00',
    });
    req.flush(frResponse({
      recoursForme: true,
      dateHeureRecours: '2026-04-23T20:00',
      statutDelaiRecours: 'RECOURS_FORME',
      dateHeureAudiencePrevisionnelle: '2026-04-25T09:00',
      dateDecisionPrevisionnelle: '2026-04-25',
      heuresRestantes: 0,
    }));

    expect(component.result()!.dateHeureAudiencePrevisionnelle).toBe('2026-04-25T09:00');
    expect(component.bannerClass('RECOURS_FORME')).toContain('oqtf-sd-banner--success');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
    expect(component.showHeuresRestantes(component.result())).toBe(false);
  });

  it('bannerClass : DISPONIBLE=danger-medium, URGENT=danger-strong, EXPIRE=danger-dark, RECOURS_FORME=success', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('oqtf-sd-banner--danger-medium');
    expect(component.bannerClass('URGENT')).toContain('oqtf-sd-banner--danger-strong');
    expect(component.bannerClass('EXPIRE')).toContain('oqtf-sd-banner--danger-dark');
    expect(component.bannerClass('RECOURS_FORME')).toContain('oqtf-sd-banner--success');
  });

  it('bannerIcon : DISPONIBLE=warning, URGENT=error, EXPIRE=error, RECOURS_FORME=check_circle', () => {
    expect(component.bannerIcon('DISPONIBLE')).toBe('warning');
    expect(component.bannerIcon('URGENT')).toBe('error');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
  });

  it('placementCra=true dans la réponse → résultat expose le flag (badge CRA visible côté template)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ placementCra: true, statutDelaiRecours: 'URGENT' }));
    expect(component.result()!.placementCra).toBe(true);
    expect(component.result()!.statutDelaiRecours).toBe('URGENT');
  });

  it('showHeuresRestantes true seulement pour DISPONIBLE et URGENT', () => {
    expect(component.showHeuresRestantes(frResponse({ statutDelaiRecours: 'DISPONIBLE' }))).toBe(true);
    expect(component.showHeuresRestantes(frResponse({ statutDelaiRecours: 'URGENT' }))).toBe(true);
    expect(component.showHeuresRestantes(frResponse({ statutDelaiRecours: 'EXPIRE' }))).toBe(false);
    expect(component.showHeuresRestantes(frResponse({ statutDelaiRecours: 'RECOURS_FORME' }))).toBe(false);
    expect(component.showHeuresRestantes(null)).toBe(false);
  });

  it('analyze() erreur backend → snackbar rouge + analyzing reset', () => {
    component.dateHeureNotificationOqtf.set('2026-04-23T10:00');
    component.motifSansDelai.set('RISQUE_FUITE');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.analyzing()).toBe(false);
  });

  it('analyze() ignoré si form invalide (pas d\'appel HTTP POST)', () => {
    component.dateHeureNotificationOqtf.set(null);
    component.motifSansDelai.set(null);
    component.analyze();
    httpMock.expectNone(r => r.method === 'POST');
  });

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

  it('statutLabel renvoie un libellé humain pour chaque statut', () => {
    expect(component.statutLabel('DISPONIBLE')).toBe('Délai 48h disponible');
    expect(component.statutLabel('URGENT')).toBe('Délai 48h urgent');
    expect(component.statutLabel('EXPIRE')).toBe('Délai 48h expiré');
    expect(component.statutLabel('RECOURS_FORME')).toBe('Recours formé');
  });

  // ------------------------------------------------------------------
  // SF-155-04-B2 — Nouveaux tests : pré-fill IA + alertes cohérence.
  // ------------------------------------------------------------------

  describe('SF-155-04-B2 — pré-fill IA', () => {
    beforeEach(() => {
      // On évite que ngOnInit déclenche un GET non attendu — on gère case par case.
    });

    it('prefillFromAi : no-op quand aiData === undefined', () => {
      component.aiData = undefined;
      component.ngOnInit();
      const req = httpMock.expectOne(BASE_URL);
      req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.dateHeureNotificationOqtf()).toBeNull();
      expect(component.motifSansDelai()).toBeNull();
      expect(component.placementCra()).toBe(false);
      expect(component.recoursForme()).toBe(false);
      expect(component.provenanceDateHeure()).toBeNull();
      expect(component.provenanceMotifSansDelai()).toBeNull();
      expect(component.provenancePlacementCra()).toBeNull();
      expect(component.provenanceRecoursForme()).toBeNull();
    });

    it('prefillFromAi : date/heure pré-remplie depuis dateHeureNotificationOqtfSansDelai + badge IA', () => {
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-23T10:30' });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.dateHeureNotificationOqtf()).toBe('2026-04-23T10:30');
      expect(component.provenanceDateHeure()).toBe('IA');
    });

    it('prefillFromAi : datetime IA avec secondes → normalisé YYYY-MM-DDTHH:mm', () => {
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-23T10:30:45' });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.dateHeureNotificationOqtf()).toBe('2026-04-23T10:30');
      expect(component.provenanceDateHeure()).toBe('IA');
    });

    it('prefillFromAi : datetime IA format invalide → non rempli, pas de provenance', () => {
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '23/04/2026 10:30' });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.dateHeureNotificationOqtf()).toBeNull();
      expect(component.provenanceDateHeure()).toBeNull();
    });

    it('prefillFromAi : placementCra=true pré-rempli depuis placementCraDetected=true', () => {
      component.aiData = aiData({ placementCraDetected: true });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.placementCra()).toBe(true);
      expect(component.provenancePlacementCra()).toBe('IA');
    });

    it('prefillFromAi : placementCraDetected=null → champ reste false, pas de provenance', () => {
      component.aiData = aiData({ placementCraDetected: null });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.placementCra()).toBe(false);
      expect(component.provenancePlacementCra()).toBeNull();
    });

    it('prefillFromAi : recoursForme=true depuis recoursFormeDetected.reponse=OUI', () => {
      component.aiData = aiData({ recoursFormeDetected: { reponse: 'OUI', justification: 'Recours déposé le 23/04' } });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.recoursForme()).toBe(true);
      expect(component.provenanceRecoursForme()).toBe('IA');
    });

    it('prefillFromAi : recoursFormeDetected.reponse=INCONNU → pas de pré-fill', () => {
      component.aiData = aiData({ recoursFormeDetected: { reponse: 'INCONNU', justification: null } });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.recoursForme()).toBe(false);
      expect(component.provenanceRecoursForme()).toBeNull();
    });

    it('prefillFromAi : motifSansDelai pré-rempli si motifOqtfCode=AUTRE (enum commun)', () => {
      component.aiData = aiData({ motifOqtfCode: 'AUTRE' });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.motifSansDelai()).toBe('AUTRE');
      expect(component.provenanceMotifSansDelai()).toBe('IA');
    });

    it('prefillFromAi : motifOqtfCode=EXPIRATION_TITRE (hors enum MotifSansDelai) → skip silencieux', () => {
      component.aiData = aiData({ motifOqtfCode: 'EXPIRATION_TITRE' });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.motifSansDelai()).toBeNull();
      expect(component.provenanceMotifSansDelai()).toBeNull();
    });

    it('workspaceCountry=BELGIQUE → aucun pré-fill, aucun appel HTTP', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = aiData({
        dateHeureNotificationOqtfSansDelai: '2026-04-23T10:30',
        placementCraDetected: true,
        recoursFormeDetected: { reponse: 'OUI', justification: null },
      });
      component.ngOnInit();
      httpMock.expectNone(r => r.url === BASE_URL);
      expect(component.dateHeureNotificationOqtf()).toBeNull();
      expect(component.placementCra()).toBe(false);
      expect(component.recoursForme()).toBe(false);
      expect(component.provenanceDateHeure()).toBeNull();
    });

    it('ngOnChanges aiData avant résolution → re-applique prefillFromAi', () => {
      component.aiData = undefined;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      expect(component.dateHeureNotificationOqtf()).toBeNull();

      // Nouvel aiData arrive après ngOnInit.
      const newAi = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-23T10:30' });
      component.aiData = newAi;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, newAi, false),
      });
      expect(component.dateHeureNotificationOqtf()).toBe('2026-04-23T10:30');
      expect(component.provenanceDateHeure()).toBe('IA');
    });

    it('ngOnChanges aiData après result() non null → pas de re-invocation prefillFromAi', () => {
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush(frResponse({ placementCra: true }));
      expect(component.showForm()).toBe(false);
      // Snapshot valeur actuelle.
      const snapshotDate = component.dateHeureNotificationOqtf();

      // aiData arrive plus tard avec une autre valeur — ne doit pas écraser.
      const newAi = aiData({ dateHeureNotificationOqtfSansDelai: '2030-01-01T00:00' });
      component.aiData = newAi;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, newAi, false),
      });
      expect(component.dateHeureNotificationOqtf()).toBe(snapshotDate);
      expect(component.provenanceDateHeure()).toBeNull();
    });
  });

  describe('SF-155-04-B2 — handlers onXxxChange effacent la provenance IA', () => {
    beforeEach(() => {
      component.aiData = undefined;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    });

    it('onDateHeureNotificationChange efface provenanceDateHeure', () => {
      component.provenanceDateHeure.set('IA');
      component.onDateHeureNotificationChange('2026-04-23T11:00');
      expect(component.provenanceDateHeure()).toBeNull();
      expect(component.dateHeureNotificationOqtf()).toBe('2026-04-23T11:00');
    });

    it('onMotifSansDelaiChange efface provenanceMotifSansDelai', () => {
      component.provenanceMotifSansDelai.set('IA');
      component.onMotifSansDelaiChange('RISQUE_FUITE');
      expect(component.provenanceMotifSansDelai()).toBeNull();
      expect(component.motifSansDelai()).toBe('RISQUE_FUITE');
    });

    it('onPlacementCraChange efface provenancePlacementCra', () => {
      component.provenancePlacementCra.set('IA');
      component.onPlacementCraChange(false);
      expect(component.provenancePlacementCra()).toBeNull();
      expect(component.placementCra()).toBe(false);
    });

    it('onRecoursFormeChange efface provenanceRecoursForme', () => {
      component.provenanceRecoursForme.set('IA');
      component.onRecoursFormeChange(true);
      expect(component.provenanceRecoursForme()).toBeNull();
      expect(component.recoursForme()).toBe(true);
    });
  });

  describe('SF-155-04-B2 — alertes cohérence F-IA-03 (urgence 48h)', () => {
    // Horloge fixée pour déterminisme : 2026-04-25 10:00:00 local.
    const NOW_MS = new Date('2026-04-25T10:00:00').getTime();

    beforeEach(() => {
      component.aiData = undefined;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      component.setNowMsOverride(NOW_MS);
    });

    it('ALERTE CRITIQUE 48h : notif IA il y a 49h + recoursForme=false → alerte RECOURS_FORME CRITICAL', () => {
      // 49h avant "now" = 2026-04-23T09:00
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-23T09:00' });
      component.recoursForme.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.RECOURS_FORME).toBeTruthy();
      expect(alerts.RECOURS_FORME!.severity).toBe('CRITICAL');
      expect(alerts.RECOURS_FORME!.expectedDisplay).toContain('hors délai');
      expect(component.alertsSummary().blockers).toBe(1);
    });

    it('ALERTE CRITIQUE 48h NON déclenchée si recoursForme=true (recours formé)', () => {
      component.aiData = aiData({
        dateHeureNotificationOqtfSansDelai: '2026-04-23T09:00', // > 48h avant
        recoursFormeDetected: { reponse: 'OUI', justification: null },
      });
      component.recoursForme.set(true);
      const alerts = component.coherenceAlerts();
      // Ni critique 48h, ni contradiction (OUI == true).
      expect(alerts.RECOURS_FORME).toBeUndefined();
    });

    it('ALERTE CRITIQUE 48h NON déclenchée si notif < 48h (encore dans le délai)', () => {
      // 24h avant "now" = 2026-04-24T10:00 (exactement 24h)
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-24T10:00' });
      component.recoursForme.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.RECOURS_FORME).toBeUndefined();
    });

    it('Edge case 48h exact : notif il y a 48h pile → alerte non déclenchée (borne non stricte)', () => {
      // 48h pile avant now = 2026-04-23T10:00
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-23T10:00' });
      component.recoursForme.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.RECOURS_FORME).toBeUndefined();
    });

    it('Edge case 48h + 1 min : notif il y a 48h01 → alerte déclenchée', () => {
      // 48h + 1 min avant now = 2026-04-23T09:59
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-23T09:59' });
      component.recoursForme.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.RECOURS_FORME).toBeTruthy();
      expect(alerts.RECOURS_FORME!.severity).toBe('CRITICAL');
    });

    it('ALERTE CRITIQUE CRA : placementCraDetected=true + avocat placementCra=false → PLACEMENT_CRA CRITICAL', () => {
      component.aiData = aiData({ placementCraDetected: true });
      component.placementCra.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.PLACEMENT_CRA).toBeTruthy();
      expect(alerts.PLACEMENT_CRA!.severity).toBe('CRITICAL');
      expect(alerts.PLACEMENT_CRA!.expectedDisplay).toContain('CRA');
    });

    it('Alerte CRA NON déclenchée si placementCraDetected=false', () => {
      component.aiData = aiData({ placementCraDetected: false });
      component.placementCra.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.PLACEMENT_CRA).toBeUndefined();
    });

    it('Alerte CRA NON déclenchée si avocat coche true (conforme à l\'IA)', () => {
      component.aiData = aiData({ placementCraDetected: true });
      component.placementCra.set(true);
      const alerts = component.coherenceAlerts();
      expect(alerts.PLACEMENT_CRA).toBeUndefined();
    });

    it('Divergence date/heure > 1h → alerte DATE_HEURE_NOTIFICATION WARNING', () => {
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-24T08:00' });
      component.dateHeureNotificationOqtf.set('2026-04-24T12:00'); // +4h
      const alerts = component.coherenceAlerts();
      expect(alerts.DATE_HEURE_NOTIFICATION).toBeTruthy();
      expect(alerts.DATE_HEURE_NOTIFICATION!.severity).toBe('WARNING');
    });

    it('Divergence date/heure <= 1h → pas d\'alerte DATE_HEURE_NOTIFICATION', () => {
      component.aiData = aiData({ dateHeureNotificationOqtfSansDelai: '2026-04-24T10:00' });
      component.dateHeureNotificationOqtf.set('2026-04-24T10:30'); // +30 min
      const alerts = component.coherenceAlerts();
      expect(alerts.DATE_HEURE_NOTIFICATION).toBeUndefined();
    });

    it('Contradiction recours formé : IA=OUI + avocat=false → RECOURS_FORME WARNING (pas CRITICAL)', () => {
      // Notif < 48h pour ne pas déclencher le critique.
      component.aiData = aiData({
        dateHeureNotificationOqtfSansDelai: '2026-04-24T11:00',
        recoursFormeDetected: { reponse: 'OUI', justification: 'Recours déposé au greffe le 23/04' },
      });
      component.recoursForme.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.RECOURS_FORME).toBeTruthy();
      expect(alerts.RECOURS_FORME!.severity).toBe('WARNING');
      expect(alerts.RECOURS_FORME!.reason).toContain('déjà formé');
    });

    it('Critique 48h prime sur la contradiction recours (une seule alerte RECOURS_FORME, CRITICAL)', () => {
      // Notif > 48h + IA dit NON + avocat false → théoriquement cohérent (NON==false), mais notif > 48h.
      // On teste que le critique prime même si la reponse IA est cohérente.
      component.aiData = aiData({
        dateHeureNotificationOqtfSansDelai: '2026-04-23T09:00',
        recoursFormeDetected: { reponse: 'NON', justification: null },
      });
      component.recoursForme.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.RECOURS_FORME).toBeTruthy();
      expect(alerts.RECOURS_FORME!.severity).toBe('CRITICAL');
    });

    it('Aucune alerte quand showForm()=false (post-calcul)', () => {
      component.aiData = aiData({
        dateHeureNotificationOqtfSansDelai: '2026-04-23T09:00',
        placementCraDetected: true,
      });
      component.recoursForme.set(false);
      component.placementCra.set(false);
      // Sanity-check : alertes présentes en showForm.
      expect(component.alertsSummary().total).toBeGreaterThan(0);

      component.showForm.set(false);
      expect(component.coherenceAlerts()).toEqual({});
      expect(component.alertsSummary().total).toBe(0);
    });

    it('Aucune alerte quand workspaceCountry=BELGIQUE (gate pays)', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = aiData({
        dateHeureNotificationOqtfSansDelai: '2026-04-23T09:00',
        placementCraDetected: true,
      });
      component.recoursForme.set(false);
      expect(component.coherenceAlerts()).toEqual({});
    });
  });

  describe('SF-155-04-B2 — helpers alertes', () => {
    it('alertBadgeLabel distingue CRITICAL vs WARNING', () => {
      const critical = {
        field: 'RECOURS_FORME' as const,
        source: 'IA' as const,
        contributors: ['IA' as const],
        severity: 'CRITICAL' as const,
        reason: 'r',
        expectedDisplay: 'Recours hors délai probable',
      };
      const warning = {
        field: 'DATE_HEURE_NOTIFICATION' as const,
        source: 'IA' as const,
        contributors: ['IA' as const],
        severity: 'WARNING' as const,
        reason: 'r',
        expectedDisplay: 'Date IA divergente',
      };
      expect(component.alertBadgeLabel(critical)).toContain('Alerte critique');
      expect(component.alertBadgeLabel(warning)).toContain('Incohérence détectée');
    });

    it('alertTooltip retourne la raison brute', () => {
      const a = {
        field: 'PLACEMENT_CRA' as const,
        source: 'IA' as const,
        contributors: ['IA' as const],
        severity: 'CRITICAL' as const,
        reason: 'raison explicative',
        expectedDisplay: 'Placement CRA détecté',
      };
      expect(component.alertTooltip(a)).toBe('raison explicative');
    });
  });

  // ---------------------------------------------------------------------------
  // SF-155-05 — interface `CoherenceAlert<OqtfSdAlertField>` partagée
  // ---------------------------------------------------------------------------

  it('SF-155-05 : alerte PLACEMENT_CRA expose contract CoherenceAlert — severity=CRITICAL', () => {
    component.aiData = { placementCraDetected: true };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onPlacementCraChange(false);
    const alert = component.coherenceAlerts().PLACEMENT_CRA;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('PLACEMENT_CRA');
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.severity).toBe('CRITICAL');
  });

  it('SF-155-05 : alerte DATE_HEURE_NOTIFICATION contract CoherenceAlert — severity=WARNING', () => {
    component.aiData = { dateHeureNotificationOqtfSansDelai: '2026-04-23T10:00' };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDateHeureNotificationChange('2026-04-23T15:00'); // > 1h d'écart
    const alert = component.coherenceAlerts().DATE_HEURE_NOTIFICATION;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.contributors).toEqual(['IA']);
  });
});
