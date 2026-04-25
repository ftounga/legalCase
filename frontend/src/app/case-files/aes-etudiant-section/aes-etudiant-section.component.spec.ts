import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { AesEtudiantSectionComponent } from './aes-etudiant-section.component';
import { AesEtudiantResponse } from '../../core/models/aes-etudiant.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('AesEtudiantSectionComponent (SF-IM-09-08)', () => {
  let component: AesEtudiantSectionComponent;
  let fixture: ComponentFixture<AesEtudiantSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/aes-etudiant';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function eleveeResponse(): AesEtudiantResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeFrance: '2022-01-15',
      dureePresenceMois: 50,
      anneesScolariteEnFranceConsecutives: 4,
      niveauEtudesActuel: 'BAC_PLUS_3_4',
      resultatsAcademiques: 'EXCELLENT',
      inscriptionEtablissementReconnu: true,
      moyensSubsistance: true,
      menaceOrdrePublic: false,
      parcoursCoherent: true,
      dateDepotDemande: '2026-04-01',
      country: 'FRANCE',
      presence3AnsOk: true,
      scolarite2AnsConsecutivesOk: true,
      resultatsAcceptables: true,
      inscriptionValide: true,
      moyensOk: true,
      pasMenace: true,
      parcoursCoherentOk: true,
      scoreGlobal: 100,
      verdictProbabiliteAcceptation: 'ELEVEE',
      criteresNonRemplis: [],
      dateExpirationInstructionSiDemande: '2026-10-01',
      formule: 'AES voie étudiante : tous critères remplis, score 100/100, probabilité ELEVEE.',
      baseJuridique: 'Circulaire Valls 28/11/2012 (actualisée Darmanin) — L.412-1 CESEDA',
      messages: ['Régularisation au titre de la circulaire Valls.'],
    };
  }

  function faibleResponse(): AesEtudiantResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeFrance: '2024-09-01',
      dureePresenceMois: 6,
      anneesScolariteEnFranceConsecutives: 0,
      niveauEtudesActuel: 'LYCEE',
      resultatsAcademiques: 'DIFFICULTES_REPETEES',
      inscriptionEtablissementReconnu: false,
      moyensSubsistance: false,
      menaceOrdrePublic: false,
      parcoursCoherent: false,
      dateDepotDemande: null,
      country: 'FRANCE',
      presence3AnsOk: false,
      scolarite2AnsConsecutivesOk: false,
      resultatsAcceptables: false,
      inscriptionValide: false,
      moyensOk: false,
      pasMenace: true,
      parcoursCoherentOk: false,
      scoreGlobal: 10,
      verdictProbabiliteAcceptation: 'FAIBLE',
      criteresNonRemplis: [
        'Moins de 3 ans de présence en France (seuil indicatif)',
        'Moins de 2 années consécutives de scolarité en France',
        'Résultats académiques insuffisants (difficultés répétées)',
        'Pas d\'inscription validée dans un établissement supérieur reconnu',
        'Pas de moyens de subsistance ou d\'hébergement stables',
        'Parcours scolaire incohérent (réorientations répétées)',
      ],
      dateExpirationInstructionSiDemande: null,
      formule: 'AES voie étudiante : score 10/100, probabilité FAIBLE.',
      baseJuridique: 'Circulaire Valls 28/11/2012 — L.412-1 CESEDA',
      messages: ['Risque rejet — conseiller réorientation.'],
    };
  }

  /** Absorbe la requête source-explanations émise par ngOnInit (fail-open). */
  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        AesEtudiantSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AesEtudiantSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // 1. Mount + lifecycle
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET émis ; 404 → mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('mount FRANCE + GET 200 → result rendu + champs persistés + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(eleveeResponse());
    expectSourceExplanationCall();
    expect(component.result()!.verdictProbabiliteAcceptation).toBe('ELEVEE');
    expect(component.dateEntreeFrance()).toBe('2022-01-15');
    expect(component.dureePresenceMois()).toBe(50);
    expect(component.anneesScolariteEnFranceConsecutives()).toBe(4);
    expect(component.niveauEtudesActuel()).toBe('BAC_PLUS_3_4');
    expect(component.resultatsAcademiques()).toBe('EXCELLENT');
    expect(component.inscriptionEtablissementReconnu()).toBe(true);
    expect(component.moyensSubsistance()).toBe(true);
    expect(component.parcoursCoherent()).toBe(true);
    expect(component.dateDepotDemande()).toBe('2026-04-01');
    expect(component.showForm()).toBe(false);
    // Pas de badge IA sur valeurs persistées.
    expect(component.provenanceDateEntreeFrance()).toBeNull();
    expect(component.provenanceDateDepotDemande()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 2. Form validators
  // ---------------------------------------------------------------------------

  it('formValid false si dateEntreeFrance manquante', () => {
    component.dateEntreeFrance.set(null);
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateEntreeFrance dans le futur', () => {
    component.dateEntreeFrance.set('2099-01-01');
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dureePresenceMois hors plage 0-600', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.dureePresenceMois.set(-1);
    expect(component.formValid()).toBe(false);
    component.dureePresenceMois.set(601);
    expect(component.formValid()).toBe(false);
    component.dureePresenceMois.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si anneesScolariteEnFrance hors plage 0-20', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.dureePresenceMois.set(50);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
    component.anneesScolariteEnFranceConsecutives.set(21);
    expect(component.formValid()).toBe(false);
    component.anneesScolariteEnFranceConsecutives.set(-1);
    expect(component.formValid()).toBe(false);
    component.anneesScolariteEnFranceConsecutives.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si niveauEtudesActuel manquant', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set(null);
    component.resultatsAcademiques.set('EXCELLENT');
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si resultatsAcademiques manquant', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateDepotDemande < dateEntreeFrance', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
    component.dateDepotDemande.set('2021-01-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid true sur saisie nominale', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
    component.dateDepotDemande.set('2026-04-01');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // 3. Calculate / submit
  // ---------------------------------------------------------------------------

  it('calculate() ignoré si form invalide (pas de POST)', () => {
    component.dateEntreeFrance.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() POST → verdict=ELEVEE → bannière succès + snackbar', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
    component.inscriptionEtablissementReconnu.set(true);
    component.moyensSubsistance.set(true);
    component.parcoursCoherent.set(true);
    component.dateDepotDemande.set('2026-04-01');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateEntreeFrance: '2022-01-15',
      dureePresenceMois: 50,
      anneesScolariteEnFranceConsecutives: 4,
      niveauEtudesActuel: 'BAC_PLUS_3_4',
      resultatsAcademiques: 'EXCELLENT',
      inscriptionEtablissementReconnu: true,
      moyensSubsistance: true,
      menaceOrdrePublic: false,
      parcoursCoherent: true,
      dateDepotDemande: '2026-04-01',
    });
    req.flush(eleveeResponse());

    expect(component.result()!.verdictProbabiliteAcceptation).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.bannerClass(component.result())).toContain('aes-banner--success');
  });

  it('calculate() POST → verdict=FAIBLE → bannière danger + chips criteresNonRemplis', () => {
    component.dateEntreeFrance.set('2024-09-01');
    component.dureePresenceMois.set(6);
    component.anneesScolariteEnFranceConsecutives.set(0);
    component.niveauEtudesActuel.set('LYCEE');
    component.resultatsAcademiques.set('DIFFICULTES_REPETEES');
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(faibleResponse());
    expect(component.result()!.verdictProbabiliteAcceptation).toBe('FAIBLE');
    expect(component.bannerClass(component.result())).toContain('aes-banner--danger');
    expect(component.result()!.criteresNonRemplis.length).toBeGreaterThan(0);
  });

  it('calculate() POST sans dateDepotDemande → champ omis du body', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
    component.inscriptionEtablissementReconnu.set(true);
    component.moyensSubsistance.set(true);
    component.parcoursCoherent.set(true);
    component.dateDepotDemande.set(null);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateDepotDemande).toBeUndefined();
    req.flush(eleveeResponse());
  });

  it('calculate() erreur backend → snackbar rouge + calculating=false', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.dureePresenceMois.set(50);
    component.anneesScolariteEnFranceConsecutives.set(4);
    component.niveauEtudesActuel.set('BAC_PLUS_3_4');
    component.resultatsAcademiques.set('EXCELLENT');
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
  // 4. Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA : dateDepotProcedure → dateDepotDemande + provenance="IA"', () => {
    component.aiData = {
      dateDepotProcedure: '2026-04-10',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateDepotDemande()).toBe('2026-04-10');
    expect(component.provenanceDateDepotDemande()).toBe('IA');
  });

  it('pré-fill IA : dateEntreeFrance via fallback gracieux → provenance="IA"', () => {
    component.aiData = {
      dateEntreeFrance: '2021-09-01',
    } as ImmigrationExtractedData & { dateEntreeFrance?: string };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateEntreeFrance()).toBe('2021-09-01');
    expect(component.provenanceDateEntreeFrance()).toBe('IA');
  });

  it('pré-fill IA : aucun aiData → champs vides', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateDepotDemande()).toBeNull();
    expect(component.provenanceDateDepotDemande()).toBeNull();
    expect(component.provenanceDateEntreeFrance()).toBeNull();
  });

  it('pré-fill IA : dateDepotProcedure dans le futur → ignorée', () => {
    component.aiData = {
      dateDepotProcedure: '2099-01-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateDepotDemande()).toBeNull();
    expect(component.provenanceDateDepotDemande()).toBeNull();
  });

  it('onDateDepotDemandeChange efface le badge IA', () => {
    component.aiData = {
      dateDepotProcedure: '2026-04-10',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.provenanceDateDepotDemande()).toBe('IA');
    component.onDateDepotDemandeChange('2026-05-01');
    expect(component.dateDepotDemande()).toBe('2026-05-01');
    expect(component.provenanceDateDepotDemande()).toBeNull();
  });

  it('onDateEntreeFranceChange efface le badge IA', () => {
    component.aiData = {
      dateEntreeFrance: '2021-09-01',
    } as ImmigrationExtractedData & { dateEntreeFrance?: string };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.provenanceDateEntreeFrance()).toBe('IA');
    component.onDateEntreeFranceChange('2022-01-15');
    expect(component.dateEntreeFrance()).toBe('2022-01-15');
    expect(component.provenanceDateEntreeFrance()).toBeNull();
  });

  it('ngOnChanges(aiData) tardif rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi: ImmigrationExtractedData = {
      dateDepotProcedure: '2026-04-15',
    };
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });
    expect(component.dateDepotDemande()).toBe('2026-04-15');
    expect(component.provenanceDateDepotDemande()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // 5. Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DATE_DEPOT_DEMANDE présent si divergence avec aiData', () => {
    component.aiData = {
      dateDepotProcedure: '2026-04-10',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // L'avocat saisit une date différente ⇒ alerte attendue.
    component.onDateDepotDemandeChange('2026-05-15');

    const alert = component.coherenceAlerts().DATE_DEPOT_DEMANDE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DATE_DEPOT_DEMANDE');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toBe('2026-04-10');
  });

  it('coherenceAlerts.DUREE_PRESENCE depuis F96 (NON_COMPLIANT)', () => {
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Durée de présence',
        statut: 'NON_COMPLIANT',
        critereCode: 'IM09_ETU_DUREE_PRESENCE',
        expectedValue: '48',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.dureePresenceMois.set(20);
    const alert = component.coherenceAlerts().DUREE_PRESENCE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
    expect(alert!.expectedDisplay).toBe('48 mois');
  });

  it('coherenceAlerts vides en mode résultat (showForm=false)', () => {
    component.aiData = {
      dateDepotProcedure: '2026-04-10',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateDepotDemandeChange('2026-05-15');
    expect(component.coherenceAlerts().DATE_DEPOT_DEMANDE).toBeDefined();
    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_DEPOT_DEMANDE).toBeUndefined();
  });

  it('alertBadgeLabel et alertTooltip retournent du texte non vide', () => {
    component.aiData = {
      dateDepotProcedure: '2026-04-10',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateDepotDemandeChange('2026-05-15');
    const alert = component.coherenceAlerts().DATE_DEPOT_DEMANDE!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence détectée');
    expect(component.alertTooltip(alert)).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // 6. Gate workspace
  // ---------------------------------------------------------------------------

  it('workspaceCountry=BELGIQUE → bannière info, pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('workspaceCountry=FRANCE → form visible, GET émis', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.isFrance()).toBe(true);
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // 7. Misc
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

  it('bannerLabel retourne le verdict humain', () => {
    expect(component.bannerLabel(null)).toBe('');
    expect(component.bannerLabel(eleveeResponse())).toContain('ÉLEVÉE');
    expect(component.bannerLabel(faibleResponse())).toContain('FAIBLE');
  });

  it('niveauLabel et resultatsLabel retournent un label humain', () => {
    expect(component.niveauLabel('BAC_PLUS_3_4')).toContain('Bac');
    expect(component.niveauLabel(null)).toBe('');
    expect(component.resultatsLabel('EXCELLENT')).toContain('Excellents');
    expect(component.resultatsLabel(null)).toBe('');
  });
});
