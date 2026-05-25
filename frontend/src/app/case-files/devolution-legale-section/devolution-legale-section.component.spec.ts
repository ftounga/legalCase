import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { DevolutionLegaleSectionComponent } from './devolution-legale-section.component';
import { DevolutionLegaleResponse } from '../../core/models/devolution-legale.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('DevolutionLegaleSectionComponent', () => {
  let component: DevolutionLegaleSectionComponent;
  let fixture: ComponentFixture<DevolutionLegaleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/devolution-legale-analysis';

  function response(overrides: Partial<DevolutionLegaleResponse> = {}): DevolutionLegaleResponse {
    return {
      caseFileId: 'case-1',
      ordreActif: 'DESCENDANTS',
      heritiersDesignes: [
        { qualite: 'CONJOINT', ordre: 0, quotePartPct: 25.0, modalite: 'PLEINE_PROPRIETE' },
        { qualite: 'DESCENDANT', ordre: 1, quotePartPct: 37.5, modalite: 'PLEINE_PROPRIETE' },
        { qualite: 'DESCENDANT', ordre: 1, quotePartPct: 37.5, modalite: 'PLEINE_PROPRIETE' },
      ],
      quotePartConjoint: 25.0,
      modaliteConjoint: 'PLEINE_PROPRIETE',
      representationActive: false,
      fenteApplicable: false,
      scoreEligibilite: 90,
      risquesContentieux: [],
      baseJuridique: 'Art. 731, 734, 738, 745, 746, 747-749, 751-755, 757 et s. Cciv',
      formule: 'Ordre DESCENDANTS actif + conjoint QUART + 2 descendants tous communs',
      messages: ['Conjoint survivant + descendants tous communs : option ¼ retenue'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        DevolutionLegaleSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(DevolutionLegaleSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('FRANCE → isFrance() true, GET appelé au ngOnInit', () => {
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

  it('charge le résultat existant si GET 200 (mode résultat hydraté)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.ordreActif).toBe('DESCENDANTS');
    expect(component.result()!.heritiersDesignes.length).toBe(3);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceConjoint()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('pré-fill IA : conjointSurvivant ← aiData.conjointSurvivantDetected', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.conjointSurvivant()).toBe(true);
    expect(component.provenanceConjoint()).toBe('IA');
  });

  it('pré-fill IA : nbDescendants + tousCommuns + nbFreresSoeurs', () => {
    component.aiData = {
      nbDescendantsDetecte: 2,
      tousDescendantsCommunsAvecConjointDetected: true,
      nbFreresSoeursDetecte: 3,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.nbDescendants()).toBe(2);
    expect(component.provenanceNbDescendants()).toBe('IA');
    expect(component.tousDescendantsCommunsAvecConjoint()).toBe(true);
    expect(component.provenanceTousCommuns()).toBe('IA');
    expect(component.nbFreresSoeurs()).toBe(3);
    expect(component.provenanceNbFreresSoeurs()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.conjointSurvivant()).toBeNull();
    expect(component.nbDescendants()).toBeNull();
    expect(component.provenanceConjoint()).toBeNull();
  });

  it('onConjointSurvivantChange efface le badge IA et reset les champs dépendants si Non', () => {
    component.aiData = {
      conjointSurvivantDetected: true,
      tousDescendantsCommunsAvecConjointDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceConjoint()).toBe('IA');
    expect(component.tousDescendantsCommunsAvecConjoint()).toBe(true);

    component.onConjointSurvivantChange(false);
    expect(component.conjointSurvivant()).toBe(false);
    expect(component.provenanceConjoint()).toBeNull();
    // Champs dépendants reset
    expect(component.tousDescendantsCommunsAvecConjoint()).toBeNull();
    expect(component.optionConjoint()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid : Ordre 1 — descendants seuls (3 enfants, pas de conjoint)', () => {
    component.conjointSurvivant.set(false);
    component.nbDescendants.set(3);
    component.pereVivant.set(false);
    component.mereVivant.set(false);
    component.nbFreresSoeurs.set(0);
    component.ascendantsOrdinaires.set(false);
    component.collateralOrdinaires.set(false);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : Ordre 1 — descendants tous communs + conjoint requiert optionConjoint', () => {
    component.conjointSurvivant.set(true);
    component.nbDescendants.set(2);
    component.tousDescendantsCommunsAvecConjoint.set(true);
    component.pereVivant.set(false);
    component.mereVivant.set(false);
    component.nbFreresSoeurs.set(0);
    component.ascendantsOrdinaires.set(false);
    component.collateralOrdinaires.set(false);

    // Sans option conjoint → invalide
    expect(component.optionConjointRequired()).toBe(true);
    expect(component.formValid()).toBe(false);

    // Avec option QUART → valide
    component.onOptionConjointChange('QUART');
    expect(component.formValid()).toBe(true);
  });

  it('formValid : Ordre 1 — descendants non communs, optionConjoint pas requise', () => {
    component.conjointSurvivant.set(true);
    component.nbDescendants.set(2);
    component.tousDescendantsCommunsAvecConjoint.set(false);
    component.pereVivant.set(false);
    component.mereVivant.set(false);
    component.nbFreresSoeurs.set(0);
    component.ascendantsOrdinaires.set(false);
    component.collateralOrdinaires.set(false);

    expect(component.optionConjointRequired()).toBe(false);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : Ordre 2 — pas de descendants, parents + frères + conjoint', () => {
    component.conjointSurvivant.set(true);
    component.nbDescendants.set(0);
    component.pereVivant.set(true);
    component.mereVivant.set(true);
    component.nbFreresSoeurs.set(2);
    component.ascendantsOrdinaires.set(false);
    component.collateralOrdinaires.set(false);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : Ordre 3 — ascendants ordinaires seuls (fente)', () => {
    component.conjointSurvivant.set(false);
    component.nbDescendants.set(0);
    component.pereVivant.set(false);
    component.mereVivant.set(false);
    component.nbFreresSoeurs.set(0);
    component.ascendantsOrdinaires.set(true);
    component.collateralOrdinaires.set(false);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : Ordre 4 — collatéraux ordinaires seuls', () => {
    component.conjointSurvivant.set(false);
    component.nbDescendants.set(0);
    component.pereVivant.set(false);
    component.mereVivant.set(false);
    component.nbFreresSoeurs.set(0);
    component.ascendantsOrdinaires.set(false);
    component.collateralOrdinaires.set(true);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : nbFreresSoeursPredecedes > nbFreresSoeurs invalide', () => {
    component.conjointSurvivant.set(true);
    component.nbDescendants.set(0);
    component.pereVivant.set(false);
    component.mereVivant.set(false);
    component.nbFreresSoeurs.set(2);
    component.nbFreresSoeursPredecedes.set(3);
    component.ascendantsOrdinaires.set(false);
    component.collateralOrdinaires.set(false);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST envoie le body attendu + résultat + snackbar succès', () => {
    component.conjointSurvivant.set(true);
    component.nbDescendants.set(2);
    component.tousDescendantsCommunsAvecConjoint.set(true);
    component.optionConjoint.set('QUART');
    component.pereVivant.set(false);
    component.mereVivant.set(false);
    component.nbFreresSoeurs.set(0);
    component.ascendantsOrdinaires.set(false);
    component.collateralOrdinaires.set(false);

    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.conjointSurvivant).toBe(true);
    expect(req.request.body.nbDescendants).toBe(2);
    expect(req.request.body.tousDescendantsCommunsAvecConjoint).toBe(true);
    expect(req.request.body.optionConjoint).toBe('QUART');
    expect(req.request.body.pereVivant).toBe(false);
    expect(req.request.body.nbFreresSoeurs).toBe(0);
    req.flush(response());

    expect(component.result()!.ordreActif).toBe('DESCENDANTS');
    expect(component.result()!.heritiersDesignes.length).toBe(3);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Dévolution légale analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.conjointSurvivant.set(false);
    component.nbDescendants.set(3);
    component.pereVivant.set(false);
    component.mereVivant.set(false);
    component.nbFreresSoeurs.set(0);
    component.ascendantsOrdinaires.set(false);
    component.collateralOrdinaires.set(false);

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

  // ============================================================
  // F-IA-03 alertes de cohérence
  // ============================================================

  it('coherenceAlerts.CONJOINT présent si IA divergente de la saisie', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit `true` puis avocat saisit `false`.
    component.onConjointSurvivantChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.CONJOINT).toBeDefined();
    expect(alerts.CONJOINT!.field).toBe('CONJOINT');
    expect(alerts.CONJOINT!.source).toBe('IA');
    expect(alerts.CONJOINT!.expectedDisplay).toContain('Conjoint survivant');
  });

  it('coherenceAlerts.CONJOINT absent si IA convergent avec saisie', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.conjointSurvivant()).toBe(true);
    expect(component.coherenceAlerts().CONJOINT).toBeUndefined();
  });

  it('coherenceAlerts.DESCENDANTS_COMMUNS indépendant de CONJOINT', () => {
    component.aiData = {
      conjointSurvivantDetected: true,
      tousDescendantsCommunsAvecConjointDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Avocat conserve conjoint=true mais retourne tousCommuns=false.
    component.onTousCommunsChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.CONJOINT).toBeUndefined();
    expect(alerts.DESCENDANTS_COMMUNS).toBeDefined();
    expect(alerts.DESCENDANTS_COMMUNS!.source).toBe('IA');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onConjointSurvivantChange(false);
    expect(component.coherenceAlerts().CONJOINT).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().CONJOINT).toBeUndefined();
  });

  it('coherenceAlerts.CONJOINT multi-sources F96 + IA → MULTI', () => {
    component.aiData = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Conjoint survivant',
        statut: 'NON_COMPLIANT',
        critereCode: 'DEVOLUTION_LEGALE_CONJOINT',
        expectedValue: 'OUI',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit true puis avocat dit false → divergence sur les 2 sources.
    component.onConjointSurvivantChange(false);

    const alert = component.coherenceAlerts().CONJOINT;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.conjointSurvivant()).toBe(true);
    expect(component.provenanceConjoint()).toBe('IA');
  });

  it('ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onConjointSurvivantChange(false);
    expect(component.provenanceConjoint()).toBeNull();

    const newAi = { conjointSurvivantDetected: true } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.conjointSurvivant()).toBe(false);
    expect(component.provenanceConjoint()).toBeNull();
  });

  // ============================================================
  // Helpers UI
  // ============================================================

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

  it('ordreLabel + ordreNumero couvrent les 6 valeurs OrdreActif', () => {
    expect(component.ordreLabel('DESCENDANTS')).toContain('Ordre 1');
    expect(component.ordreLabel('PRIVILEGIES')).toContain('Ordre 2');
    expect(component.ordreLabel('ASCENDANTS_ORDINAIRES')).toContain('Ordre 3');
    expect(component.ordreLabel('COLLATERAUX_ORDINAIRES')).toContain('Ordre 4');
    expect(component.ordreLabel('CONJOINT_SEUL')).toContain('Conjoint seul');
    expect(component.ordreLabel('DESHERENCE')).toContain('déshérence');
    expect(component.ordreLabel(null)).toBe('');

    expect(component.ordreNumero('DESCENDANTS')).toBe('1');
    expect(component.ordreNumero('PRIVILEGIES')).toBe('2');
    expect(component.ordreNumero('ASCENDANTS_ORDINAIRES')).toBe('3');
    expect(component.ordreNumero('COLLATERAUX_ORDINAIRES')).toBe('4');
    expect(component.ordreNumero('CONJOINT_SEUL')).toBe('C');
    expect(component.ordreNumero('DESHERENCE')).toBe('∅');
  });

  it('modaliteLabel couvre les 3 modalités', () => {
    expect(component.modaliteLabel('PLEINE_PROPRIETE')).toBe('Pleine propriété');
    expect(component.modaliteLabel('USUFRUIT')).toBe('Usufruit');
    expect(component.modaliteLabel('NUE_PROPRIETE')).toBe('Nue-propriété');
    expect(component.modaliteLabel(null)).toBe('');
  });

  it('qualiteLabel couvre les qualités principales', () => {
    expect(component.qualiteLabel('CONJOINT')).toBe('Conjoint');
    expect(component.qualiteLabel('DESCENDANT')).toBe('Descendant');
    expect(component.qualiteLabel('PERE')).toBe('Père');
    expect(component.qualiteLabel('MERE')).toBe('Mère');
    expect(component.qualiteLabel('FRERE_SOEUR')).toBe('Frère/Sœur');
    expect(component.qualiteLabel('REPRESENTANT')).toBe('Représentant');
  });

  it('ordreChipClass marque DESHERENCE comme critique', () => {
    expect(component.ordreChipClass('DESHERENCE')).toContain('critical');
    expect(component.ordreChipClass('DESCENDANTS')).toContain('info');
    expect(component.ordreChipClass(null)).toContain('info');
  });

  it('onNbDescendantsChange à 0 reset les champs dépendants', () => {
    component.nbDescendants.set(2);
    component.tousDescendantsCommunsAvecConjoint.set(true);
    component.optionConjoint.set('QUART');
    component.nbDescendantsPredecedes.set(1);
    component.nbPetitsEnfantsParRepresentation.set(2);

    component.onNbDescendantsChange(0);

    expect(component.nbDescendants()).toBe(0);
    expect(component.tousDescendantsCommunsAvecConjoint()).toBeNull();
    expect(component.optionConjoint()).toBeNull();
    expect(component.nbDescendantsPredecedes()).toBe(0);
    expect(component.nbPetitsEnfantsParRepresentation()).toBe(0);
  });
});
