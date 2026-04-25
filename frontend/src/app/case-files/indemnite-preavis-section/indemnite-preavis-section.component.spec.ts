import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { IndemnitePreavisSectionComponent } from './indemnite-preavis-section.component';
import { IndemnitePreavisResponse } from '../../core/models/indemnite-preavis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('IndemnitePreavisSectionComponent', () => {
  let component: IndemnitePreavisSectionComponent;
  let fixture: ComponentFixture<IndemnitePreavisSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-25/indemnite-preavis';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-25/source-explanations';
  const CONVENTIONS_URL = '/api/v1/referentials/conventions';

  function response(overrides: Partial<IndemnitePreavisResponse> = {}):
      IndemnitePreavisResponse {
    return {
      caseFileId: 'case-25',
      ancienneteAnnees: 3,
      ancienneteMois: 4,
      salaireMensuelBrutEur: 2500,
      conventionCollectiveCode: 'IDCC_3248',
      fonction: 'EMPLOYE',
      exemptionEmployeur: false,
      dateRupture: '2026-03-15',
      dureePreavisMois: 2,
      sourceDuree: 'CCN',
      montantIndemniteEur: 5000,
      exemptionRetenue: false,
      baseJuridique: 'Art. L.1234-1 Code du travail',
      formule: '2 mois × 2 500,00 € = 5 000,00 €',
      messages: ['Préavis dû en l\'absence de faute grave (L.1234-5).'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  /** Absorbe la requête source-explanations émise par ngOnInit. */
  function flushSourceExplanations(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  /** Absorbe la requête /referentials/conventions. */
  function flushConventions(): void {
    const reqs = httpMock.match(CONVENTIONS_URL);
    reqs.forEach((r) => r.flush([
      { code: 'IDCC_3248', label: 'Métallurgie (IDCC 3248)', country: 'FRANCE' },
      { code: 'IDCC_1486', label: 'Syntec (IDCC 1486)', country: 'FRANCE' },
      { code: 'CP200',     label: 'CP 200 — Employés',     country: 'BELGIQUE' },
    ]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        IndemnitePreavisSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IndemnitePreavisSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-25';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Cycle de vie & gate pays
  // ---------------------------------------------------------------------------

  it('FRANCE → 4 options de fonction exposées', () => {
    expect(component.fonctionOptions.length).toBe(4);
    const codes = component.fonctionOptions.map((o) => o.code);
    expect(codes).toEqual(['OUVRIER', 'EMPLOYE', 'AGENT_MAITRISE', 'CADRE']);
  });

  it('BELGIQUE → pas de chargement HTTP, gate bannière info (pas de masquage)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    httpMock.expectNone(CONVENTIONS_URL);
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('charge l\'analyse existante si présente (GET 200) et masque le form', () => {
    component.ngOnInit();
    flushConventions();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(response());
    flushSourceExplanations();

    expect(component.result()!.montantIndemniteEur).toBe(5000);
    expect(component.showForm()).toBe(false);
    expect(component.ancienneteAnnees()).toBe(3);
    expect(component.ancienneteMois()).toBe(4);
    expect(component.salaireMensuelBrutEur()).toBe(2500);
    expect(component.conventionCollectiveCode()).toBe('IDCC_3248');
    expect(component.fonction()).toBe('EMPLOYE');
    expect(component.exemptionEmployeur()).toBe(false);
    expect(component.dateRupture()).toBe('2026-03-15');
    // Valeurs persistées : pas de badge IA.
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceDateRupture()).toBeNull();
    expect(component.provenanceConvention()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    flushConventions();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid false si un champ requis manque', () => {
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(3);
    expect(component.formValid()).toBe(false);

    component.ancienneteMois.set(4);
    component.salaireMensuelBrutEur.set(2500);
    component.fonction.set('EMPLOYE');
    component.dateRupture.set('2026-03-15');
    expect(component.formValid()).toBe(true);

    component.salaireMensuelBrutEur.set(0);
    expect(component.formValid()).toBe(false);

    component.salaireMensuelBrutEur.set(2500);
    component.ancienneteMois.set(12);
    expect(component.formValid()).toBe(false);

    component.ancienneteMois.set(-1);
    expect(component.formValid()).toBe(false);

    component.ancienneteMois.set(0);
    component.ancienneteAnnees.set(-1);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('prefill IA complet (salaire + dateLicenciement + conventionCollective normalisé) → 3 badges IA', () => {
    component.aiData = {
      salaireBrutMensuel: 2700,
      dateLicenciement: '2026-02-01',
      conventionCollective: 'IDCC_3248',
    } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBe(2700);
    expect(component.dateRupture()).toBe('2026-02-01');
    expect(component.conventionCollectiveCode()).toBe('IDCC_3248');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.provenanceConvention()).toBe('IA');
  });

  it('prefill sans aiData → aucune valeur ni badge', () => {
    component.aiData = null;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.dateRupture()).toBeNull();
    expect(component.conventionCollectiveCode()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('prefill salaire ≤ 0 → pas de pré-fill salaire (gracieux)', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('prefill convention non normalisable (texte libre inconnu) → fallback vers code brut, badge IA si match référentiel', () => {
    component.aiData = {
      conventionCollective: 'INCONNUE_LIBRE',
    } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    // INCONNUE_LIBRE n'est pas dans la liste référentiel → pas de pré-sélection.
    expect(component.conventionCollectiveCode()).toBeNull();
    expect(component.provenanceConvention()).toBeNull();
  });

  it('prefill avec dateLicenciement seul → seul dateRupture badgé', () => {
    component.aiData = { dateLicenciement: '2026-01-15' } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.dateRupture()).toBe('2026-01-15');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.conventionCollectiveCode()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Handlers (effacent provenance IA)
  // ---------------------------------------------------------------------------

  it('onSalaireChange efface le badge IA salaire', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.provenanceSalaire()).toBe('IA');

    component.onSalaireChange(3000);
    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onDateRuptureChange efface le badge IA dateRupture', () => {
    component.aiData = { dateLicenciement: '2026-02-01' } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.provenanceDateRupture()).toBe('IA');

    component.onDateRuptureChange('2026-03-01');
    expect(component.dateRupture()).toBe('2026-03-01');
    expect(component.provenanceDateRupture()).toBeNull();
  });

  it('onConventionChange efface le badge IA convention', () => {
    component.aiData = {
      conventionCollective: 'IDCC_3248',
    } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    expect(component.provenanceConvention()).toBe('IA');

    component.onConventionChange('IDCC_1486');
    expect(component.conventionCollectiveCode()).toBe('IDCC_1486');
    expect(component.provenanceConvention()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE présent si écart > 10 %', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(3500); // écart 40 %

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.field).toBe('SALAIRE');
    expect(alerts.SALAIRE!.source).toBe('IA');
    expect(alerts.SALAIRE!.expectedDisplay).toContain('€');
  });

  it('coherenceAlerts.SALAIRE absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(2600); // écart 4 %

    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('coherenceAlerts.DATE_RUPTURE présent si écart ≥ 15 jours', () => {
    component.aiData = { dateLicenciement: '2026-02-01' } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onDateRuptureChange('2026-03-15'); // 42 jours d'écart

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_RUPTURE).toBeDefined();
    expect(alerts.DATE_RUPTURE!.field).toBe('DATE_RUPTURE');
    expect(alerts.DATE_RUPTURE!.expectedDisplay).toBe('2026-02-01');
  });

  it('coherenceAlerts.DATE_RUPTURE absent si écart < 15 jours', () => {
    component.aiData = { dateLicenciement: '2026-02-01' } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onDateRuptureChange('2026-02-10'); // 9 jours d'écart

    expect(component.coherenceAlerts().DATE_RUPTURE).toBeUndefined();
  });

  it('coherenceAlerts.CONVENTION présent si codes divergent', () => {
    component.aiData = {
      conventionCollective: 'IDCC_3248',
    } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    // L'avocat change vers une autre convention.
    component.onConventionChange('IDCC_1486');

    const alerts = component.coherenceAlerts();
    expect(alerts.CONVENTION).toBeDefined();
    expect(alerts.CONVENTION!.field).toBe('CONVENTION');
    expect(alerts.CONVENTION!.expectedDisplay).toBe('IDCC_3248');
  });

  it('coherenceAlerts.CONVENTION absent si codes matchent (case-insensitive)', () => {
    component.aiData = {
      conventionCollective: 'idcc_3248',
    } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    expect(component.coherenceAlerts().CONVENTION).toBeUndefined();
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    expect(component.coherenceAlerts().SALAIRE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // calculate POST
  // ---------------------------------------------------------------------------

  it('calculate() POST avec body correct + snackbar succès + triggerRefresh', () => {
    component.ancienneteAnnees.set(3);
    component.ancienneteMois.set(4);
    component.salaireMensuelBrutEur.set(2500);
    component.conventionCollectiveCode.set('IDCC_3248');
    component.fonction.set('EMPLOYE');
    component.exemptionEmployeur.set(false);
    component.dateRupture.set('2026-03-15');

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      ancienneteAnnees: 3,
      ancienneteMois: 4,
      salaireMensuelBrutEur: 2500,
      conventionCollectiveCode: 'IDCC_3248',
      fonction: 'EMPLOYE',
      exemptionEmployeur: false,
      dateRupture: '2026-03-15',
    });
    req.flush(response());

    expect(component.result()!.montantIndemniteEur).toBe(5000);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur backend → snackbar rouge + calculating reset', () => {
    component.ancienneteAnnees.set(3);
    component.ancienneteMois.set(4);
    component.salaireMensuelBrutEur.set(2500);
    component.fonction.set('EMPLOYE');
    component.dateRupture.set('2026-03-15');

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

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.ancienneteAnnees.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    const newAi = {
      salaireBrutMensuel: 2800,
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = {
      aiData: new SimpleChange(null, newAi, false),
    };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelBrutEur()).toBe(2800);
    expect(component.dateRupture()).toBe('2026-04-01');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceDateRupture()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();

    component.onSalaireChange(4200);
    component.onDateRuptureChange('2026-05-01');
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceDateRupture()).toBeNull();

    const newAi = {
      salaireBrutMensuel: 2800,
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    // Saisie avocat préservée — provenance===null bloque la ré-application.
    expect(component.salaireMensuelBrutEur()).toBe(4200);
    expect(component.dateRupture()).toBe('2026-05-01');
  });

  // ---------------------------------------------------------------------------
  // sourceDureeLabel + toggleCollapse + editMode (non-régression)
  // ---------------------------------------------------------------------------

  it('sourceDureeLabel renvoie un libellé lisible pour chaque source', () => {
    expect(component.sourceDureeLabel('LEGALE')).toContain('Légale');
    expect(component.sourceDureeLabel('CCN')).toContain('Convention');
    expect(component.sourceDureeLabel('USAGE')).toContain('Usage');
  });

  it('toggleCollapse fonctionne (non-régression)', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode ré-affiche le form (non-régression)', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // SF-DT-25-02 : alertBadgeLabel + alertTooltip helpers
  // ---------------------------------------------------------------------------

  it('alertBadgeLabel et alertTooltip fonctionnent avec une alerte IA simple', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    flushConventions();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    flushSourceExplanations();
    component.onSalaireChange(5000);
    const alert = component.coherenceAlerts().SALAIRE!;

    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
    expect(alert.contributors).toEqual(['IA']);
    expect(alert.severity).toBe('WARNING');
  });
});
