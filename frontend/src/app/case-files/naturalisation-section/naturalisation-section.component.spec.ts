import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NaturalisationSectionComponent } from './naturalisation-section.component';
import { NaturalisationResponse } from '../../core/models/naturalisation.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('NaturalisationSectionComponent (SF-IM-13-02)', () => {
  let component: NaturalisationSectionComponent;
  let fixture: ComponentFixture<NaturalisationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/naturalisation-analysis';

  function response(overrides: Partial<NaturalisationResponse> = {}): NaturalisationResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      voieNaturalisation: 'DECRET',
      voieRecommandee: 'Naturalisation par décret (art. 21-15+)',
      verdictRecevabilite: 'ELEVEE',
      criteresNonRemplis: [],
      documentsAFournir: [
        'Justificatifs de résidence régulière en France',
        'Acte de naissance traduit/légalisé',
        'Bulletin n°3 du casier judiciaire',
      ],
      delaiInstructionMois: 18,
      baseJuridique: 'Code civil art. 21-15 à 21-25-1',
      formule: 'Naturalisation — décret : verdict ELEVEE.',
      messages: ['Délai d\'instruction estimatif : 18 mois.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        NaturalisationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(NaturalisationSectionComponent);
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

  it('GET 200 → résultat hydraté + voie persistée + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.voieNaturalisation()).toBe('DECRET');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceVoie()).toBeNull();
  });

  it('GET 404 → mode formulaire (showForm=true)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 2. Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA : no-op gracieux sans aiData (pas d\'erreur, pas de provenance)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.voieNaturalisation()).toBeNull();
    expect(component.provenanceVoie()).toBeNull();
  });

  it('pré-fill IA : no-op gracieux avec aiData sans champ exploitable (template canonique respecté)', () => {
    component.aiData = { typeTitreSejour: 'ETUDIANT' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Aucun champ naturalisation extractible — `prefillFromAi()` reste no-op.
    expect(component.voieNaturalisation()).toBeNull();
    expect(component.provenanceVoie()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 3. Form validation + champs conditionnels
  // ---------------------------------------------------------------------------

  it('formValid false initialement (aucune voie sélectionnée)', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true pour DECRET dès que durée résidence renseignée', () => {
    component.voieNaturalisation.set('DECRET');
    expect(component.formValid()).toBe(false);
    component.dureeResidenceReguliereAnnees.set(5);
    expect(component.formValid()).toBe(true);
    component.dureeResidenceReguliereAnnees.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid true pour MARIAGE dès que durée mariage renseignée', () => {
    component.voieNaturalisation.set('MARIAGE');
    expect(component.formValid()).toBe(false);
    component.dureeMariageAnnees.set(4);
    expect(component.formValid()).toBe(true);
  });

  it('formValid true pour ASCENDANT dès que âge + durée résidence renseignées', () => {
    component.voieNaturalisation.set('ASCENDANT');
    expect(component.formValid()).toBe(false);
    component.ageDemandeur.set(67);
    expect(component.formValid()).toBe(false);
    component.dureeResidenceReguliereAnnees.set(25);
    expect(component.formValid()).toBe(true);
  });

  it('formValid true pour MINEUR / REINTEGRATION / OPPOSITION sans champ requis', () => {
    component.voieNaturalisation.set('MINEUR');
    expect(component.formValid()).toBe(true);
    component.voieNaturalisation.set('REINTEGRATION');
    expect(component.formValid()).toBe(true);
    component.voieNaturalisation.set('OPPOSITION');
    expect(component.formValid()).toBe(true);
  });

  it('champs conditionnels : showDecretFields/showMariageFields/etc. selon voie', () => {
    component.voieNaturalisation.set('DECRET');
    expect(component.showDecretFields()).toBe(true);
    expect(component.showMariageFields()).toBe(false);

    component.voieNaturalisation.set('MARIAGE');
    expect(component.showDecretFields()).toBe(false);
    expect(component.showMariageFields()).toBe(true);

    component.voieNaturalisation.set('ASCENDANT');
    expect(component.showAscendantFields()).toBe(true);

    component.voieNaturalisation.set('MINEUR');
    expect(component.showMineurFields()).toBe(true);

    component.voieNaturalisation.set('REINTEGRATION');
    expect(component.showReintegrationFields()).toBe(true);

    component.voieNaturalisation.set('OPPOSITION');
    expect(component.showOppositionFields()).toBe(true);
  });

  it('onVoieChange efface le badge IA', () => {
    component.provenanceVoie.set('IA');
    component.onVoieChange('MARIAGE');
    expect(component.voieNaturalisation()).toBe('MARIAGE');
    expect(component.provenanceVoie()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 4. Calculate / submit
  // ---------------------------------------------------------------------------

  it('calculate() POST DECRET avec champs ciblés + résultat + snackbar succès', () => {
    component.voieNaturalisation.set('DECRET');
    component.dureeResidenceReguliereAnnees.set(5);
    component.assimilationLangueB1.set(true);
    component.ressourcesStables.set(true);
    component.casierJudiciaireVierge.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.voieNaturalisation).toBe('DECRET');
    expect(req.request.body.dureeResidenceReguliereAnnees).toBe(5);
    expect(req.request.body.assimilationLangueB1).toBe(true);
    expect(req.request.body.ressourcesStables).toBe(true);
    // Champs MARIAGE ne doivent PAS être envoyés.
    expect(req.request.body.dureeMariageAnnees).toBeUndefined();
    expect(req.request.body.cohabitationContinue).toBeUndefined();
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Naturalisation analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() POST MARIAGE n\'envoie pas les champs DECRET résiduels', () => {
    component.voieNaturalisation.set('MARIAGE');
    component.dureeMariageAnnees.set(4);
    component.cohabitationContinue.set(true);
    component.assimilationLangueB1.set(true);
    // Résidu DECRET — doit être ignoré.
    component.ressourcesStables.set(true);
    component.etudesSuperieuresFrance.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.voieNaturalisation).toBe('MARIAGE');
    expect(req.request.body.dureeMariageAnnees).toBe(4);
    expect(req.request.body.cohabitationContinue).toBe(true);
    expect(req.request.body.assimilationLangueB1).toBe(true);
    expect(req.request.body.ressourcesStables).toBeUndefined();
    expect(req.request.body.etudesSuperieuresFrance).toBeUndefined();
    expect(req.request.body.dureeResidenceReguliereAnnees).toBeUndefined();
    req.flush(response({ voieNaturalisation: 'MARIAGE',
      voieRecommandee: 'Déclaration de nationalité par mariage (art. 21-2)' }));
  });

  it('calculate() OPPOSITION envoie uniquement la voie + transversaux (pas de champ spécifique)', () => {
    component.voieNaturalisation.set('OPPOSITION');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.voieNaturalisation).toBe('OPPOSITION');
    expect(req.request.body.casierJudiciaireVierge).toBe(true);
    expect(req.request.body.oppositionGouvernementaleActive).toBe(false);
    expect(req.request.body.dureeResidenceReguliereAnnees).toBeUndefined();
    expect(req.request.body.dureeMariageAnnees).toBeUndefined();
    req.flush(response({ voieNaturalisation: 'OPPOSITION',
      verdictRecevabilite: 'MOYENNE' }));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.voieNaturalisation.set('DECRET');
    component.dureeResidenceReguliereAnnees.set(5);
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
  // 5. F-IA-03 alertes de cohérence
  // ---------------------------------------------------------------------------

  it('coherenceAlerts vide quand pas de voie sélectionnée', () => {
    expect(component.coherenceAlerts().VOIE_NATURALISATION).toBeUndefined();
  });

  it('coherenceAlerts vide quand IA convergente avec saisie', () => {
    component.aiData = { typeProcedureDetectee: 'NATURALISATION_DECRET' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onVoieChange('DECRET');

    expect(component.coherenceAlerts().VOIE_NATURALISATION).toBeUndefined();
  });

  it('coherenceAlerts.VOIE_NATURALISATION présent si IA détecte une voie divergente', () => {
    component.aiData = { typeProcedureDetectee: 'NATURALISATION_MARIAGE' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onVoieChange('DECRET');

    const alert = component.coherenceAlerts().VOIE_NATURALISATION;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('VOIE_NATURALISATION');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toContain('mariage');
  });

  it('coherenceAlerts multi-sources F96 + IA convergents → MULTI', () => {
    component.aiData = { typeProcedureDetectee: 'NATURALISATION_MARIAGE' } as ImmigrationExtractedData;
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Voie de naturalisation',
        statut: 'NON_COMPLIANT',
        critereCode: 'IM13_VOIE_NATURALISATION',
        expectedValue: 'MARIAGE',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onVoieChange('DECRET');

    const alert = component.coherenceAlerts().VOIE_NATURALISATION;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
  });

  it('coherenceAlerts vidé après calcul (showForm=false)', () => {
    component.aiData = { typeProcedureDetectee: 'NATURALISATION_MARIAGE' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onVoieChange('DECRET');
    expect(component.coherenceAlerts().VOIE_NATURALISATION).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().VOIE_NATURALISATION).toBeUndefined();
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
    expect(component.bannerClass('ELEVEE')).toContain('nat-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('nat-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('nat-banner--critical');
    expect(component.bannerClass(null)).toContain('nat-banner--info');
  });

  it('bannerLabel retourne le verdict humain', () => {
    expect(component.bannerLabel('ELEVEE')).toContain('élevée');
    expect(component.bannerLabel('MOYENNE')).toContain('conditions limites');
    expect(component.bannerLabel('FAIBLE')).toContain('bloquant');
    expect(component.bannerLabel(null)).toBe('');
  });

  it('critereChipClass : "casier non vierge" → critical, "langue B1 non" → warning', () => {
    expect(component.critereChipClass('Casier judiciaire non vierge')).toContain('critical');
    expect(component.critereChipClass('Opposition gouvernementale active')).toContain('critical');
    expect(component.critereChipClass('Durée de résidence insuffisante')).toContain('critical');
    expect(component.critereChipClass('Assimilation linguistique B1 non établie')).toContain('warning');
  });

  it('voieLabel retourne le libellé humain ou le code en fallback', () => {
    expect(component.voieLabel('DECRET')).toContain('décret');
    expect(component.voieLabel('MARIAGE')).toContain('mariage');
    expect(component.voieLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.voieLabel(null)).toBe('');
  });
});
