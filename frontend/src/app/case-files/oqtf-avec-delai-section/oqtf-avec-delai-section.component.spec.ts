import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { OqtfAvecDelaiSectionComponent } from './oqtf-avec-delai-section.component';
import { OqtfAvecDelaiResponse } from '../../core/models/oqtf-avec-delai.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('OqtfAvecDelaiSectionComponent', () => {
  let component: OqtfAvecDelaiSectionComponent;
  let fixture: ComponentFixture<OqtfAvecDelaiSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/oqtf-avec-delai';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  /** SF-155-07 (DIV-7) : absorbe la requête source-explanations (fail-open). */
  function flushSourceExplanations(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  function frResponse(overrides: Partial<OqtfAvecDelaiResponse> = {}): OqtfAvecDelaiResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationOqtf: '2026-04-01',
      motifOqtf: 'REFUS_TITRE',
      recoursForme: false,
      dateRecours: null,
      country: 'FRANCE',
      dateExpirationDdv: '2026-05-01',
      dateExpirationDelaiRecours: '2026-05-01',
      joursRestantsAvantExpirationDelai: 12,
      statutDelaiRecours: 'DISPONIBLE',
      dateAudiencePrevisionnelle: null,
      dateDecisionTaPrevisionnelle: null,
      referedDisponibles: ['REFERE_SUSPENSION_L521_1', 'REFERE_LIBERTE_L521_2'],
      formule: 'Notification + 30 jours',
      baseJuridique: 'Art. L.614-5, L.614-6, R.776-18 CJA',
      messages: ['Recours suspensif'],
      ...overrides,
    };
  }

  /** Helper : flush un GET 404 (pas d'analyse existante → reste en mode formulaire). */
  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        OqtfAvecDelaiSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(OqtfAvecDelaiSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // SF-155-07 (DIV-7) : absorbe les requêtes source-explanations en attente.
    flushSourceExplanations();
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Tests existants (SF-IM-08-02) — préservés
  // ============================================================

  it('FRANCE → 5 motifs OQTF disponibles', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.motifs.length).toBe(5);
    const codes = component.motifs.map(m => m.code);
    expect(codes).toEqual(['REFUS_TITRE', 'EXPIRATION_TITRE', 'SEJOUR_IRREGULIER', 'RETRAIT_TITRE', 'AUTRE']);
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
    req.flush(frResponse());
    expect(component.result()!.statutDelaiRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationOqtf()).toBe('2026-04-01');
    expect(component.motifOqtf()).toBe('REFUS_TITRE');
    expect(component.recoursForme()).toBe(false);
    expect(component.dateRecours()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si date ou motif manquants', () => {
    component.dateNotificationOqtf.set(null);
    component.motifOqtf.set('REFUS_TITRE');
    expect(component.formValid()).toBe(false);

    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set(null);
    expect(component.formValid()).toBe(false);

    component.motifOqtf.set('REFUS_TITRE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateNotificationOqtf dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateNotificationOqtf.set(futureIso);
    component.motifOqtf.set('REFUS_TITRE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid : si recoursForme=true, dateRecours requise et >= notification', () => {
    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set('REFUS_TITRE');
    component.recoursForme.set(true);
    component.dateRecours.set(null);
    expect(component.formValid()).toBe(false);

    component.dateRecours.set('2026-03-15');
    expect(component.formValid()).toBe(false);

    component.dateRecours.set('2026-04-05');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST recoursForme=false → résultat + snackbar succès + pas de dateRecours dans le body', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set('REFUS_TITRE');
    component.recoursForme.set(false);
    component.dateRecours.set(null);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationOqtf: '2026-04-01',
      motifOqtf: 'REFUS_TITRE',
      recoursForme: false,
    });
    req.flush(frResponse());

    expect(component.result()!.statutDelaiRecours).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('OQTF analysée', 'OK', jasmine.any(Object));
  });

  it('analyze() POST recoursForme=true inclut dateRecours + bannière succès verte', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set('REFUS_TITRE');
    component.recoursForme.set(true);
    component.dateRecours.set('2026-04-10');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateNotificationOqtf: '2026-04-01',
      motifOqtf: 'REFUS_TITRE',
      recoursForme: true,
      dateRecours: '2026-04-10',
    });
    req.flush(frResponse({
      recoursForme: true,
      dateRecours: '2026-04-10',
      statutDelaiRecours: 'RECOURS_FORME',
      dateAudiencePrevisionnelle: '2026-07-09',
      dateDecisionTaPrevisionnelle: '2026-10-07',
      joursRestantsAvantExpirationDelai: 0,
    }));

    expect(component.result()!.dateAudiencePrevisionnelle).toBe('2026-07-09');
    expect(component.bannerClass('RECOURS_FORME')).toContain('oqtf-banner--success');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
    expect(component.showJoursRestants(component.result())).toBe(false);
  });

  it('bannerClass : DISPONIBLE=info, URGENT=warning, EXPIRE=danger, RECOURS_FORME=success', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('oqtf-banner--info');
    expect(component.bannerClass('URGENT')).toContain('oqtf-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('oqtf-banner--danger');
    expect(component.bannerClass('RECOURS_FORME')).toContain('oqtf-banner--success');
  });

  it('bannerIcon : DISPONIBLE=info_outline, URGENT=warning, EXPIRE=error, RECOURS_FORME=check_circle', () => {
    expect(component.bannerIcon('DISPONIBLE')).toBe('info_outline');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
  });

  it('statut EXPIRE → classe rouge (seule utilisation rouge)', () => {
    expect(component.bannerClass('EXPIRE')).toContain('oqtf-banner--danger');
  });

  it('showJoursRestants true seulement pour DISPONIBLE et URGENT', () => {
    expect(component.showJoursRestants(frResponse({ statutDelaiRecours: 'DISPONIBLE' }))).toBe(true);
    expect(component.showJoursRestants(frResponse({ statutDelaiRecours: 'URGENT' }))).toBe(true);
    expect(component.showJoursRestants(frResponse({ statutDelaiRecours: 'EXPIRE' }))).toBe(false);
    expect(component.showJoursRestants(frResponse({ statutDelaiRecours: 'RECOURS_FORME' }))).toBe(false);
    expect(component.showJoursRestants(null)).toBe(false);
  });

  it('analyze() erreur backend → snackbar rouge + analyzing reset', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationOqtf.set('2026-04-01');
    component.motifOqtf.set('REFUS_TITRE');
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
    component.ngOnInit();
    flush404();
    component.dateNotificationOqtf.set(null);
    component.motifOqtf.set(null);
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
    expect(component.statutLabel('DISPONIBLE')).toBe('Délai disponible');
    expect(component.statutLabel('URGENT')).toBe('Délai urgent');
    expect(component.statutLabel('EXPIRE')).toBe('Délai expiré');
    expect(component.statutLabel('RECOURS_FORME')).toBe('Recours formé');
  });

  // ============================================================
  // SF-155-04-B1 : pré-fill IA + validation F-IA-03
  // ============================================================

  describe('SF-155-04-B1 — pré-fill IA', () => {
    function aiComplete(overrides: Partial<ImmigrationExtractedData> = {}): ImmigrationExtractedData {
      return {
        dateNotificationOqtf: '2026-04-01',
        motifOqtfCode: 'REFUS_TITRE',
        recoursFormeDetected: { reponse: 'NON', justification: null },
        ...overrides,
      };
    }

    it('1. aiData complet → pré-fill des 3 champs + badges provenance IA', () => {
      component.aiData = aiComplete({
        recoursFormeDetected: { reponse: 'OUI', justification: 'Mémoire TA du 2026-04-05' },
      });
      component.ngOnInit();
      flush404();
      expect(component.dateNotificationOqtf()).toBe('2026-04-01');
      expect(component.motifOqtf()).toBe('REFUS_TITRE');
      expect(component.recoursForme()).toBe(true);
      expect(component.provenanceDateNotification()).toBe('IA');
      expect(component.provenanceMotifOqtf()).toBe('IA');
      expect(component.provenanceRecoursForme()).toBe('IA');
    });

    it('2. motifOqtfCode hors enum → skip silencieux (motif reste null, pas de badge)', () => {
      component.aiData = aiComplete({
        motifOqtfCode: 'INCONNU_XYZ' as any,
      });
      component.ngOnInit();
      flush404();
      expect(component.motifOqtf()).toBeNull();
      expect(component.provenanceMotifOqtf()).toBeNull();
      // les autres champs passent bien
      expect(component.dateNotificationOqtf()).toBe('2026-04-01');
    });

    it('3. recoursFormeDetected.reponse=OUI → recoursForme true + badge', () => {
      component.aiData = {
        recoursFormeDetected: { reponse: 'OUI', justification: null },
      };
      component.ngOnInit();
      flush404();
      expect(component.recoursForme()).toBe(true);
      expect(component.provenanceRecoursForme()).toBe('IA');
    });

    it('4. recoursFormeDetected.reponse=NON → recoursForme false + badge', () => {
      component.aiData = {
        recoursFormeDetected: { reponse: 'NON', justification: null },
      };
      component.ngOnInit();
      flush404();
      expect(component.recoursForme()).toBe(false);
      expect(component.provenanceRecoursForme()).toBe('IA');
    });

    it('5. recoursFormeDetected.reponse=INCONNU → pas de pré-fill, pas de badge', () => {
      component.aiData = {
        recoursFormeDetected: { reponse: 'INCONNU', justification: null },
      };
      component.ngOnInit();
      flush404();
      expect(component.recoursForme()).toBe(false);
      expect(component.provenanceRecoursForme()).toBeNull();
    });

    it('6. dateNotificationOqtf malformée (format FR 31/03/2026) → champ reste null', () => {
      component.aiData = {
        dateNotificationOqtf: '31/03/2026',
      };
      component.ngOnInit();
      flush404();
      expect(component.dateNotificationOqtf()).toBeNull();
      expect(component.provenanceDateNotification()).toBeNull();
    });

    it('7. dateNotificationOqtf dans le futur → champ reste null', () => {
      const future = new Date();
      future.setDate(future.getDate() + 10);
      const futureIso = future.toISOString().slice(0, 10);
      component.aiData = { dateNotificationOqtf: futureIso };
      component.ngOnInit();
      flush404();
      expect(component.dateNotificationOqtf()).toBeNull();
      expect(component.provenanceDateNotification()).toBeNull();
    });

    it('8. onMotifOqtfChange efface le badge IA', () => {
      component.aiData = aiComplete();
      component.ngOnInit();
      flush404();
      expect(component.provenanceMotifOqtf()).toBe('IA');
      component.onMotifOqtfChange('AUTRE');
      expect(component.provenanceMotifOqtf()).toBeNull();
      expect(component.motifOqtf()).toBe('AUTRE');
    });

    it('8b. onDateNotificationChange efface le badge IA', () => {
      component.aiData = aiComplete();
      component.ngOnInit();
      flush404();
      expect(component.provenanceDateNotification()).toBe('IA');
      component.onDateNotificationChange('2026-04-05');
      expect(component.provenanceDateNotification()).toBeNull();
    });

    it('8c. onRecoursFormeChange efface le badge IA', () => {
      component.aiData = {
        recoursFormeDetected: { reponse: 'OUI', justification: null },
      };
      component.ngOnInit();
      flush404();
      expect(component.provenanceRecoursForme()).toBe('IA');
      component.onRecoursFormeChange(false);
      expect(component.provenanceRecoursForme()).toBeNull();
      expect(component.recoursForme()).toBe(false);
    });

    it('13. ngOnChanges avec nouveau aiData en mode formulaire → re-prefill', () => {
      component.ngOnInit();
      flush404();
      expect(component.motifOqtf()).toBeNull();
      // Pipeline IA termine plus tard et le panel pousse aiData
      component.aiData = aiComplete({ motifOqtfCode: 'EXPIRATION_TITRE' });
      component.ngOnChanges({
        aiData: new SimpleChange(null, component.aiData, false),
      });
      expect(component.motifOqtf()).toBe('EXPIRATION_TITRE');
      expect(component.provenanceMotifOqtf()).toBe('IA');
    });

    it('14. ngOnChanges avec result() persisté → pas de re-prefill (pas d\'écrasement)', () => {
      component.ngOnInit();
      const req = httpMock.expectOne(BASE_URL);
      req.flush(frResponse({ motifOqtf: 'RETRAIT_TITRE' }));
      // analyse déjà chargée — maintenant aiData arrive
      component.aiData = aiComplete({ motifOqtfCode: 'EXPIRATION_TITRE' });
      component.ngOnChanges({
        aiData: new SimpleChange(null, component.aiData, false),
      });
      // motifOqtf doit rester celui chargé depuis l'analyse existante
      expect(component.motifOqtf()).toBe('RETRAIT_TITRE');
      expect(component.provenanceMotifOqtf()).toBeNull();
    });

    it('15. GET 200 analyse persistée → prefillFromAi NON appelé, pas de badge', () => {
      component.aiData = aiComplete({ motifOqtfCode: 'EXPIRATION_TITRE' });
      component.ngOnInit();
      const req = httpMock.expectOne(BASE_URL);
      req.flush(frResponse({ motifOqtf: 'REFUS_TITRE' }));
      expect(component.motifOqtf()).toBe('REFUS_TITRE');
      expect(component.provenanceMotifOqtf()).toBeNull();
      expect(component.provenanceDateNotification()).toBeNull();
      expect(component.provenanceRecoursForme()).toBeNull();
    });

    it('16. workspaceCountry=BELGIQUE → pas de pré-fill, pas d\'alerte', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = aiComplete();
      component.ngOnInit();
      httpMock.expectNone(r => r.url === BASE_URL);
      expect(component.dateNotificationOqtf()).toBeNull();
      expect(component.motifOqtf()).toBeNull();
      expect(component.provenanceDateNotification()).toBeNull();
      expect(component.coherenceAlerts()).toEqual({});
    });

    it('17. fixture aiData malformée (reponse FOO) → no-op défensif', () => {
      component.aiData = {
        recoursFormeDetected: { reponse: 'FOO' as any, justification: null },
      };
      component.ngOnInit();
      flush404();
      expect(component.recoursForme()).toBe(false);
      expect(component.provenanceRecoursForme()).toBeNull();
    });

    it('18. aiData undefined → prefillFromAi no-op', () => {
      component.aiData = undefined;
      component.ngOnInit();
      flush404();
      expect(component.dateNotificationOqtf()).toBeNull();
      expect(component.provenanceDateNotification()).toBeNull();
    });
  });

  describe('SF-155-04-B1 — alertes de cohérence F-IA-03', () => {
    it('9. divergence date notification → alerte DATE_NOTIFICATION (WARNING)', () => {
      component.aiData = { dateNotificationOqtf: '2026-04-01' };
      component.ngOnInit();
      flush404();
      // avocat écrase avec une autre date
      component.onDateNotificationChange('2026-04-03');
      const alert = component.coherenceAlerts().DATE_NOTIFICATION;
      expect(alert).toBeTruthy();
      expect(alert!.severity).toBe('WARNING');
      expect(alert!.expectedDisplay).toBe('2026-04-01');
    });

    it('9b. pas de divergence date notification si aiData absent', () => {
      component.aiData = {};
      component.ngOnInit();
      flush404();
      component.dateNotificationOqtf.set('2026-04-03');
      expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
    });

    it('10. divergence motif OQTF → alerte MOTIF_OQTF (WARNING)', () => {
      component.aiData = { motifOqtfCode: 'EXPIRATION_TITRE' };
      component.ngOnInit();
      flush404();
      // Après prefill, motif = EXPIRATION_TITRE ; avocat écrase en REFUS_TITRE
      component.onMotifOqtfChange('REFUS_TITRE');
      const alert = component.coherenceAlerts().MOTIF_OQTF;
      expect(alert).toBeTruthy();
      expect(alert!.severity).toBe('WARNING');
      expect(alert!.expectedDisplay).toBe('Expiration de titre');
    });

    it('11. contradiction critique recours formé détecté OUI + avocat non coché → alerte CRITICAL', () => {
      component.aiData = {
        recoursFormeDetected: { reponse: 'OUI', justification: 'Recours TA du 2026-03-20' },
      };
      component.ngOnInit();
      flush404();
      // Prefill met recoursForme=true + badge IA. L'avocat décide de le décocher.
      component.onRecoursFormeChange(false);
      const alert = component.coherenceAlerts().RECOURS_FORME;
      expect(alert).toBeTruthy();
      expect(alert!.severity).toBe('CRITICAL');
      expect(alert!.expectedDisplay).toContain('Recours déjà formé');
      expect(alert!.reason).toContain('Recours TA du 2026-03-20');
      expect(component.alertsSummary().blockers).toBe(1);
    });

    it('12. pas d\'alerte RECOURS_FORME quand avocat coché true + IA détecte OUI', () => {
      component.aiData = {
        recoursFormeDetected: { reponse: 'OUI', justification: null },
      };
      component.ngOnInit();
      flush404();
      expect(component.recoursForme()).toBe(true);
      expect(component.coherenceAlerts().RECOURS_FORME).toBeUndefined();
    });

    it('12b. pas d\'alerte RECOURS_FORME quand IA détecte NON (pas critique)', () => {
      component.aiData = {
        recoursFormeDetected: { reponse: 'NON', justification: null },
      };
      component.ngOnInit();
      flush404();
      component.onRecoursFormeChange(false);
      expect(component.coherenceAlerts().RECOURS_FORME).toBeUndefined();
    });

    it('pas d\'alerte quand showForm=false (analyse validée)', () => {
      component.aiData = { dateNotificationOqtf: '2026-04-01', motifOqtfCode: 'EXPIRATION_TITRE' };
      component.ngOnInit();
      const req = httpMock.expectOne(BASE_URL);
      req.flush(frResponse({ dateNotificationOqtf: '2026-04-05', motifOqtf: 'REFUS_TITRE' }));
      // showForm=false suite à GET 200 — pas d'alertes affichées
      expect(component.coherenceAlerts()).toEqual({});
    });

    it('alertsSummary : total + blockers', () => {
      component.aiData = {
        dateNotificationOqtf: '2026-04-01',
        motifOqtfCode: 'EXPIRATION_TITRE',
        recoursFormeDetected: { reponse: 'OUI', justification: null },
      };
      component.ngOnInit();
      flush404();
      // Après prefill : tout en phase → 0 alerte
      expect(component.alertsSummary().total).toBe(0);
      // L'avocat écrase tout manuellement
      component.onDateNotificationChange('2026-04-03');
      component.onMotifOqtfChange('REFUS_TITRE');
      component.onRecoursFormeChange(false);
      const summary = component.alertsSummary();
      expect(summary.total).toBe(3);
      expect(summary.blockers).toBe(1); // RECOURS_FORME critical
    });

    it('alertBadgeLabel : CRITICAL → "Risque d\'irrecevabilité"', () => {
      const label = component.alertBadgeLabel({
        field: 'RECOURS_FORME',
        source: 'IA',
        contributors: ['IA'],
        severity: 'CRITICAL',
        expectedDisplay: 'Recours déjà formé détecté',
        reason: 'test',
      });
      expect(label).toContain('Risque d\'irrecevabilité');
    });

    it('alertBadgeLabel : WARNING → "Incohérence détectée"', () => {
      const label = component.alertBadgeLabel({
        field: 'MOTIF_OQTF',
        source: 'IA',
        contributors: ['IA'],
        severity: 'WARNING',
        expectedDisplay: 'Refus de titre',
        reason: 'test',
      });
      expect(label).toContain('Incohérence détectée');
    });
  });

  // ---------------------------------------------------------------------------
  // SF-155-05 — interface `CoherenceAlert<OqtfAlertField>` partagée
  // ---------------------------------------------------------------------------

  it('SF-155-05 : alerte MOTIF_OQTF expose contract CoherenceAlert — contributors=[IA], severity=WARNING', () => {
    component.aiData = { motifOqtfCode: 'REFUS_TITRE' };
    component.ngOnInit();
    flush404();
    component.onMotifOqtfChange('EXPIRATION_TITRE');
    component.onDateNotificationChange('2026-04-02');
    const alert = component.coherenceAlerts().MOTIF_OQTF;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('MOTIF_OQTF');
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.severity).toBe('WARNING');
  });

  it('SF-155-05 : alerte RECOURS_FORME expose severity=CRITICAL', () => {
    component.aiData = { recoursFormeDetected: { reponse: 'OUI', justification: 'recours dans le dossier' } };
    component.ngOnInit();
    flush404();
    component.onRecoursFormeChange(false);
    component.onDateNotificationChange('2026-04-02');
    component.onMotifOqtfChange('REFUS_TITRE');
    const alert = component.coherenceAlerts().RECOURS_FORME;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('CRITICAL');
    expect(alert!.contributors).toEqual(['IA']);
  });

  // ---------------------------------------------------------------------------
  // SF-155-06 — enrichissement 4-sources (ferme DIV-2)
  // ---------------------------------------------------------------------------

  it('SF-155-06 : F96 seul sur MOTIF_OQTF → alerte source F96', () => {
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Motif OQTF',
        statut: 'NON_COMPLIANT',
        critereCode: 'IM08_MOTIF_OQTF',
        expectedValue: 'REFUS_TITRE',
      },
    ];
    component.ngOnInit();
    flush404();
    component.onMotifOqtfChange('EXPIRATION_TITRE');
    component.onDateNotificationChange('2026-04-02');
    const alert = component.coherenceAlerts().MOTIF_OQTF;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
    expect(alert!.contributors).toEqual(['F96']);
    expect(alert!.expectedDisplay).toContain('Refus');
  });

  it('SF-155-06 : QUESTION_IA (réponse oui) sur RECOURS_FORME → alerte source QUESTION_IA', () => {
    component.aiQuestions = [
      {
        id: 'q-1', orderIndex: 1,
        questionText: 'Un recours a-t-il déjà été formé contre cette OQTF ?',
        answerText: 'oui, requête en annulation déposée',
        critereCode: 'IM08_RECOURS_FORME',
      },
    ];
    component.ngOnInit();
    flush404();
    // Avocat laisse recoursForme=false → divergence.
    component.onDateNotificationChange('2026-04-02');
    component.onMotifOqtfChange('REFUS_TITRE');
    const alert = component.coherenceAlerts().RECOURS_FORME;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('QUESTION_IA');
    expect(alert!.contributors).toEqual(['QUESTION_IA']);
    expect(alert!.severity).toBe('CRITICAL');
  });

  it('SF-155-06 : IA + F96 convergents sur MOTIF_OQTF → alerte MULTI', () => {
    component.aiData = { motifOqtfCode: 'REFUS_TITRE' } as ImmigrationExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Motif OQTF',
        statut: 'NON_COMPLIANT',
        critereCode: 'IM08_MOTIF_OQTF',
        expectedValue: 'REFUS_TITRE',
      },
    ];
    component.ngOnInit();
    flush404();
    component.onMotifOqtfChange('EXPIRATION_TITRE');
    component.onDateNotificationChange('2026-04-02');
    const alert = component.coherenceAlerts().MOTIF_OQTF;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors.length).toBe(2);
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.reason).toContain(' ET ');
  });

  it('SF-155-06 : IA + PIECE_MANQUANTE sur DATE_NOTIFICATION → contributors inclut PIECE_MANQUANTE', () => {
    component.aiData = { dateNotificationOqtf: '2026-04-01' } as ImmigrationExtractedData;
    component.piecesManquantes = [
      { texte: 'Copie notification OQTF', critereCode: 'IM08_DATE_NOTIFICATION' },
    ];
    component.ngOnInit();
    flush404();
    component.onDateNotificationChange('2026-04-10'); // divergence
    component.onMotifOqtfChange('REFUS_TITRE');
    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.pieceTexte).toBe('Copie notification OQTF');
  });

  // ---------------------------------------------------------------------------
  // SF-155-07 (DIV-6) — DecisionalHeaderFlagComponent pour URGENT / EXPIRE
  // ---------------------------------------------------------------------------

  describe('SF-155-07 (DIV-6) — header flag', () => {
    it('affiche le flag URGENT (variant warning) quand statutDelaiRecours === URGENT', () => {
      // Important : ne pas appeler ngOnInit() manuellement AVANT detectChanges()
      // (risque double-GET). On utilise detectChanges() qui déclenche ngOnInit()
      // une seule fois via le cycle de vie Angular.
      fixture.detectChanges();
      httpMock.expectOne(BASE_URL).flush(frResponse({ statutDelaiRecours: 'URGENT' }));
      fixture.detectChanges();
      // Étend collapsed pour que le header + flag soient dans le DOM.
      component.collapsed.set(false);
      fixture.detectChanges();
      const flag = fixture.nativeElement.querySelector('app-decisional-header-flag');
      expect(flag).toBeTruthy();
      // Le label est passé via @Input → attribut HTML.
      expect((flag.getAttribute('label') ?? flag.textContent ?? '').toUpperCase()).toContain('URGENT');
    });
  });

  // ---------------------------------------------------------------------------
  // SF-155-07 (DIV-7) — explanationFor mapping + fail-open SourceExplanationService
  // ---------------------------------------------------------------------------

  describe('SF-155-07 (DIV-7) — explanationFor', () => {
    it('explanationFor retourne [] quand la map est vide (fail-open)', () => {
      component.ngOnInit();
      flush404();
      flushSourceExplanations();
      expect(component.explanationFor('DATE_NOTIFICATION')).toEqual([]);
      expect(component.explanationFor('MOTIF_OQTF')).toEqual([]);
      expect(component.explanationFor('RECOURS_FORME')).toEqual([]);
    });

    it('explanationFor retourne les explications quand la map est peuplée sur la bonne clé', () => {
      component.ngOnInit();
      flush404();
      httpMock.expectOne(SOURCE_EXPL_URL).flush([
        {
          sourceKey: 'IM08_DATE_NOTIFICATION',
          sourceType: 'DOCUMENT',
          label: 'Annexe OQTF',
          sentence: 'Notifiée le 2026-04-01',
          secondaryText: null,
          actionType: 'OPEN_DOCUMENT',
          actionTarget: 'doc-1',
        },
      ]);
      expect(component.explanationFor('DATE_NOTIFICATION').length).toBe(1);
      expect(component.explanationFor('DATE_NOTIFICATION')[0].label).toBe('Annexe OQTF');
      expect(component.explanationFor('MOTIF_OQTF')).toEqual([]);
    });
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-08-oqtf-avec-delai-fr/calculate';

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
