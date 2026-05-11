import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FinMissionInterimService } from '../../core/services/fin-mission-interim.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { FinMissionInterimSectionComponent } from './fin-mission-interim-section.component';
import { FinMissionInterimResponse } from '../../core/models/fin-mission-interim.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('FinMissionInterimSectionComponent', () => {
  let component: FinMissionInterimSectionComponent;
  let fixture: ComponentFixture<FinMissionInterimSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-42/fin-mission-interim';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-42/source-explanations';

  function response(overrides: Partial<FinMissionInterimResponse> = {}):
      FinMissionInterimResponse {
    return {
      caseFileId: 'case-42',
      totalRemunerationsBrutesEur: 15000,
      dureeMissionJours: 90,
      motifExclusion: null,
      dateFinMission: '2026-04-01',
      tauxApplique: 0.10,
      montantIndemniteEur: 1500,
      exclusionRetenue: false,
      baseJuridique: 'Art. L.1251-32 Code du travail',
      formule: '10 % × 15 000,00 € = 1 500,00 €',
      messages: ['Indemnité de fin de mission due à l\'issue de la mission d\'intérim.'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  function flushSourceExplanations(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        FinMissionInterimSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FinMissionInterimSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-42';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Scénarios classiques (cycle de vie, form, HTTP)
  // ---------------------------------------------------------------------------

  it('6 motifs d\'exclusion L.1251-33 exposés', () => {
    expect(component.motifExclusionOptions.length).toBe(6);
    const codes = component.motifExclusionOptions.map((o) => o.code);
    expect(codes).toContain('CONTRAT_INDETERMINEE_PROPOSE');
    expect(codes).toContain('RUPTURE_ANTICIPEE_SALARIE');
    expect(codes).toContain('FAUTE_GRAVE');
    expect(codes).toContain('FORCE_MAJEURE');
    expect(codes).toContain('MISSION_PEPINIERE_QUALIFIANTE');
    expect(codes).toContain('INTERIMAIRE_REFUS_PROPOSITION_CDI');
  });

  it('GET 200 → mode lecture, form masqué, valeurs hydratées', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.montantIndemniteEur).toBe(1500);
    expect(component.showForm()).toBe(false);
    expect(component.totalRemunerationsBrutesEur()).toBe(15000);
    expect(component.dureeMissionJours()).toBe(90);
    expect(component.motifExclusion()).toBeNull();
    expect(component.dateFinMission()).toBe('2026-04-01');
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid : total > 0, durée > 0 et dateFinMission requis', () => {
    component.totalRemunerationsBrutesEur.set(null);
    component.dureeMissionJours.set(90);
    component.dateFinMission.set('2026-04-01');
    expect(component.formValid()).toBe(false);

    component.totalRemunerationsBrutesEur.set(15000);
    component.dureeMissionJours.set(null);
    expect(component.formValid()).toBe(false);

    component.dureeMissionJours.set(90);
    component.dateFinMission.set(null);
    expect(component.formValid()).toBe(false);

    component.dateFinMission.set('2026-04-01');
    expect(component.formValid()).toBe(true);

    component.totalRemunerationsBrutesEur.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('calculate() POST → résultat + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationsBrutesEur.set(15000);
    component.dureeMissionJours.set(90);
    component.dateFinMission.set('2026-04-01');
    component.motifExclusion.set(null);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      totalRemunerationsBrutesEur: 15000,
      dureeMissionJours: 90,
      motifExclusion: null,
      dateFinMission: '2026-04-01',
    });
    req.flush(response());

    expect(component.result()!.montantIndemniteEur).toBe(1500);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationsBrutesEur.set(15000);
    component.dureeMissionJours.set(90);
    component.dateFinMission.set('2026-04-01');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Motif inconnu' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(refreshSpy.triggerRefresh).not.toHaveBeenCalled();
  });

  it('calculate() ignoré si form invalide (pas de POST)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationsBrutesEur.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Scénarios pré-fill IA + provenance
  // ---------------------------------------------------------------------------

  it('pré-fill IA salaire mensuel si aiData.salaireBrutMensuel > 0', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelReference()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('aiData.salaireBrutMensuel = 0 → pas de pré-fill', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('aiData null → pas de badge IA, pas de pré-fill', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onSalaireChange manuel efface le badge IA', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(2800);
    expect(component.salaireMensuelReference()).toBe(2800);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('GET 200 → pas de badge IA même si aiData présent (persisté > IA)', () => {
    component.aiData = { salaireBrutMensuel: 9999 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response());
    flushSourceExplanations();

    expect(component.totalRemunerationsBrutesEur()).toBe(15000);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Scénarios alertes de cohérence F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE_MENSUEL présent si écart > 10 % vs IA', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // IA = 3000, avocat modifie à 5000 → écart 66 %.
    component.onSalaireChange(5000);

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE_MENSUEL).toBeDefined();
    expect(alerts.SALAIRE_MENSUEL!.field).toBe('SALAIRE_MENSUEL');
    expect(alerts.SALAIRE_MENSUEL!.source).toBe('IA');
    expect(alerts.SALAIRE_MENSUEL!.severity).toBe('WARNING');
    expect(alerts.SALAIRE_MENSUEL!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(3100);

    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const newAi = { salaireBrutMensuel: 2800 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = { aiData: new SimpleChange(null, newAi, false) };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelReference()).toBe(2800);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(4200);
    expect(component.provenanceSalaire()).toBeNull();

    const newAi = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    // Saisie avocat préservée.
    expect(component.salaireMensuelReference()).toBe(4200);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('alertes masquées après showForm=false (anti-bug SF-IA-03-12)', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Scénarios spécifiques F-DT-18
  // ---------------------------------------------------------------------------

  it('workspaceCountry BELGIQUE → bannière info, pas de GET', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL); // ne déclenche pas l'API en BE
    expect(component.showForm()).toBe(true);
  });

  it('motif d\'exclusion sélectionné → envoyé dans le POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.totalRemunerationsBrutesEur.set(20000);
    component.dureeMissionJours.set(120);
    component.dateFinMission.set('2026-04-01');
    component.onMotifExclusionChange('FAUTE_GRAVE');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.motifExclusion).toBe('FAUTE_GRAVE');
    req.flush(response({
      motifExclusion: 'FAUTE_GRAVE',
      tauxApplique: 0,
      montantIndemniteEur: 0,
      exclusionRetenue: true,
      formule: '0,00 €',
      messages: ['Indemnité non due : faute grave (L.1251-33).'],
    }));
    expect(component.result()!.montantIndemniteEur).toBe(0);
    expect(component.result()!.exclusionRetenue).toBe(true);
  });

  it('auto-calcul totalRemunerationsBrutesEur = round(salaireMensuel × dureeJours / 30)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(2500);
    component.onDureeChange(90);
    // 2500 × 90 / 30 = 7500
    expect(component.totalRemunerationsBrutesEur()).toBe(7500);
  });

  it('saisie manuelle totalRemunerationsBrutesEur → l\'auto-calc ne l\'écrase plus', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(2500);
    component.onDureeChange(90);
    expect(component.totalRemunerationsBrutesEur()).toBe(7500);

    // Avocat corrige manuellement → nouvelle saisie durée ne doit pas écraser.
    component.onTotalRemunerationsChange(8200);
    component.onDureeChange(120);
    expect(component.totalRemunerationsBrutesEur()).toBe(8200);
  });

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('alertBadgeLabel et alertTooltip exposent un texte pertinent', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    const alert = component.coherenceAlerts().SALAIRE_MENSUEL!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
    expect(alert.contributors).toEqual(['IA']);
  });

  it('resultBannerClass : --danger si exclusion retenue, neutre sinon', () => {
    component.result.set(response({ exclusionRetenue: false }));
    expect(component.resultBannerClass()).toBe('fmi-amount-block');
    component.result.set(response({ exclusionRetenue: true, montantIndemniteEur: 0 }));
    expect(component.resultBannerClass()).toContain('fmi-amount-block--danger');
  });

  it('onDateFinMissionChange normalise les chaînes vides à null', () => {
    component.onDateFinMissionChange('2026-04-01');
    expect(component.dateFinMission()).toBe('2026-04-01');
    component.onDateFinMissionChange('');
    expect(component.dateFinMission()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02b — mode standalone (CA-08, CA-09, CA-10).
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02b — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-DT-18-fin-mission-interim/calculate';

    it('CA-08 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-08 : pas de bannière en mode case-file (standaloneMode=false)', () => {
      component.standaloneMode = false;
      fixture.detectChanges();
      const matches = httpMock.match(() => true);
      matches.forEach((r) => { try { r.flush({}, { status: 404, statusText: 'Not Found' }); } catch {} });
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).toBeNull();
    });

    it("CA-08 : coherenceAlerts() retourne vide en standalone", () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const alerts = (component as any).coherenceAlerts ? (component as any).coherenceAlerts() : {};
      expect(Object.keys(alerts)).toHaveLength(0);
    });

    it("CA-09 : exposition du service standalone (route dispatcher)", () => {
      // Garde-fou statique : le service expose le toolId du dispatcher.
      // L'intégration runtime est couverte par CA-09 manuel sur staging
      // (3 outils échantillonnés — cf. mini-spec).
      expect(FinMissionInterimService.STANDALONE_TOOL_ID).toBe('F-DT-18-fin-mission-interim');
      expect(STANDALONE_URL).toContain(FinMissionInterimService.STANDALONE_TOOL_ID);
    });
  });

});
