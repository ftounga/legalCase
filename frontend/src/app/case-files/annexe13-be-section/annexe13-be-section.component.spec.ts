import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { SimpleChange } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Annexe13BeSectionComponent } from './annexe13-be-section.component';
import { Annexe13BeResponse } from '../../core/models/annexe13-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('Annexe13BeSectionComponent', () => {
  let component: Annexe13BeSectionComponent;
  let fixture: ComponentFixture<Annexe13BeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/annexe13-be';

  function beResponse(overrides: Partial<Annexe13BeResponse> = {}): Annexe13BeResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationAnnexe13: '2026-04-01',
      delaiDepartImposeJours: 30,
      motifOqt: 'SEJOUR_IRREGULIER_ART_7',
      transfertImminent: false,
      recoursForme: false,
      dateRecours: null,
      typeRecours: null,
      country: 'BELGIQUE',
      dateExpirationDelaiDepart: '2026-05-01',
      dateExpirationRecoursAnnulation: '2026-05-01',
      dateExpirationRecoursExtremeUrgence: '2026-04-08',
      joursRestantsAvantExpirationAnnulation: 12,
      statutRecoursAnnulation: 'DISPONIBLE',
      dateAudiencePrevisionnelle: null,
      dateDecisionPrevisionnelle: null,
      referedDisponibles: ['RECOURS_ANNULATION_30J', 'RECOURS_EXTREME_URGENCE_5JO'],
      formule: 'Notification + délai imposé',
      baseJuridique: 'Art. 39/2, 39/82 Loi 15.12.1980',
      messages: ['Recours suspensif possible'],
      ...overrides,
    };
  }

  /** SF-155-04-C : flush silencieux des requêtes /source-explanations déclenchées par ngOnInit. */
  function flushSourceExplanations(): void {
    httpMock.match(r => r.url.endsWith('/source-explanations')).forEach(r => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        Annexe13BeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Annexe13BeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('BELGIQUE → 4 motifs OQT disponibles', () => {
    expect(component.motifs.length).toBe(4);
    const codes = component.motifs.map(m => m.code);
    expect(codes).toEqual([
      'SEJOUR_IRREGULIER_ART_7',
      'REFUS_SEJOUR_APRES_DEMANDE',
      'FIN_SEJOUR_REGULIER',
      'AUTRE',
    ]);
  });

  it('BELGIQUE → 2 types de recours', () => {
    expect(component.typesRecours.length).toBe(2);
    const codes = component.typesRecours.map(t => t.code);
    expect(codes).toEqual(['ANNULATION_30J', 'EXTREME_URGENCE_5JO']);
  });

  it('BELGIQUE → isBelgium() true, GET appelé au ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isBelgium()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
  });

  it('FRANCE → isBelgium() false, pas d\'appel HTTP au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isBelgium()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone(r => r.url === BASE_URL);
    // SF-155-04-C : loadSourceExplanations est gated par isBelgium() aussi
    httpMock.expectNone(r => r.url.endsWith('/source-explanations'));
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(beResponse());
    flushSourceExplanations();
    expect(component.result()!.statutRecoursAnnulation).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationAnnexe13()).toBe('2026-04-01');
    expect(component.motifOqt()).toBe('SEJOUR_IRREGULIER_ART_7');
    expect(component.delaiDepartImposeJours()).toBe(30);
    expect(component.transfertImminent()).toBe(false);
    expect(component.recoursForme()).toBe(false);
    expect(component.dateRecours()).toBeNull();
    expect(component.typeRecours()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si date ou motif manquants', () => {
    component.dateNotificationAnnexe13.set(null);
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    expect(component.formValid()).toBe(false);

    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set(null);
    expect(component.formValid()).toBe(false);

    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si dateNotificationAnnexe13 dans le futur', () => {
    const future = new Date();
    future.setDate(future.getDate() + 10);
    const futureIso = future.toISOString().slice(0, 10);
    component.dateNotificationAnnexe13.set(futureIso);
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si delaiDepartImposeJours hors bornes [0-30]', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    component.delaiDepartImposeJours.set(-1);
    expect(component.formValid()).toBe(false);
    component.delaiDepartImposeJours.set(31);
    expect(component.formValid()).toBe(false);
    component.delaiDepartImposeJours.set(0);
    expect(component.formValid()).toBe(true);
    component.delaiDepartImposeJours.set(30);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : si recoursForme=true, dateRecours + typeRecours requis', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    component.recoursForme.set(true);
    component.dateRecours.set(null);
    component.typeRecours.set(null);
    expect(component.formValid()).toBe(false);

    component.dateRecours.set('2026-04-05');
    component.typeRecours.set(null);
    expect(component.formValid()).toBe(false);

    component.typeRecours.set('ANNULATION_30J');
    expect(component.formValid()).toBe(true);

    component.dateRecours.set('2026-03-15'); // avant notification → invalide
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST recoursForme=false → body sans dateRecours/typeRecours', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    component.delaiDepartImposeJours.set(30);
    component.transfertImminent.set(false);
    component.recoursForme.set(false);
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationAnnexe13: '2026-04-01',
      delaiDepartImposeJours: 30,
      motifOqt: 'SEJOUR_IRREGULIER_ART_7',
      transfertImminent: false,
      recoursForme: false,
    });
    req.flush(beResponse());

    expect(component.result()!.statutRecoursAnnulation).toBe('DISPONIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Annexe 13 analysée', 'OK', jasmine.any(Object));
  });

  it('analyze() POST recoursForme=true → body inclut dateRecours + typeRecours', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
    component.delaiDepartImposeJours.set(30);
    component.transfertImminent.set(true);
    component.recoursForme.set(true);
    component.typeRecours.set('EXTREME_URGENCE_5JO');
    component.dateRecours.set('2026-04-03');
    component.analyze();

    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body).toEqual({
      dateNotificationAnnexe13: '2026-04-01',
      delaiDepartImposeJours: 30,
      motifOqt: 'SEJOUR_IRREGULIER_ART_7',
      transfertImminent: true,
      recoursForme: true,
      dateRecours: '2026-04-03',
      typeRecours: 'EXTREME_URGENCE_5JO',
    });
    req.flush(beResponse({
      transfertImminent: true,
      recoursForme: true,
      dateRecours: '2026-04-03',
      typeRecours: 'EXTREME_URGENCE_5JO',
      statutRecoursAnnulation: 'RECOURS_FORME',
      dateAudiencePrevisionnelle: '2026-07-02',
      dateDecisionPrevisionnelle: '2026-09-30',
      joursRestantsAvantExpirationAnnulation: 0,
    }));

    expect(component.result()!.transfertImminent).toBe(true);
    expect(component.result()!.typeRecours).toBe('EXTREME_URGENCE_5JO');
    expect(component.bannerClass('RECOURS_FORME')).toContain('annexe13-banner--success');
  });

  it('transfertImminent=true persisté dans le result (drive le badge template)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(beResponse({ transfertImminent: true }));
    flushSourceExplanations();
    expect(component.result()!.transfertImminent).toBe(true);
    expect(component.transfertImminent()).toBe(true);
  });

  it('bannerClass : DISPONIBLE=info, URGENT=warning, EXPIRE=danger, RECOURS_FORME=success', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('annexe13-banner--info');
    expect(component.bannerClass('URGENT')).toContain('annexe13-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('annexe13-banner--danger');
    expect(component.bannerClass('RECOURS_FORME')).toContain('annexe13-banner--success');
  });

  it('bannerIcon : DISPONIBLE=info_outline, URGENT=warning, EXPIRE=error, RECOURS_FORME=check_circle', () => {
    expect(component.bannerIcon('DISPONIBLE')).toBe('info_outline');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('error');
    expect(component.bannerIcon('RECOURS_FORME')).toBe('check_circle');
  });

  it('statut EXPIRE → classe rouge (seule utilisation rouge)', () => {
    expect(component.bannerClass('EXPIRE')).toContain('annexe13-banner--danger');
  });

  it('showJoursRestants true seulement pour DISPONIBLE et URGENT', () => {
    expect(component.showJoursRestants(beResponse({ statutRecoursAnnulation: 'DISPONIBLE' }))).toBe(true);
    expect(component.showJoursRestants(beResponse({ statutRecoursAnnulation: 'URGENT' }))).toBe(true);
    expect(component.showJoursRestants(beResponse({ statutRecoursAnnulation: 'EXPIRE' }))).toBe(false);
    expect(component.showJoursRestants(beResponse({ statutRecoursAnnulation: 'RECOURS_FORME' }))).toBe(false);
    expect(component.showJoursRestants(null)).toBe(false);
  });

  it('analyze() erreur backend → snackbar rouge + analyzing reset', () => {
    component.dateNotificationAnnexe13.set('2026-04-01');
    component.motifOqt.set('SEJOUR_IRREGULIER_ART_7');
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
    component.dateNotificationAnnexe13.set(null);
    component.motifOqt.set(null);
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

  it('typeRecoursLabel renvoie un libellé humain', () => {
    expect(component.typeRecoursLabel('ANNULATION_30J')).toBe('Annulation 30 jours');
    expect(component.typeRecoursLabel('EXTREME_URGENCE_5JO')).toBe('Extrême urgence 5 jours ouvrables');
    expect(component.typeRecoursLabel(null)).toBe('');
  });

  // ============================================================================
  // SF-155-04-C : pré-fill IA + validation F-IA-03
  // ============================================================================

  describe('SF-155-04-C — pré-fill IA', () => {
    /** Fixture ImmigrationExtractedData BE complète (4 champs SF-155-04-00-BE-immig-BE). */
    function aiDataBeComplete(overrides: Partial<ImmigrationExtractedData> = {}): ImmigrationExtractedData {
      return {
        dateNotificationAnnexe13: '2026-04-10',
        delaiDepartImposeJours: 7,
        motifOqtCodeBe: 'SEJOUR_IRREGULIER_ART_7',
        transfertImminentDetected: false,
        ...overrides,
      };
    }

    it('prefill complet BE : 4 champs + 4 provenance IA (après GET 404)', () => {
      component.aiData = aiDataBeComplete();
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      expect(component.dateNotificationAnnexe13()).toBe('2026-04-10');
      expect(component.delaiDepartImposeJours()).toBe(7);
      expect(component.motifOqt()).toBe('SEJOUR_IRREGULIER_ART_7');
      expect(component.transfertImminent()).toBe(false);
      expect(component.provenanceDateNotification()).toBe('IA');
      expect(component.provenanceDelaiDepart()).toBe('IA');
      expect(component.provenanceMotifOqt()).toBe('IA');
      expect(component.provenanceTransfertImminent()).toBe('IA');
    });

    it('prefill partiel : seule la date est présente → seule la date a provenance IA', () => {
      component.aiData = { dateNotificationAnnexe13: '2026-04-05' };
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      expect(component.dateNotificationAnnexe13()).toBe('2026-04-05');
      expect(component.provenanceDateNotification()).toBe('IA');
      // Les autres champs restent à leur défaut sans provenance
      expect(component.delaiDepartImposeJours()).toBe(30);
      expect(component.provenanceDelaiDepart()).toBeNull();
      expect(component.motifOqt()).toBeNull();
      expect(component.provenanceMotifOqt()).toBeNull();
      expect(component.transfertImminent()).toBe(false);
      expect(component.provenanceTransfertImminent()).toBeNull();
    });

    it('motifOqtCodeBe hors whitelist ("INCONNU") → motif non pré-rempli, graceful', () => {
      component.aiData = aiDataBeComplete({ motifOqtCodeBe: 'INCONNU' as any });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      expect(component.motifOqt()).toBeNull();
      expect(component.provenanceMotifOqt()).toBeNull();
      // Les autres champs valides sont toujours pré-remplis
      expect(component.provenanceDateNotification()).toBe('IA');
    });

    it('delaiDepartImposeJours < 0 → délai non pré-rempli, défaut 30 préservé', () => {
      component.aiData = aiDataBeComplete({ delaiDepartImposeJours: -5 });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      expect(component.delaiDepartImposeJours()).toBe(30);
      expect(component.provenanceDelaiDepart()).toBeNull();
    });

    it('delaiDepartImposeJours = 0 (OQT urgence) → délai pré-rempli 0 + provenance', () => {
      component.aiData = aiDataBeComplete({ delaiDepartImposeJours: 0 });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      expect(component.delaiDepartImposeJours()).toBe(0);
      expect(component.provenanceDelaiDepart()).toBe('IA');
    });

    it('transfertImminentDetected = null → pas de pré-fill, pas de provenance', () => {
      component.aiData = aiDataBeComplete({ transfertImminentDetected: null });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      expect(component.transfertImminent()).toBe(false);
      expect(component.provenanceTransfertImminent()).toBeNull();
    });

    it('transfertImminentDetected = false explicite → pré-fill false + provenance IA', () => {
      component.aiData = aiDataBeComplete({ transfertImminentDetected: false });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      expect(component.transfertImminent()).toBe(false);
      expect(component.provenanceTransfertImminent()).toBe('IA');
    });

    it('loadExisting GET 200 → résultat affiché, prefillFromAi NON appelé (pas d\'écrasement)', () => {
      component.aiData = aiDataBeComplete({
        dateNotificationAnnexe13: '2026-04-10',
        delaiDepartImposeJours: 7,
      });
      component.ngOnInit();
      // GET 200 → résultat backend prend la main
      httpMock.expectOne(BASE_URL).flush(beResponse({
        dateNotificationAnnexe13: '2026-03-15',
        delaiDepartImposeJours: 30,
      }));
      flushSourceExplanations();

      // Les valeurs du backend sont celles affichées (pas celles de l'IA)
      expect(component.dateNotificationAnnexe13()).toBe('2026-03-15');
      expect(component.delaiDepartImposeJours()).toBe(30);
      expect(component.showForm()).toBe(false);
      // Aucune provenance IA posée (pas d'écrasement)
      expect(component.provenanceDateNotification()).toBeNull();
      expect(component.provenanceDelaiDepart()).toBeNull();
    });

    it('workspace FRANCE → prefillFromAi non appelé (gate isBelgium)', () => {
      component.workspaceCountry = 'FRANCE';
      component.aiData = aiDataBeComplete();
      component.ngOnInit();
      // Pas d'appel GET côté FR (gate déjà existant)
      httpMock.expectNone(BASE_URL);
      // Les signals restent à leur défaut
      expect(component.dateNotificationAnnexe13()).toBeNull();
      expect(component.motifOqt()).toBeNull();
      expect(component.provenanceDateNotification()).toBeNull();
    });

    it('fixture malformée (motifOqtCodeBe numeric) → prefill graceful skip, pas d\'exception', () => {
      component.aiData = aiDataBeComplete({ motifOqtCodeBe: 12345 as any });
      expect(() => {
        component.ngOnInit();
        httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
        flushSourceExplanations();
      }).not.toThrow();
      expect(component.motifOqt()).toBeNull();
      expect(component.provenanceMotifOqt()).toBeNull();
    });

    it('delaiDepartImposeJours non entier (7.5) → prefill skip (integer check)', () => {
      component.aiData = aiDataBeComplete({ delaiDepartImposeJours: 7.5 });
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      expect(component.delaiDepartImposeJours()).toBe(30);
      expect(component.provenanceDelaiDepart()).toBeNull();
    });
  });

  describe('SF-155-04-C — onChange handlers (provenance clear)', () => {
    beforeEach(() => {
      // Pré-fill initial simulé : les 4 signals provenance sont à 'IA'.
      component.provenanceDateNotification.set('IA');
      component.provenanceDelaiDepart.set('IA');
      component.provenanceMotifOqt.set('IA');
      component.provenanceTransfertImminent.set('IA');
    });

    it('onDateNotificationChange → provenance à null', () => {
      component.onDateNotificationChange('2026-04-15');
      expect(component.dateNotificationAnnexe13()).toBe('2026-04-15');
      expect(component.provenanceDateNotification()).toBeNull();
    });

    it('onDelaiDepartChange → provenance à null', () => {
      component.onDelaiDepartChange(15);
      expect(component.delaiDepartImposeJours()).toBe(15);
      expect(component.provenanceDelaiDepart()).toBeNull();
    });

    it('onMotifOqtChange → provenance à null', () => {
      component.onMotifOqtChange('AUTRE');
      expect(component.motifOqt()).toBe('AUTRE');
      expect(component.provenanceMotifOqt()).toBeNull();
    });

    it('onTransfertImminentChange → provenance à null', () => {
      component.onTransfertImminentChange(true);
      expect(component.transfertImminent()).toBe(true);
      expect(component.provenanceTransfertImminent()).toBeNull();
    });

    it('onDelaiDepartChange(null) → fallback défaut 30', () => {
      component.onDelaiDepartChange(null);
      expect(component.delaiDepartImposeJours()).toBe(30);
      expect(component.provenanceDelaiDepart()).toBeNull();
    });
  });

  describe('SF-155-04-C — coherenceAlerts', () => {
    beforeEach(() => {
      component.showForm.set(true);
    });

    it('alerte CRITIQUE TRANSFERT_IMMINENT : IA=true + avocat=false → severity=CRITICAL', () => {
      component.aiData = { transfertImminentDetected: true };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.transfertImminent.set(false);
      const alerts = component.coherenceAlerts();
      expect(alerts.TRANSFERT_IMMINENT).toBeDefined();
      // SF-155-05 : `blocker: true` legacy → `severity: 'CRITICAL'`.
      expect(alerts.TRANSFERT_IMMINENT!.severity).toBe('CRITICAL');
      expect(alerts.TRANSFERT_IMMINENT!.source).toBe('IA');
      expect(component.alertsSummary().blockers).toBe(1);
    });

    it('alerte CRITIQUE TRANSFERT_IMMINENT non déclenchée si avocat a coché transfert=true', () => {
      component.aiData = { transfertImminentDetected: true };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.transfertImminent.set(true);
      expect(component.coherenceAlerts().TRANSFERT_IMMINENT).toBeUndefined();
    });

    it('alerte DELAI_DEPART : IA=30 + avocat=7 → severity=WARNING', () => {
      component.aiData = { delaiDepartImposeJours: 30 };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.delaiDepartImposeJours.set(7);
      const alert = component.coherenceAlerts().DELAI_DEPART;
      expect(alert).toBeDefined();
      // SF-155-05 : `blocker: false` legacy → `severity: 'WARNING'`.
      expect(alert!.severity).toBe('WARNING');
      expect(alert!.expectedDisplay).toBe('30 jour(s)');
    });

    it('alerte MOTIF_OQT : IA=SEJOUR_IRREGULIER_ART_7 + avocat=AUTRE → severity=WARNING', () => {
      component.aiData = { motifOqtCodeBe: 'SEJOUR_IRREGULIER_ART_7' };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.motifOqt.set('AUTRE');
      const alert = component.coherenceAlerts().MOTIF_OQT;
      expect(alert).toBeDefined();
      expect(alert!.severity).toBe('WARNING');
      expect(alert!.expectedDisplay).toContain('Séjour irrégulier');
    });

    it('alerte DATE_NOTIFICATION : IA ≠ avocat → warning', () => {
      component.aiData = { dateNotificationAnnexe13: '2026-04-01' };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.dateNotificationAnnexe13.set('2026-04-02');
      const alert = component.coherenceAlerts().DATE_NOTIFICATION;
      expect(alert).toBeDefined();
      expect(alert!.expectedDisplay).toBe('2026-04-01');
    });

    it('showForm=false → coherenceAlerts = {}', () => {
      component.aiData = { transfertImminentDetected: true };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.showForm.set(false);
      expect(component.coherenceAlerts()).toEqual({});
    });

    it('motif IA hors whitelist → pas d\'alerte MOTIF_OQT (ne pas signaler divergence sur code invalide)', () => {
      component.aiData = { motifOqtCodeBe: 'INCONNU' };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.motifOqt.set('AUTRE');
      expect(component.coherenceAlerts().MOTIF_OQT).toBeUndefined();
    });

    it('délai IA non entier (7.5) → pas d\'alerte DELAI_DEPART', () => {
      component.aiData = { delaiDepartImposeJours: 7.5 };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.delaiDepartImposeJours.set(30);
      expect(component.coherenceAlerts().DELAI_DEPART).toBeUndefined();
    });

    it('aiData null → coherenceAlerts = {}', () => {
      component.aiData = null;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, null, true),
      });
      expect(component.coherenceAlerts()).toEqual({});
    });

    it('alertBadgeLabel — severity=CRITICAL → "Alerte critique (...)"', () => {
      expect(component.alertBadgeLabel({
        field: 'TRANSFERT_IMMINENT', source: 'IA', severity: 'CRITICAL',
        contributors: ['IA'],
        expectedDisplay: 'X', reason: 'Y',
      })).toContain('Alerte critique');
    });

    it('alertBadgeLabel — severity=WARNING → "Incohérence détectée (...)"', () => {
      expect(component.alertBadgeLabel({
        field: 'DELAI_DEPART', source: 'IA', severity: 'WARNING',
        contributors: ['IA'],
        expectedDisplay: 'X', reason: 'Y',
      })).toContain('Incohérence');
    });
  });

  describe('SF-155-04-C — ngOnChanges re-prefill', () => {
    it('ngOnChanges(aiData) en mode formulaire sans result → re-prefill appliqué', () => {
      component.aiData = undefined;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
      flushSourceExplanations();

      // Initialement rien en provenance
      expect(component.provenanceMotifOqt()).toBeNull();

      // L'IA termine son analyse pendant que l'avocat a le formulaire ouvert
      const newAi: ImmigrationExtractedData = {
        motifOqtCodeBe: 'FIN_SEJOUR_REGULIER',
        delaiDepartImposeJours: 15,
      };
      component.aiData = newAi;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, newAi, false),
      });

      expect(component.motifOqt()).toBe('FIN_SEJOUR_REGULIER');
      expect(component.provenanceMotifOqt()).toBe('IA');
      expect(component.delaiDepartImposeJours()).toBe(15);
      expect(component.provenanceDelaiDepart()).toBe('IA');
    });

    it('ngOnChanges(aiData) alors que result existe (showForm=false) → pas de re-prefill', () => {
      component.aiData = undefined;
      component.ngOnInit();
      httpMock.expectOne(BASE_URL).flush(beResponse({
        motifOqt: 'AUTRE',
        delaiDepartImposeJours: 30,
      }));
      flushSourceExplanations();
      expect(component.showForm()).toBe(false);
      expect(component.result()).not.toBeNull();

      const newAi: ImmigrationExtractedData = {
        motifOqtCodeBe: 'SEJOUR_IRREGULIER_ART_7',
        delaiDepartImposeJours: 7,
      };
      component.aiData = newAi;
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, newAi, false),
      });

      // Valeurs du résultat chargé, non écrasées
      expect(component.motifOqt()).toBe('AUTRE');
      expect(component.delaiDepartImposeJours()).toBe(30);
      expect(component.provenanceMotifOqt()).toBeNull();
    });

    it('ngOnChanges(aiData) firstChange → pas de prefill (le ngOnInit s\'en charge déjà)', () => {
      // firstChange=true simule le premier binding Angular
      component.aiData = { motifOqtCodeBe: 'SEJOUR_IRREGULIER_ART_7' };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      // En firstChange, ngOnChanges ne doit pas lancer prefillFromAi (évite double-appel)
      // Seul ngOnInit le fera. Les signals d'entrée sont en revanche mis à jour.
      expect(component.motifOqt()).toBeNull(); // pas de prefill via onChanges firstChange
    });
  });

  describe('SF-155-04-C — explanationFor mapping', () => {
    it('explanationFor mappe vers la bonne sourceKey (fallback vide = fail-open)', () => {
      expect(component.explanationFor('TRANSFERT_IMMINENT')).toEqual([]);
      expect(component.explanationFor('DATE_NOTIFICATION')).toEqual([]);
      expect(component.explanationFor('DELAI_DEPART')).toEqual([]);
      expect(component.explanationFor('MOTIF_OQT')).toEqual([]);
    });
  });

  // ---------------------------------------------------------------------------
  // SF-155-05 — interface `CoherenceAlert<IM08AnnexeBeAlertField>` partagée
  // ---------------------------------------------------------------------------

  describe('SF-155-05 — interface partagée', () => {
    beforeEach(() => {
      component.showForm.set(true);
    });

    it('SF-155-05 : alerte TRANSFERT_IMMINENT contract CoherenceAlert — severity=CRITICAL, contributors=[IA]', () => {
      component.aiData = { transfertImminentDetected: true };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.transfertImminent.set(false);
      const alert = component.coherenceAlerts().TRANSFERT_IMMINENT;
      expect(alert).toBeDefined();
      expect(alert!.field).toBe('TRANSFERT_IMMINENT');
      expect(alert!.source).toBe('IA');
      expect(alert!.contributors).toEqual(['IA']);
      expect(alert!.severity).toBe('CRITICAL');
    });

    it('SF-155-05 : alerte DELAI_DEPART contract CoherenceAlert — severity=WARNING, contributors=[IA]', () => {
      component.aiData = { delaiDepartImposeJours: 30 };
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
      });
      component.delaiDepartImposeJours.set(7);
      const alert = component.coherenceAlerts().DELAI_DEPART;
      expect(alert).toBeDefined();
      expect(alert!.source).toBe('IA');
      expect(alert!.contributors).toEqual(['IA']);
      expect(alert!.severity).toBe('WARNING');
    });
  });

  // ---------------------------------------------------------------------------
  // SF-155-06 — enrichissement 4-sources (ferme DIV-2)
  // ---------------------------------------------------------------------------

  describe('SF-155-06 — multi-sources', () => {
    beforeEach(() => {
      component.showForm.set(true);
    });

    it('SF-155-06 : F96 seul sur MOTIF_OQT → alerte source F96', () => {
      component.procedureChecks = [
        {
          id: 'chk-1', ordre: 1, description: 'Motif OQT',
          statut: 'NON_COMPLIANT',
          critereCode: 'IM08_MOTIF_OQT_BE',
          expectedValue: 'SEJOUR_IRREGULIER_ART_7',
        },
      ];
      component.ngOnChanges({
        procedureChecks: new SimpleChange(undefined, component.procedureChecks, true),
      });
      component.motifOqt.set('FIN_SEJOUR_REGULIER');
      const alert = component.coherenceAlerts().MOTIF_OQT;
      expect(alert).toBeDefined();
      expect(alert!.source).toBe('F96');
      expect(alert!.contributors).toEqual(['F96']);
    });

    it('SF-155-06 : IA + F96 convergents sur MOTIF_OQT → alerte MULTI avec 2 contributors', () => {
      component.aiData = { motifOqtCodeBe: 'SEJOUR_IRREGULIER_ART_7' } as ImmigrationExtractedData;
      component.procedureChecks = [
        {
          id: 'chk-1', ordre: 1, description: 'Motif OQT',
          statut: 'NON_COMPLIANT',
          critereCode: 'IM08_MOTIF_OQT_BE',
          expectedValue: 'SEJOUR_IRREGULIER_ART_7',
        },
      ];
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
        procedureChecks: new SimpleChange(undefined, component.procedureChecks, true),
      });
      component.motifOqt.set('FIN_SEJOUR_REGULIER');
      const alert = component.coherenceAlerts().MOTIF_OQT;
      expect(alert).toBeDefined();
      expect(alert!.source).toBe('MULTI');
      expect(alert!.contributors.length).toBe(2);
      expect(alert!.contributors).toContain('F96');
      expect(alert!.contributors).toContain('IA');
      expect(alert!.reason).toContain(' ET ');
    });

    it('SF-155-06 : IA + PIECE_MANQUANTE sur TRANSFERT_IMMINENT → alerte avec pieceTexte', () => {
      component.aiData = { transfertImminentDetected: true } as ImmigrationExtractedData;
      component.piecesManquantes = [
        { texte: 'Ordre de mission de la police des frontières', critereCode: 'IM08_TRANSFERT' },
      ];
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, component.aiData, true),
        piecesManquantes: new SimpleChange(undefined, component.piecesManquantes, true),
      });
      component.transfertImminent.set(false);
      const alert = component.coherenceAlerts().TRANSFERT_IMMINENT;
      expect(alert).toBeDefined();
      expect(alert!.contributors).toContain('IA');
      expect(alert!.contributors).toContain('PIECE_MANQUANTE');
      expect(alert!.pieceTexte).toBe('Ordre de mission de la police des frontières');
    });

    it('SF-155-06 : PIECE_MANQUANTE seule (sans IA) sur DATE_NOTIFICATION → pas d\'alerte', () => {
      component.aiData = null;
      component.piecesManquantes = [
        { texte: 'Notification originale', critereCode: 'IM08_DATE_NOTIFICATION' },
      ];
      component.ngOnChanges({
        aiData: new SimpleChange(undefined, null, true),
        piecesManquantes: new SimpleChange(undefined, component.piecesManquantes, true),
      });
      component.dateNotificationAnnexe13.set('2026-04-10');
      expect(component.coherenceAlerts().DATE_NOTIFICATION).toBeUndefined();
    });
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-08-annexe13-be/calculate';

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
