import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { HarcelementLicenciementNulSectionComponent } from './harcelement-licenciement-nul-section.component';
import { HarcelementNulliteResponse } from '../../core/models/harcelement-nullite.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('HarcelementLicenciementNulSectionComponent', () => {
  let component: HarcelementLicenciementNulSectionComponent;
  let fixture: ComponentFixture<HarcelementLicenciementNulSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/harcelement-licenciement-nul';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function frResponse(): HarcelementNulliteResponse {
    return {
      caseFileId: 'case-1',
      salaireMensuelReference: 3000,
      motifNullite: 'HARCELEMENT_MORAL',
      country: 'FRANCE',
      indemniteMinimumNullite: 18000,
      formule: '6 × 3 000,00 € = 18 000,00 €',
      baseJuridique: 'Art. L1235-3-1 Code du travail',
      messages: ['Minimum 6 mois de salaire'],
    };
  }

  /**
   * SF-155-04-A1 : absorbe la requête source-explanations émise par ngOnInit
   * (fail-open si absente mais pas si non matchée). Empêche les tests de
   * crash sur httpMock.verify().
   */
  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        HarcelementLicenciementNulSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(HarcelementLicenciementNulSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Tests existants (non-régression) — SF-DT-11-02.
  // ---------------------------------------------------------------------------

  it('FRANCE → 8 motifs disponibles', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.motifsDisponibles().length).toBe(8);
    const codes = component.motifsDisponibles().map((m) => m.code);
    expect(codes).toContain('HARCELEMENT_MORAL');
    expect(codes).toContain('ALERTE_ETHIQUE');
  });

  it('BELGIQUE → 4 motifs disponibles', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.motifsDisponibles().length).toBe(4);
    const codes = component.motifsDisponibles().map((m) => m.code);
    expect(codes).toContain('HARCELEMENT_MORAL_BE');
    expect(codes).toContain('DISCRIMINATION_BE');
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expectSourceExplanationCall();
    expect(component.result()!.indemniteMinimumNullite).toBe(18000);
    expect(component.showForm()).toBe(false);
    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.motifNullite()).toBe('HARCELEMENT_MORAL');
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false si salaire manquant, motif manquant ou salaire ≤ 0', () => {
    component.salaireMensuelReference.set(null);
    component.motifNullite.set('HARCELEMENT_MORAL');
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.motifNullite.set(null);
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(0);
    component.motifNullite.set('HARCELEMENT_MORAL');
    expect(component.formValid()).toBe(false);

    component.salaireMensuelReference.set(3000);
    component.motifNullite.set('HARCELEMENT_MORAL');
    expect(component.formValid()).toBe(true);
  });

  it('calculate() POST + affiche résultat + snackbar succès', () => {
    component.salaireMensuelReference.set(3000);
    component.motifNullite.set('HARCELEMENT_MORAL');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      salaireMensuelReference: 3000,
      motifNullite: 'HARCELEMENT_MORAL',
    });
    req.flush(frResponse());

    expect(component.result()!.indemniteMinimumNullite).toBe(18000);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Indemnité calculée', 'OK', jasmine.any(Object));
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.salaireMensuelReference.set(3000);
    component.motifNullite.set('HARCELEMENT_MORAL');
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
    component.salaireMensuelReference.set(null);
    component.motifNullite.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Tests SF-155-04-A1 — pré-fill IA + validation F-IA-03 (nouveaux).
  // ---------------------------------------------------------------------------

  it('SF-155-04-A1 : prefill IA complet (salaire + motif mappable) + GET 404 → valeurs + badges IA', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
      motifNullitePressenti: 'HARCELEMENT_MORAL',
    } as TravailExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.motifNullite()).toBe('HARCELEMENT_MORAL');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceMotifNullite()).toBe('IA');
  });

  it('SF-155-04-A1 : prefill sans aiData → pas de pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.motifNullite()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceMotifNullite()).toBeNull();
  });

  it('SF-155-04-A1 : motifNullitePressenti hors mapping (RETORSION) → motif non pré-rempli', () => {
    component.aiData = {
      salaireBrutMensuel: 2500,
      motifNullitePressenti: 'RETORSION',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBe(2500);
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.motifNullite()).toBeNull();
    expect(component.provenanceMotifNullite()).toBeNull();
  });

  it('SF-155-04-A1 : salaireBrutMensuel ≤ 0 → pas de pré-fill salaire', () => {
    component.aiData = {
      salaireBrutMensuel: 0,
      motifNullitePressenti: 'DISCRIMINATION',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
    // motif mappable → pré-rempli.
    expect(component.motifNullite()).toBe('DISCRIMINATION');
    expect(component.provenanceMotifNullite()).toBe('IA');
  });

  it('SF-155-04-A1 : onSalaireChange efface le badge IA salaire', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(3500);
    expect(component.salaireMensuelReference()).toBe(3500);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('SF-155-04-A1 : onMotifNulliteChange efface le badge IA motif', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
      motifNullitePressenti: 'HARCELEMENT_MORAL',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceMotifNullite()).toBe('IA');
    component.onMotifNulliteChange('DISCRIMINATION');
    expect(component.motifNullite()).toBe('DISCRIMINATION');
    expect(component.provenanceMotifNullite()).toBeNull();
  });

  it('SF-155-04-A1 : loadExisting (GET 200) → pas de badge IA (valeurs persistées prioritaires)', () => {
    component.aiData = {
      salaireBrutMensuel: 9999, // aiData divergent — ne doit pas écraser
      motifNullitePressenti: 'DISCRIMINATION',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expectSourceExplanationCall();

    // Valeurs persistées prioritaires sur aiData.
    expect(component.salaireMensuelReference()).toBe(3000);
    expect(component.motifNullite()).toBe('HARCELEMENT_MORAL');
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceMotifNullite()).toBeNull();
  });

  it('SF-155-04-A1 : coherenceAlerts.SALAIRE présent si divergence > 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // Salaire IA = 3000, avocat saisit 5000 → écart 66 %.
    component.onSalaireChange(5000);

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.field).toBe('SALAIRE');
    expect(alerts.SALAIRE!.source).toBe('IA');
    expect(alerts.SALAIRE!.expectedDisplay).toContain('3');
  });

  it('SF-155-04-A1 : coherenceAlerts.SALAIRE absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // Salaire IA = 3000, avocat saisit 3100 → écart 3.3 %.
    component.onSalaireChange(3100);

    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('SF-155-04-A1 : coherenceAlerts.MOTIF_NULLITE présent si divergence mapping IA vs saisie', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
      motifNullitePressenti: 'DISCRIMINATION',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // Pré-fill a mis motif='DISCRIMINATION'. L'avocat change en HARCELEMENT_MORAL.
    component.onMotifNulliteChange('HARCELEMENT_MORAL');

    const alerts = component.coherenceAlerts();
    expect(alerts.MOTIF_NULLITE).toBeDefined();
    expect(alerts.MOTIF_NULLITE!.field).toBe('MOTIF_NULLITE');
    expect(alerts.MOTIF_NULLITE!.expectedDisplay).toBe('Discrimination');
  });

  it('SF-155-04-A1 : ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Nouvel aiData arrive tardivement (pipeline IA plus rapide que le mount).
    const newAi = {
      salaireBrutMensuel: 2800,
      motifNullitePressenti: 'HARCELEMENT_SEXUEL',
    } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = {
      aiData: new SimpleChange(null, newAi, false),
    };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelReference()).toBe(2800);
    expect(component.motifNullite()).toBe('HARCELEMENT_SEXUEL');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceMotifNullite()).toBe('IA');
  });

  it('SF-155-04-A1 : ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // Avocat saisit manuellement d'abord.
    component.onSalaireChange(4200);
    component.onMotifNulliteChange('LIBERTE_FONDAMENTALE');
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceMotifNullite()).toBeNull();

    // aiData arrive ensuite.
    const newAi = {
      salaireBrutMensuel: 3000,
      motifNullitePressenti: 'HARCELEMENT_MORAL',
    } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    // Saisie avocat préservée (pas d'écrasement — seule la 1re résolution écrase
    // les null). Contrat : provenance===null sur salaire → pas de ré-application.
    expect(component.salaireMensuelReference()).toBe(4200);
    expect(component.motifNullite()).toBe('LIBERTE_FONDAMENTALE');
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceMotifNullite()).toBeNull();
  });

  it('SF-155-04-A1 : workspaceCountry BELGIQUE bloque le pré-fill motif (salaire OK)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = {
      salaireBrutMensuel: 2800,
      motifNullitePressenti: 'HARCELEMENT_MORAL',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelReference()).toBe(2800);
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.motifNullite()).toBeNull();
    expect(component.provenanceMotifNullite()).toBeNull();
  });

  it('SF-155-04-A1 : salaireEstDeduit=true → note déduction exposée', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
      salaireEstDeduit: true,
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireEstDeduit()).toBe(true);
  });

  it('SF-155-04-A1 : mapping MATERNITE_PATERNITE → GROSSESSE', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
      motifNullitePressenti: 'MATERNITE_PATERNITE',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.motifNullite()).toBe('GROSSESSE');
    expect(component.provenanceMotifNullite()).toBe('IA');
  });

  it('SF-155-04-A1 : alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
      motifNullitePressenti: 'DISCRIMINATION',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(5000); // divergent vs IA
    expect(component.coherenceAlerts().SALAIRE).toBeDefined();

    // Après calcul, form masqué → alertes doivent disparaître.
    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('SF-155-04-A1 : toggleCollapse fonctionne (non-régression)', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('SF-155-04-A1 : editMode ré-affiche le form (non-régression)', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});
