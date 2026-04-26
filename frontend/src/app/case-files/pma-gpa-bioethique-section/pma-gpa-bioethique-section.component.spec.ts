import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { PmaGpaBioethiqueSectionComponent } from './pma-gpa-bioethique-section.component';
import { PmaGpaBioethiqueResponse } from '../../core/models/pma-gpa-bioethique.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('PmaGpaBioethiqueSectionComponent', () => {
  let component: PmaGpaBioethiqueSectionComponent;
  let fixture: ComponentFixture<PmaGpaBioethiqueSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/pma-gpa-bioethique';

  function response(overrides: Partial<PmaGpaBioethiqueResponse> = {}): PmaGpaBioethiqueResponse {
    return {
      caseFileId: 'case-1',
      dispositif: 'PMA_RECONNAISSANCE_ANTICIPEE',
      verdictRecevabilite: 'ELEVEE',
      proceduresAFollow: ['Établir l\'acte notarié'],
      risqueRefus: 'FAIBLE',
      documentsRequis: ['Acte de consentement notarié'],
      delaiInstructionMois: 1,
      baseJuridique: 'Art. 342-9 et s. Cciv',
      formule: 'PMA reconnaissance anticipée — verdict ELEVEE.',
      messages: ['Reconnaissance notariée antérieure à la PMA'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        PmaGpaBioethiqueSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(PmaGpaBioethiqueSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

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

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Switch dispositif
  // ============================================================

  it('switch dispositif PMA → GPA réinitialise les champs PMA', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.dispositif.set('PMA_RECONNAISSANCE_ANTICIPEE');
    component.consentementsConjointsNotaire.set(true);
    component.dateReconnaissanceAnterieurePMA.set('2024-01-15');
    component.datePMA.set('2024-04-01');
    component.conditionsAccesPMA.set(true);

    component.onDispositifChange('GPA_TRANSCRIPTION_ETAT_CIVIL');

    expect(component.dispositif()).toBe('GPA_TRANSCRIPTION_ETAT_CIVIL');
    expect(component.consentementsConjointsNotaire()).toBeNull();
    expect(component.dateReconnaissanceAnterieurePMA()).toBeNull();
    expect(component.datePMA()).toBeNull();
    expect(component.conditionsAccesPMA()).toBeNull();
  });

  it('switch dispositif GPA → DON_GAMETES réinitialise les champs GPA', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDispositifChange('GPA_TRANSCRIPTION_ETAT_CIVIL');
    component.paysGPACode.set('USA');
    component.parentBiologiqueAvere.set(true);
    component.decisionEtrangereProduite.set(true);
    component.adoptionDemande.set(true);

    component.onDispositifChange('DON_GAMETES_ACCES_ORIGINES');

    expect(component.dispositif()).toBe('DON_GAMETES_ACCES_ORIGINES');
    expect(component.paysGPACode()).toBeNull();
    expect(component.parentBiologiqueAvere()).toBeNull();
    expect(component.decisionEtrangereProduite()).toBeNull();
    expect(component.adoptionDemande()).toBeNull();
  });

  it('switch dispositif DON → PMA réinitialise les champs DON', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDispositifChange('DON_GAMETES_ACCES_ORIGINES');
    component.dateDon.set('2023-06-01');
    component.demandeAcces.set(true);
    component.ageDemandeur.set(22);

    component.onDispositifChange('PMA_RECONNAISSANCE_ANTICIPEE');

    expect(component.dispositif()).toBe('PMA_RECONNAISSANCE_ANTICIPEE');
    expect(component.dateDon()).toBeNull();
    expect(component.demandeAcces()).toBeNull();
    expect(component.ageDemandeur()).toBeNull();
  });

  // ============================================================
  // formValid
  // ============================================================

  it('formValid PMA exige consentementsConjointsNotaire + conditionsAccesPMA', () => {
    component.dispositif.set('PMA_RECONNAISSANCE_ANTICIPEE');
    component.consentementsConjointsNotaire.set(true);
    component.conditionsAccesPMA.set(true);
    expect(component.formValid()).toBe(true);

    component.consentementsConjointsNotaire.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid GPA exige les 4 champs GPA', () => {
    component.dispositif.set('GPA_TRANSCRIPTION_ETAT_CIVIL');
    component.paysGPACode.set('USA');
    component.parentBiologiqueAvere.set(true);
    component.decisionEtrangereProduite.set(true);
    component.adoptionDemande.set(true);
    expect(component.formValid()).toBe(true);

    component.adoptionDemande.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid DON_GAMETES exige demandeAcces et ageDemandeur ≥ 0', () => {
    component.dispositif.set('DON_GAMETES_ACCES_ORIGINES');
    component.demandeAcces.set(true);
    component.ageDemandeur.set(20);
    expect(component.formValid()).toBe(true);

    component.ageDemandeur.set(-1);
    expect(component.formValid()).toBe(false);

    component.ageDemandeur.set(0);
    expect(component.formValid()).toBe(true);

    component.ageDemandeur.set(20);
    component.demandeAcces.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur + isolation par dispositif
  // ============================================================

  it('calculate() PMA POST + résultat + snackbar succès + champs GPA/DON null', () => {
    component.dispositif.set('PMA_RECONNAISSANCE_ANTICIPEE');
    component.consentementsConjointsNotaire.set(true);
    component.dateReconnaissanceAnterieurePMA.set('2024-01-15');
    component.datePMA.set('2024-04-01');
    component.conditionsAccesPMA.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositif).toBe('PMA_RECONNAISSANCE_ANTICIPEE');
    expect(req.request.body.consentementsConjointsNotaire).toBe(true);
    expect(req.request.body.dateReconnaissanceAnterieurePMA).toBe('2024-01-15');
    expect(req.request.body.datePMA).toBe('2024-04-01');
    expect(req.request.body.conditionsAccesPMA).toBe(true);
    expect(req.request.body.paysGPALegal).toBeNull();
    expect(req.request.body.parentBiologiqueAvere).toBeNull();
    expect(req.request.body.dateDon).toBeNull();
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('PMA / GPA / bioéthique analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() GPA traduit le code pays en paysGPALegal boolean', () => {
    component.dispositif.set('GPA_TRANSCRIPTION_ETAT_CIVIL');
    component.paysGPACode.set('USA');
    component.parentBiologiqueAvere.set(true);
    component.decisionEtrangereProduite.set(true);
    component.adoptionDemande.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositif).toBe('GPA_TRANSCRIPTION_ETAT_CIVIL');
    expect(req.request.body.paysGPALegal).toBe(true);
    expect(req.request.body.parentBiologiqueAvere).toBe(true);
    expect(req.request.body.consentementsConjointsNotaire).toBeNull();
    req.flush(response({
      dispositif: 'GPA_TRANSCRIPTION_ETAT_CIVIL',
      verdictRecevabilite: 'ELEVEE',
      delaiInstructionMois: 9,
    }));

    expect(component.result()!.delaiInstructionMois).toBe(9);
  });

  it('calculate() GPA pays non légal → paysGPALegal false', () => {
    component.dispositif.set('GPA_TRANSCRIPTION_ETAT_CIVIL');
    component.paysGPACode.set('AUTRE_NON_LEGAL');
    component.parentBiologiqueAvere.set(false);
    component.decisionEtrangereProduite.set(false);
    component.adoptionDemande.set(false);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.paysGPALegal).toBe(false);
    req.flush(response({
      dispositif: 'GPA_TRANSCRIPTION_ETAT_CIVIL',
      verdictRecevabilite: 'FAIBLE',
      risqueRefus: 'ELEVE',
    }));

    expect(component.result()!.verdictRecevabilite).toBe('FAIBLE');
  });

  it('calculate() DON_GAMETES POST envoie dateDon + demandeAcces + ageDemandeur', () => {
    component.dispositif.set('DON_GAMETES_ACCES_ORIGINES');
    component.dateDon.set('2023-06-01');
    component.demandeAcces.set(true);
    component.ageDemandeur.set(22);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dispositif).toBe('DON_GAMETES_ACCES_ORIGINES');
    expect(req.request.body.dateDon).toBe('2023-06-01');
    expect(req.request.body.demandeAcces).toBe(true);
    expect(req.request.body.ageDemandeur).toBe(22);
    expect(req.request.body.consentementsConjointsNotaire).toBeNull();
    expect(req.request.body.paysGPALegal).toBeNull();
    req.flush(response({
      dispositif: 'DON_GAMETES_ACCES_ORIGINES',
      verdictRecevabilite: 'ELEVEE',
      delaiInstructionMois: 6,
    }));

    expect(component.result()!.dispositif).toBe('DON_GAMETES_ACCES_ORIGINES');
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    // dispositif PMA par défaut, mais champs requis non remplis
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge + calculating false', () => {
    component.dispositif.set('PMA_RECONNAISSANCE_ANTICIPEE');
    component.consentementsConjointsNotaire.set(false);
    component.conditionsAccesPMA.set(false);
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
  // Pré-fill IA
  // ============================================================

  it('pré-fill IA : dispositif ← aiData.dispositifBioethiqueDetecte', () => {
    component.aiData = {
      dispositifBioethiqueDetecte: 'GPA_TRANSCRIPTION_ETAT_CIVIL',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dispositif()).toBe('GPA_TRANSCRIPTION_ETAT_CIVIL');
    expect(component.provenanceDispositif()).toBe('IA');
  });

  it('pré-fill IA : valeur invalide ignorée (no-op gracieux)', () => {
    component.aiData = {
      dispositifBioethiqueDetecte: 'INCONNU',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Reste sur le défaut PMA, pas de provenance.
    expect(component.dispositif()).toBe('PMA_RECONNAISSANCE_ANTICIPEE');
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceDispositif()).toBeNull();
  });

  it('onDispositifChange manuel efface la provenance IA', () => {
    component.aiData = {
      dispositifBioethiqueDetecte: 'DON_GAMETES_ACCES_ORIGINES',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceDispositif()).toBe('IA');
    component.onDispositifChange('PMA_RECONNAISSANCE_ANTICIPEE');
    expect(component.dispositif()).toBe('PMA_RECONNAISSANCE_ANTICIPEE');
    expect(component.provenanceDispositif()).toBeNull();
  });

  // ============================================================
  // F-IA-03 alertes de cohérence
  // ============================================================

  it('coherenceAlerts.DISPOSITIF présent si IA diverge de saisie', () => {
    component.aiData = {
      dispositifBioethiqueDetecte: 'GPA_TRANSCRIPTION_ETAT_CIVIL',
    } as FamilleExtractedData;
    // Sans GET (BELGIQUE = pas de GET) — mais on veut FRANCE pour activer alerts
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit GPA. Avocat saisit PMA → divergence IA.
    component.onDispositifChange('PMA_RECONNAISSANCE_ANTICIPEE');

    const alerts = component.coherenceAlerts();
    expect(alerts.DISPOSITIF).toBeDefined();
    expect(alerts.DISPOSITIF!.field).toBe('DISPOSITIF');
    expect(alerts.DISPOSITIF!.source).toBe('IA');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = {
      dispositifBioethiqueDetecte: 'GPA_TRANSCRIPTION_ETAT_CIVIL',
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDispositifChange('PMA_RECONNAISSANCE_ANTICIPEE');
    expect(component.coherenceAlerts().DISPOSITIF).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DISPOSITIF).toBeUndefined();
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = {
      dispositifBioethiqueDetecte: 'DON_GAMETES_ACCES_ORIGINES',
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dispositif()).toBe('DON_GAMETES_ACCES_ORIGINES');
    expect(component.provenanceDispositif()).toBe('IA');
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

  it('bannerClass mappe verdict → classe CSS attendue', () => {
    expect(component.bannerClass('ELEVEE')).toContain('pma-gpa-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('pma-gpa-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('pma-gpa-banner--critical');
    expect(component.bannerClass(null)).toContain('pma-gpa-banner--info');
  });

  it('dispositifLabel renvoie le libellé humain ou le code en fallback', () => {
    expect(component.dispositifLabel('PMA_RECONNAISSANCE_ANTICIPEE')).toContain('PMA');
    expect(component.dispositifLabel('GPA_TRANSCRIPTION_ETAT_CIVIL')).toContain('GPA');
    expect(component.dispositifLabel('DON_GAMETES_ACCES_ORIGINES')).toContain('Don');
    expect(component.dispositifLabel(null)).toBe('');
  });

  it('isDonPosterieurReforme : 2023-06-01 ≥ 2022-09-01 → true', () => {
    component.dateDon.set('2023-06-01');
    expect(component.isDonPosterieurReforme()).toBe(true);
  });

  it('isDonPosterieurReforme : 2020-01-01 < 2022-09-01 → false', () => {
    component.dateDon.set('2020-01-01');
    expect(component.isDonPosterieurReforme()).toBe(false);
  });

  it('paysLabel renvoie le libellé pays ou code en fallback', () => {
    expect(component.paysLabel('USA')).toContain('États-Unis');
    expect(component.paysLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.paysLabel(null)).toBe('');
  });
});
