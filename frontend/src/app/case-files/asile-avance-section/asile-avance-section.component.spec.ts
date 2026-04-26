import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AsileAvanceSectionComponent } from './asile-avance-section.component';
import { AsileAvanceResponse } from '../../core/models/asile-avance.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('AsileAvanceSectionComponent (SF-IM-12-02)', () => {
  let component: AsileAvanceSectionComponent;
  let fixture: ComponentFixture<AsileAvanceSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/asile-avance-analysis';

  function response(overrides: Partial<AsileAvanceResponse> = {}): AsileAvanceResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      dispositifAsile: 'DUBLIN_III',
      dispositifLibelle: 'Procédure Dublin III (Règl. UE 604/2013)',
      verdictRecevabilite: 'RECEVABLE_TRANSFERT',
      delaiInstructionMois: 6,
      recoursPossible: 'Recours suspensif 15 jours devant le TA contre la décision de transfert (CESEDA L.572-4).',
      documentsRequis: [
        'Récépissé de demande d\'asile en France',
        'Relevé EURODAC (empreintes)',
      ],
      risqueRefus: [
        'Recours suspensif 15 jours devant le TA contre la décision de transfert.',
      ],
      baseJuridique: 'Règlement UE 604/2013 (Dublin III)',
      formule: 'Asile / Dublin III — État membre responsable saisi, transfert probable (délai 6 mois).',
      messages: ['Empreintes EURODAC trouvées dans un autre EM — saisine de l\'État responsable.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        AsileAvanceSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AsileAvanceSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // 1. Mount + lifecycle
  // ---------------------------------------------------------------------------

  it('FRANCE → isFrance() true, GET émis au ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('BELGIQUE → isFrance() false, aucun appel HTTP au ngOnInit (gate pays)', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.isFrance()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('GET 200 → résultat hydraté + dispositif persisté + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('RECEVABLE_TRANSFERT');
    expect(component.dispositifAsile()).toBe('DUBLIN_III');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('GET 404 → mode formulaire (showForm=true)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 2. Pré-fill IA (RÈGLE FONDAMENTALE)
  // ---------------------------------------------------------------------------

  it('pré-fill IA : ASILE_DUBLIN_III → DUBLIN_III + provenance IA', () => {
    component.aiData = { typeProcedureDetectee: 'ASILE_DUBLIN_III' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dispositifAsile()).toBe('DUBLIN_III');
    expect(component.provenanceDispositif()).toBe('IA');
  });

  it('pré-fill IA : aiData absent → no-op gracieux', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dispositifAsile()).toBeNull();
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('pré-fill IA : typeProcedureDetectee non asile → no-op', () => {
    component.aiData = { typeProcedureDetectee: 'NATURALISATION_DECRET' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dispositifAsile()).toBeNull();
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('pré-fill IA : ASILE_REEXAMEN → REEXAMEN + provenance IA', () => {
    component.aiData = { typeProcedureDetectee: 'ASILE_REEXAMEN' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dispositifAsile()).toBe('REEXAMEN');
    expect(component.provenanceDispositif()).toBe('IA');
  });

  it('onDispositifChange efface le badge IA', () => {
    component.provenanceDispositif.set('IA');
    component.onDispositifChange('APATRIDIE');
    expect(component.dispositifAsile()).toBe('APATRIDIE');
    expect(component.provenanceDispositif()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 3. Form validation + champs conditionnels
  // ---------------------------------------------------------------------------

  it('formValid false initialement (aucun dispositif sélectionné)', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true pour DUBLIN_III dès que dispositif choisi', () => {
    component.dispositifAsile.set('DUBLIN_III');
    expect(component.formValid()).toBe(true);
  });

  it('formValid pour REEXAMEN exige une date de décision antérieure', () => {
    component.dispositifAsile.set('REEXAMEN');
    expect(component.formValid()).toBe(false);
    component.dateDecisionAnterieure.set('2024-05-12');
    expect(component.formValid()).toBe(true);
  });

  it('formValid true pour APATRIDIE / PROTECTION_SUBSIDIAIRE / PROCEDURE_ACCELEREE sans champ requis', () => {
    component.dispositifAsile.set('APATRIDIE');
    expect(component.formValid()).toBe(true);
    component.dispositifAsile.set('PROTECTION_SUBSIDIAIRE');
    expect(component.formValid()).toBe(true);
    component.dispositifAsile.set('PROCEDURE_ACCELEREE');
    expect(component.formValid()).toBe(true);
  });

  it('champs conditionnels : showXxxFields selon dispositif', () => {
    component.dispositifAsile.set('DUBLIN_III');
    expect(component.showDublinFields()).toBe(true);
    expect(component.showAccelereeFields()).toBe(false);
    expect(component.showReexamenFields()).toBe(false);
    expect(component.showApatridieFields()).toBe(false);
    expect(component.showProtectionSubFields()).toBe(false);

    component.dispositifAsile.set('PROCEDURE_ACCELEREE');
    expect(component.showAccelereeFields()).toBe(true);
    expect(component.showDublinFields()).toBe(false);

    component.dispositifAsile.set('REEXAMEN');
    expect(component.showReexamenFields()).toBe(true);

    component.dispositifAsile.set('APATRIDIE');
    expect(component.showApatridieFields()).toBe(true);

    component.dispositifAsile.set('PROTECTION_SUBSIDIAIRE');
    expect(component.showProtectionSubFields()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // 4. Calculate / submit
  // ---------------------------------------------------------------------------

  it('calculate() POST DUBLIN_III avec champs ciblés + résultat + snackbar succès', () => {
    component.dispositifAsile.set('DUBLIN_III');
    component.empreintesEurodacAutresEm.set(true);
    component.demandeurEnFuite.set(false);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositifAsile).toBe('DUBLIN_III');
    expect(req.request.body.empreintesEurodacAutresEm).toBe(true);
    expect(req.request.body.demandeurEnFuite).toBe(false);
    // Champs d'autres dispositifs ne doivent pas être envoyés.
    expect(req.request.body.elementsNouveaux).toBeUndefined();
    expect(req.request.body.paysOrigineDansListeSurs).toBeUndefined();
    expect(req.request.body.traitementsGravesEtablis).toBeUndefined();
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('RECEVABLE_TRANSFERT');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Asile avancé analysé', 'OK', jasmine.any(Object));
  });

  it('calculate() POST REEXAMEN envoie date + elementsNouveaux, pas les autres champs', () => {
    component.dispositifAsile.set('REEXAMEN');
    component.dateDecisionAnterieure.set('2024-03-01');
    component.elementsNouveaux.set(true);
    // Résidu DUBLIN — doit être ignoré.
    component.empreintesEurodacAutresEm.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositifAsile).toBe('REEXAMEN');
    expect(req.request.body.dateDecisionAnterieure).toBe('2024-03-01');
    expect(req.request.body.elementsNouveaux).toBe(true);
    expect(req.request.body.empreintesEurodacAutresEm).toBeUndefined();
    expect(req.request.body.paysOrigineDansListeSurs).toBeUndefined();
    req.flush(response({
      dispositifAsile: 'REEXAMEN',
      verdictRecevabilite: 'RECEVABLE_REEXAMEN',
      delaiInstructionMois: 0.3,
    }));
  });

  it('calculate() PROTECTION_SUBSIDIAIRE envoie traitements + motifsExclusion uniquement', () => {
    component.dispositifAsile.set('PROTECTION_SUBSIDIAIRE');
    component.traitementsGravesEtablis.set(true);
    component.motifsExclusion.set(false);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositifAsile).toBe('PROTECTION_SUBSIDIAIRE');
    expect(req.request.body.traitementsGravesEtablis).toBe(true);
    expect(req.request.body.motifsExclusion).toBe(false);
    expect(req.request.body.empreintesEurodacAutresEm).toBeUndefined();
    expect(req.request.body.dateDecisionAnterieure).toBeUndefined();
    req.flush(response({
      dispositifAsile: 'PROTECTION_SUBSIDIAIRE',
      verdictRecevabilite: 'RECEVABLE_PROTECTION_SUBSIDIAIRE',
      delaiInstructionMois: 18,
    }));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() ignoré pour REEXAMEN sans date (pas d\'appel HTTP)', () => {
    component.dispositifAsile.set('REEXAMEN');
    component.dateDecisionAnterieure.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.dispositifAsile.set('DUBLIN_III');
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

  // ---------------------------------------------------------------------------
  // 5. F-IA-03 alertes de cohérence (RÈGLE FONDAMENTALE)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts vide quand pas de dispositif sélectionné', () => {
    expect(component.coherenceAlerts().DISPOSITIF_ASILE).toBeUndefined();
  });

  it('coherenceAlerts vide quand IA convergente avec saisie', () => {
    component.aiData = { typeProcedureDetectee: 'ASILE_DUBLIN_III' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Le pré-fill IA aligne automatiquement — pas de divergence → pas d'alerte.

    expect(component.coherenceAlerts().DISPOSITIF_ASILE).toBeUndefined();
  });

  it('coherenceAlerts.DISPOSITIF_ASILE présent si IA détecte un dispositif divergent', () => {
    component.aiData = { typeProcedureDetectee: 'ASILE_DUBLIN_III' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // L'avocat change manuellement vers APATRIDIE — divergence avec l'IA.
    component.onDispositifChange('APATRIDIE');

    const alert = component.coherenceAlerts().DISPOSITIF_ASILE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DISPOSITIF_ASILE');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toContain('Dublin');
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { typeProcedureDetectee: 'ASILE_DUBLIN_III' } as ImmigrationExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Dispositif d\'asile',
        statut: 'NON_COMPLIANT',
        critereCode: 'IM12_DISPOSITIF_ASILE',
        expectedValue: 'DUBLIN_III',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDispositifChange('APATRIDIE');

    const alert = component.coherenceAlerts().DISPOSITIF_ASILE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts vidé après calcul (showForm=false)', () => {
    component.aiData = { typeProcedureDetectee: 'ASILE_DUBLIN_III' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDispositifChange('APATRIDIE');
    expect(component.coherenceAlerts().DISPOSITIF_ASILE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DISPOSITIF_ASILE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // 6. Misc
  // ---------------------------------------------------------------------------

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

  it('bannerClass mappe verdict → classe CSS attendue', () => {
    expect(component.bannerClass('RECEVABLE_TRANSFERT')).toContain('asile-banner--info');
    expect(component.bannerClass('FRANCE_COMPETENTE')).toContain('asile-banner--info');
    expect(component.bannerClass('RECEVABLE_APATRIDIE')).toContain('asile-banner--info');
    expect(component.bannerClass('ACCELEREE_APPLICABLE')).toContain('asile-banner--warning');
    expect(component.bannerClass('ACCELEREE_NON_APPLICABLE')).toContain('asile-banner--warning');
    expect(component.bannerClass('IRRECEVABLE')).toContain('asile-banner--critical');
    expect(component.bannerClass(null)).toContain('asile-banner--info');
  });

  it('bannerLabel retourne le verdict humain', () => {
    expect(component.bannerLabel('RECEVABLE_TRANSFERT')).toContain('Transfert Dublin');
    expect(component.bannerLabel('RECEVABLE_REEXAMEN')).toContain('Réexamen recevable');
    expect(component.bannerLabel('IRRECEVABLE')).toContain('bloquant');
    expect(component.bannerLabel(null)).toBe('');
  });

  it('risqueChipClass : "exclusion" / "non établi" → critical, autre → warning', () => {
    expect(component.risqueChipClass('Motifs d\'exclusion')).toContain('critical');
    expect(component.risqueChipClass('Présence non régulière en France')).toContain('critical');
    expect(component.risqueChipClass('Crainte fondée non établie')).toContain('critical');
    expect(component.risqueChipClass('Recours suspensif possible')).toContain('warning');
  });

  it('dispositifLabel retourne le libellé humain ou le code en fallback', () => {
    expect(component.dispositifLabel('DUBLIN_III')).toContain('Dublin');
    expect(component.dispositifLabel('APATRIDIE')).toContain('apatride');
    expect(component.dispositifLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.dispositifLabel(null)).toBe('');
  });
});
