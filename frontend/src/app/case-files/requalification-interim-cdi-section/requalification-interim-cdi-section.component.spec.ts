import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RequalificationInterimCdiService } from '../../core/services/requalification-interim-cdi.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { RequalificationInterimCdiSectionComponent } from './requalification-interim-cdi-section.component';
import { RequalificationInterimCdiResponse } from '../../core/models/requalification-interim-cdi.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('RequalificationInterimCdiSectionComponent', () => {
  let component: RequalificationInterimCdiSectionComponent;
  let fixture: ComponentFixture<RequalificationInterimCdiSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-23/requalification-interim-cdi';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-23/source-explanations';

  function defaultResponse(overrides: Partial<RequalificationInterimCdiResponse> = {}):
      RequalificationInterimCdiResponse {
    return {
      caseFileId: 'case-23',
      motifInterimInvoque: 'ACCROISSEMENT_TEMPORAIRE',
      motifInterdit: false,
      motifInterditType: null,
      successionMissions: [],
      delaiCarenceRespecte: true,
      dureeMissionsTotaleMois: 12,
      salaireMensuelBrutEur: 2500,
      dateFinDerniereMission: '2026-04-15',
      memeEntrepriseUtilisatrice: false,
      scoreRequalification: 70,
      verdictProbabiliteRequalification: 'MOYENNE',
      indemniteRequalificationEur: 2500,
      indemniteFinMissionInterimEur: 3000,
      totalDommagesIndemniteEur: 5500,
      baseJuridique: 'Art. L.1251-40, L.1251-41 Code du travail',
      formule: 'Indemnité requalif = max(salaire mensuel) = 2 500,00 €',
      messages: ['Action en requalification : prescription 12 mois (L.1471-1).',
                 'Cass. soc. — relation triangulaire ETT/EU/salarié.'],
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
        RequalificationInterimCdiSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RequalificationInterimCdiSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-23';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + enums
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE) + 6 motifInterimInvoque + 4 motifInterditType', () => {
    expect(component).toBeTruthy();
    expect(component.motifInterimOptions.length).toBe(6);
    expect(component.motifInterditOptions.length).toBe(4);
    const motifCodes = component.motifInterimOptions.map((o) => o.code);
    expect(motifCodes).toContain('ACCROISSEMENT_TEMPORAIRE');
    expect(motifCodes).toContain('REMPLACEMENT_SALARIE');
    expect(motifCodes).toContain('EMPLOI_SAISONNIER');
    expect(motifCodes).toContain('EMPLOI_USAGE');
    expect(motifCodes).toContain('MISSION_PEPINIERE');
    expect(motifCodes).toContain('AUTRE');
    const interditCodes = component.motifInterditOptions.map((o) => o.code);
    expect(interditCodes).toContain('EMPLOI_PERMANENT');
    expect(interditCodes).toContain('REMPLACEMENT_GREVISTE');
    expect(interditCodes).toContain('TRAVAUX_DANGEREUX');
    expect(interditCodes).toContain('AUTRE');
  });

  // ---------------------------------------------------------------------------
  // Form validators
  // ---------------------------------------------------------------------------

  it('formValid faux si motifInterimInvoque null', () => {
    component.dureeMissionsTotaleMois.set(12);
    component.salaireMensuelBrutEur.set(2500);
    component.dateFinDerniereMission.set('2026-04-15');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si motifInterdit=true sans motifInterditType', () => {
    component.motifInterimInvoque.set('ACCROISSEMENT_TEMPORAIRE');
    component.motifInterdit.set(true);
    component.motifInterditType.set(null);
    component.dureeMissionsTotaleMois.set(12);
    component.salaireMensuelBrutEur.set(2500);
    component.dateFinDerniereMission.set('2026-04-15');
    expect(component.formValid()).toBe(false);

    component.motifInterditType.set('EMPLOI_PERMANENT');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si dureeMissionsTotaleMois null/0/négatif', () => {
    component.motifInterimInvoque.set('ACCROISSEMENT_TEMPORAIRE');
    component.salaireMensuelBrutEur.set(2500);
    component.dateFinDerniereMission.set('2026-04-15');
    component.dureeMissionsTotaleMois.set(null);
    expect(component.formValid()).toBe(false);
    component.dureeMissionsTotaleMois.set(0);
    expect(component.formValid()).toBe(false);
    component.dureeMissionsTotaleMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si salaireMensuelBrutEur null/0/négatif', () => {
    component.motifInterimInvoque.set('ACCROISSEMENT_TEMPORAIRE');
    component.dureeMissionsTotaleMois.set(12);
    component.dateFinDerniereMission.set('2026-04-15');
    component.salaireMensuelBrutEur.set(null);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelBrutEur.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si dateFinDerniereMission vide', () => {
    component.motifInterimInvoque.set('ACCROISSEMENT_TEMPORAIRE');
    component.dureeMissionsTotaleMois.set(12);
    component.salaireMensuelBrutEur.set(2500);
    component.dateFinDerniereMission.set(null);
    expect(component.formValid()).toBe(false);
    component.dateFinDerniereMission.set('');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai sur cas nominal complet', () => {
    component.motifInterimInvoque.set('ACCROISSEMENT_TEMPORAIRE');
    component.motifInterdit.set(false);
    component.dureeMissionsTotaleMois.set(12);
    component.salaireMensuelBrutEur.set(2500);
    component.dateFinDerniereMission.set('2026-04-15');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP lifecycle
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué, valeurs hydratées, pas de badge IA', () => {
    component.aiData = { salaireBrutMensuel: 9999 } as TravailExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse({ memeEntrepriseUtilisatrice: true }));
    flushSourceExplanations();

    expect(component.result()!.scoreRequalification).toBe(70);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelBrutEur()).toBe(2500);
    expect(component.memeEntrepriseUtilisatrice()).toBe(true);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire ; pré-fill IA appliqué', () => {
    component.aiData = { salaireBrutMensuel: 2700 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.showForm()).toBe(true);
    expect(component.salaireMensuelBrutEur()).toBe(2700);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('calculate() POST → résultat affiché + snackbar + dashboardRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.motifInterimInvoque.set('ACCROISSEMENT_TEMPORAIRE');
    component.motifInterdit.set(false);
    component.delaiCarenceRespecte.set(true);
    component.dureeMissionsTotaleMois.set(12);
    component.salaireMensuelBrutEur.set(2500);
    component.dateFinDerniereMission.set('2026-04-15');
    component.memeEntrepriseUtilisatrice.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      motifInterimInvoque: 'ACCROISSEMENT_TEMPORAIRE',
      motifInterdit: false,
      motifInterditType: null,
      successionMissions: [],
      delaiCarenceRespecte: true,
      dureeMissionsTotaleMois: 12,
      salaireMensuelBrutEur: 2500,
      dateFinDerniereMission: '2026-04-15',
      memeEntrepriseUtilisatrice: true,
    });
    req.flush(defaultResponse());

    expect(component.result()!.verdictProbabiliteRequalification).toBe('MOYENNE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse de requalification calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge, pas de refresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.motifInterimInvoque.set('ACCROISSEMENT_TEMPORAIRE');
    component.dureeMissionsTotaleMois.set(12);
    component.salaireMensuelBrutEur.set(2500);
    component.dateFinDerniereMission.set('2026-04-15');
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

    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + provenance
  // ---------------------------------------------------------------------------

  it('pré-fill IA salaireMensuelBrutEur si aiData.salaireBrutMensuel > 0', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('aiData.salaireBrutMensuel = 0 → pas de pré-fill', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('aiData null → pas de badge IA, pas de pré-fill', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onSalaireChange manuel efface le badge IA', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(2800);
    expect(component.salaireMensuelBrutEur()).toBe(2800);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const newAi = { salaireBrutMensuel: 2700 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = { aiData: new SimpleChange(null, newAi, false) };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelBrutEur()).toBe(2700);
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

    expect(component.salaireMensuelBrutEur()).toBe(4200);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-IA-03 cohérence
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE_MENSUEL présent si écart > 10 % vs IA', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(4000);
    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE_MENSUEL).toBeDefined();
    expect(alerts.SALAIRE_MENSUEL!.field).toBe('SALAIRE_MENSUEL');
    expect(alerts.SALAIRE_MENSUEL!.source).toBe('IA');
    expect(alerts.SALAIRE_MENSUEL!.severity).toBe('WARNING');
    expect(alerts.SALAIRE_MENSUEL!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(2600);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  it('alertes masquées après showForm=false (anti-bug SF-IA-03-12)', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(4000);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE_MENSUEL).toBeUndefined();
  });

  it('alertBadgeLabel et alertTooltip exposent un texte pertinent', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    const alert = component.coherenceAlerts().SALAIRE_MENSUEL!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
    expect(alert.contributors).toEqual(['IA']);
  });

  // ---------------------------------------------------------------------------
  // Specifics F-DT-23
  // ---------------------------------------------------------------------------

  it('motifInterdit=false → motifInterditType envoyé null même si saisi avant', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.motifInterimInvoque.set('ACCROISSEMENT_TEMPORAIRE');
    component.motifInterdit.set(true);
    component.motifInterditType.set('EMPLOI_PERMANENT');
    component.onMotifInterditChange(false);

    expect(component.motifInterditType()).toBeNull();

    component.dureeMissionsTotaleMois.set(12);
    component.salaireMensuelBrutEur.set(2500);
    component.dateFinDerniereMission.set('2026-04-15');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.motifInterdit).toBe(false);
    expect(req.request.body.motifInterditType).toBeNull();
    req.flush(defaultResponse());
  });

  it('addMissionInterim ajoute une entrée + reset des champs courants', () => {
    component.onNewMissionDateDebutChange('2025-01-01');
    component.onNewMissionDateFinChange('2025-06-30');
    component.onNewMissionMotifChange('Accroissement temporaire');
    component.onNewMissionEntrepriseUtilisatriceChange('ACME SAS');
    expect(component.newMissionValid()).toBe(true);

    component.addMissionInterim();
    const list = component.successionMissions();
    expect(list.length).toBe(1);
    expect(list[0]).toEqual({
      dateDebut: '2025-01-01',
      dateFin: '2025-06-30',
      motif: 'Accroissement temporaire',
      entrepriseUtilisatrice: 'ACME SAS',
    });
    expect(component.newMissionDateDebut()).toBeNull();
    expect(component.newMissionDateFin()).toBeNull();
    expect(component.newMissionMotif()).toBe('');
    expect(component.newMissionEntrepriseUtilisatrice()).toBe('');
  });

  it('addMissionInterim ignoré si entrepriseUtilisatrice manquante', () => {
    component.onNewMissionDateDebutChange('2025-01-01');
    component.onNewMissionDateFinChange('2025-06-30');
    component.onNewMissionMotifChange('Motif');
    component.onNewMissionEntrepriseUtilisatriceChange('');
    expect(component.newMissionValid()).toBe(false);
    component.addMissionInterim();
    expect(component.successionMissions().length).toBe(0);
  });

  it('addMissionInterim ignoré si dateFin manquante', () => {
    component.onNewMissionDateDebutChange('2025-01-01');
    component.onNewMissionDateFinChange(null);
    component.onNewMissionMotifChange('Motif');
    component.onNewMissionEntrepriseUtilisatriceChange('ACME');
    expect(component.newMissionValid()).toBe(false);
    component.addMissionInterim();
    expect(component.successionMissions().length).toBe(0);
  });

  it('removeMissionInterim supprime l\'entrée à l\'index donné', () => {
    component.onNewMissionDateDebutChange('2025-01-01');
    component.onNewMissionDateFinChange('2025-06-30');
    component.onNewMissionMotifChange('A');
    component.onNewMissionEntrepriseUtilisatriceChange('EU1');
    component.addMissionInterim();

    component.onNewMissionDateDebutChange('2025-07-01');
    component.onNewMissionDateFinChange('2025-12-31');
    component.onNewMissionMotifChange('B');
    component.onNewMissionEntrepriseUtilisatriceChange('EU2');
    component.addMissionInterim();

    expect(component.successionMissions().length).toBe(2);
    component.removeMissionInterim(0);
    expect(component.successionMissions().length).toBe(1);
    expect(component.successionMissions()[0].entrepriseUtilisatrice).toBe('EU2');
  });

  it('successionMissions + memeEntrepriseUtilisatrice envoyés dans le POST', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onNewMissionDateDebutChange('2025-01-01');
    component.onNewMissionDateFinChange('2025-06-30');
    component.onNewMissionMotifChange('Accroissement');
    component.onNewMissionEntrepriseUtilisatriceChange('ACME');
    component.addMissionInterim();

    component.motifInterimInvoque.set('REMPLACEMENT_SALARIE');
    component.dureeMissionsTotaleMois.set(8);
    component.salaireMensuelBrutEur.set(2200);
    component.dateFinDerniereMission.set('2026-04-15');
    component.memeEntrepriseUtilisatrice.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.successionMissions.length).toBe(1);
    expect(req.request.body.successionMissions[0].entrepriseUtilisatrice).toBe('ACME');
    expect(req.request.body.memeEntrepriseUtilisatrice).toBe(true);
    req.flush(defaultResponse());
  });

  it('workspaceCountry BELGIQUE → bannière info, pas de GET', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.isFrance()).toBe(false);
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

  it('verdictBannerClass mappe ELEVEE→danger, MOYENNE→warn, FAIBLE→info', () => {
    expect(component.verdictBannerClass('ELEVEE')).toContain('--danger');
    expect(component.verdictBannerClass('MOYENNE')).toContain('--warn');
    expect(component.verdictBannerClass('FAIBLE')).toContain('--info');
  });

  it('verdictIcon mappe ELEVEE→gavel, MOYENNE→balance, FAIBLE→info_outline', () => {
    expect(component.verdictIcon('ELEVEE')).toBe('gavel');
    expect(component.verdictIcon('MOYENNE')).toBe('balance');
    expect(component.verdictIcon('FAIBLE')).toBe('info_outline');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02b — mode standalone (CA-08, CA-09, CA-10).
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02b — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-DT-23-requalification-interim-cdi/calculate';

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
      expect(RequalificationInterimCdiService.STANDALONE_TOOL_ID).toBe('F-DT-23-requalification-interim-cdi');
      expect(STANDALONE_URL).toContain(RequalificationInterimCdiService.STANDALONE_TOOL_ID);
    });
  });

});
