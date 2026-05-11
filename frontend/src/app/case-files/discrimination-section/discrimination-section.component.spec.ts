import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DiscriminationService } from '../../core/services/discrimination.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { DiscriminationSectionComponent } from './discrimination-section.component';
import { DiscriminationResponse } from '../../core/models/discrimination.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DiscriminationSectionComponent', () => {
  let component: DiscriminationSectionComponent;
  let fixture: ComponentFixture<DiscriminationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/discrimination-dommages-interets';

  function frResponse(): DiscriminationResponse {
    return {
      caseFileId: 'case-1',
      salaireMensuelReference: 3000,
      motifDiscrimination: 'SEXE_GROSSESSE',
      contexteActe: 'LICENCIEMENT',
      country: 'FRANCE',
      fourchetteMin: 18000,
      fourchetteMediane: 27000,
      fourchetteMax: 36000,
      formule: 'Salaire 3 000,00 € × [6,00 ; 12,00] mois (motif SEXE_GROSSESSE, contexte LICENCIEMENT) = [18 000,00 € ; 36 000,00 €], médiane 27 000,00 €',
      baseJuridique: 'Art. L.1132-1 + L.1134-5 Code du travail',
      messages: ['Régime probatoire L.1134-1…', 'Prescription 5 ans…'],
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        DiscriminationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DiscriminationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Structure & enums
  // ---------------------------------------------------------------------------

  it('FRANCE → 8 motifs disponibles (incluant SEXE_GROSSESSE et AGE)', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.motifsDisponibles().length).toBe(8);
    const codes = component.motifsDisponibles().map((m) => m.code);
    expect(codes).toContain('SEXE_GROSSESSE');
    expect(codes).toContain('AGE');
    expect(codes).toContain('ORIGINE_ETHNIQUE');
  });

  it('BELGIQUE → 5 motifs disponibles (incluant DISCRIMINATION_GENRE_BE)', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.motifsDisponibles().length).toBe(5);
    const codes = component.motifsDisponibles().map((m) => m.code);
    expect(codes).toContain('DISCRIMINATION_GENRE_BE');
    expect(codes).toContain('DISCRIMINATION_AGE_BE');
  });

  it('6 contextes d\'acte toujours disponibles (communs FR/BE)', () => {
    expect(component.contextesDisponibles.length).toBe(6);
    const codes = component.contextesDisponibles.map((c) => c.code);
    expect(codes).toContain('LICENCIEMENT');
    expect(codes).toContain('DIFFERENCE_SALARIALE');
    expect(codes).toContain('HARCELEMENT_LIE_DISCRIMINATION');
  });

  // ---------------------------------------------------------------------------
  // UI state — collapse / editMode
  // ---------------------------------------------------------------------------

  it('toggleCollapse() bascule collapsed', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode() ré-affiche le formulaire', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // GET (load) — 200 vs 404
  // ---------------------------------------------------------------------------

  it('charge l\'analyse existante si présente (GET 200) — valeurs persistées, pas de badge IA', () => {
    component.aiData = { salaireBrutMensuel: 9999 } as TravailExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()!.fourchetteMediane).toBe(27000);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.motifDiscrimination()).toBe('SEXE_GROSSESSE');
    expect(component.contexteActe()).toBe('LICENCIEMENT');
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('reste en mode formulaire si GET 404 + aiData absent', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.motifDiscrimination()).toBeNull();
    expect(component.contexteActe()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid() — 3 champs requis
  // ---------------------------------------------------------------------------

  it('formValid false si un champ manque ; true si les 3 sont remplis', () => {
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.motifDiscrimination.set('SEXE_GROSSESSE');
    expect(component.formValid()).toBe(false); // contexte null

    component.contexteActe.set('LICENCIEMENT');
    expect(component.formValid()).toBe(true);

    component.salaireMensuelReference.set(0);
    expect(component.formValid()).toBe(false); // salaire ≤ 0
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.salaireMensuelReference.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // POST — succès + erreur
  // ---------------------------------------------------------------------------

  it('calculate() POST → result + snackbar vert + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.salaireMensuelReference.set(3000);
    component.motifDiscrimination.set('SEXE_GROSSESSE');
    component.contexteActe.set('LICENCIEMENT');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      salaireMensuelReference: 3000,
      motifDiscrimination: 'SEXE_GROSSESSE',
      contexteActe: 'LICENCIEMENT',
    });
    req.flush(frResponse());

    expect(component.result()!.fourchetteMediane).toBe(27000);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Fourchette indicative calculée', 'OK', jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 (motif FR sur BE) → snackbar rouge', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.salaireMensuelReference.set(3000);
    component.motifDiscrimination.set('SEXE_GROSSESSE');
    component.contexteActe.set('LICENCIEMENT');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush(
      { message: "Motif FR 'SEXE_GROSSESSE' incompatible avec workspace BELGIQUE" },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(refreshSpy.triggerRefresh).not.toHaveBeenCalled();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA — SF-DT-12-02 (palier 1 : salaire uniquement)
  // ---------------------------------------------------------------------------

  it('prefill IA salaire (GET 404) → valeur + badge IA', () => {
    component.aiData = { salaireBrutMensuel: 3200 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.salaireMensuelReference()).toBe(3200);
    expect(component.provenanceSalaire()).toBe('IA');
    // Motif et contexte ne sont PAS pré-remplissables (palier 1).
    expect(component.motifDiscrimination()).toBeNull();
    expect(component.contexteActe()).toBeNull();
  });

  it('prefill salaire ≤ 0 → pas de pré-fill', () => {
    component.aiData = { salaireBrutMensuel: 0 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onSalaireChange efface le badge IA salaire', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(3500);
    expect(component.salaireMensuelReference()).toBe(3500);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Alertes F-IA-03 (divergence salaire)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE présent si divergence > 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // IA 3000 vs avocat 5000 → écart 66 %.
    component.onSalaireChange(5000);

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.field).toBe('SALAIRE');
    expect(alerts.SALAIRE!.source).toBe('IA');
    expect(alerts.SALAIRE!.contributors).toEqual(['IA']);
    expect(alerts.SALAIRE!.severity).toBe('WARNING');
    expect(alerts.SALAIRE!.expectedDisplay).toContain('€');
    expect(alerts.SALAIRE!.reason).toContain('Analyse du dossier');
  });

  it('coherenceAlerts.SALAIRE absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // IA 3000 vs avocat 3100 → écart 3.3 %.
    component.onSalaireChange(3100);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onSalaireChange(5000); // divergence
    expect(component.coherenceAlerts().SALAIRE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('ngOnChanges(aiData) post-mount applique le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.salaireMensuelReference()).toBeNull();

    const newAi = { salaireBrutMensuel: 2800 } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = { aiData: new SimpleChange(null, newAi, false) };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelReference()).toBe(2800);
    expect(component.provenanceSalaire()).toBe('IA');
  });

  it('ngOnChanges(aiData) n\'écrase pas la saisie avocat manuelle', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onSalaireChange(4200); // saisie avocat
    expect(component.provenanceSalaire()).toBeNull();

    const newAi = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });
    // Saisie avocat préservée.
    expect(component.salaireMensuelReference()).toBe(4200);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('PIECE_MANQUANTE salaire → enrichit l\'alerte avec contributor + pieceTexte', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.piecesManquantes = [
      { texte: 'Fiche de paie du mois de référence', critereCode: 'SALAIRE_BRUT_MENSUEL' },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onSalaireChange(5000); // divergence ancrée sur IA

    const alert = component.coherenceAlerts().SALAIRE!;
    expect(alert).toBeDefined();
    expect(alert.contributors).toContain('IA');
    expect(alert.contributors).toContain('PIECE_MANQUANTE');
    expect(alert.source).toBe('MULTI');
    expect(alert.pieceTexte).toBe('Fiche de paie du mois de référence');
  });

  it('salaireEstDeduit=true → note déduction exposée', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
      salaireEstDeduit: true,
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.salaireEstDeduit()).toBe(true);
  });

  it('alertBadgeLabel et alertTooltip compatibles CoherenceAlert<F>', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onSalaireChange(5000);
    const alert = component.coherenceAlerts().SALAIRE!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02b — mode standalone (CA-08, CA-09, CA-10).
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02b — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-DT-12-discrimination-dommages-interets/calculate';

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
      expect(DiscriminationService.STANDALONE_TOOL_ID).toBe('F-DT-12-discrimination-dommages-interets');
      expect(STANDALONE_URL).toContain(DiscriminationService.STANDALONE_TOOL_ID);
    });
  });

});
