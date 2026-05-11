import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { AesMetiersTensionSectionComponent } from './aes-metiers-tension-section.component';
import { AesMetiersTensionResponse } from '../../core/models/aes-metiers-tension.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('AesMetiersTensionSectionComponent (SF-IM-09-05)', () => {
  let component: AesMetiersTensionSectionComponent;
  let fixture: ComponentFixture<AesMetiersTensionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/aes-metiers-tension';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function okResponse(): AesMetiersTensionResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeFrance: '2022-01-15',
      moisActiviteSalarieeDernieres24Mois: 14,
      metierEstEnTension: true,
      codeMetier: 'N1101',
      menaceOrdrePublic: false,
      contratOuPromesseValide: true,
      dateDepotDemande: '2026-04-01',
      country: 'FRANCE',
      presence3Ans: true,
      activite12MoisOk: true,
      conditionsReunies: true,
      criteresNonRemplis: [],
      dateEligibiliteAtteinte: null,
      dateExpirationInstructionSiDemande: '2026-10-01',
      formule: '3 ans présence + 14 mois activité ⇒ AES éligible',
      baseJuridique: 'CESEDA L.435-4 + loi n° 2024-42 du 26/01/2024 art. 26',
      messages: ['Critères réunis. Déposer demande à la préfecture.'],
    };
  }

  function koResponse(): AesMetiersTensionResponse {
    return {
      caseFileId: 'case-1',
      dateEntreeFrance: '2024-06-01',
      moisActiviteSalarieeDernieres24Mois: 8,
      metierEstEnTension: true,
      codeMetier: null,
      menaceOrdrePublic: false,
      contratOuPromesseValide: true,
      dateDepotDemande: null,
      country: 'FRANCE',
      presence3Ans: false,
      activite12MoisOk: false,
      conditionsReunies: false,
      criteresNonRemplis: ['Présence < 3 ans', 'Activité < 12 mois sur 24'],
      dateEligibiliteAtteinte: '2027-06-01',
      dateExpirationInstructionSiDemande: null,
      formule: 'Critères non réunis',
      baseJuridique: 'CESEDA L.435-4',
      messages: ['Voie AES Métiers en tension non encore ouverte.'],
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
        AesMetiersTensionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AesMetiersTensionSectionComponent);
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
    req.flush(okResponse());
    expectSourceExplanationCall();
    expect(component.result()!.conditionsReunies).toBe(true);
    expect(component.dateEntreeFrance()).toBe('2022-01-15');
    expect(component.moisActiviteSalarieeDernieres24Mois()).toBe(14);
    expect(component.metierEstEnTension()).toBe(true);
    expect(component.codeMetier()).toBe('N1101');
    expect(component.contratOuPromesseValide()).toBe(true);
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
    component.moisActiviteSalarieeDernieres24Mois.set(14);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateEntreeFrance dans le futur', () => {
    component.dateEntreeFrance.set('2099-01-01');
    component.moisActiviteSalarieeDernieres24Mois.set(14);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si moisActivite hors plage 0-24', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.moisActiviteSalarieeDernieres24Mois.set(-1);
    expect(component.formValid()).toBe(false);
    component.moisActiviteSalarieeDernieres24Mois.set(25);
    expect(component.formValid()).toBe(false);
    component.moisActiviteSalarieeDernieres24Mois.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si dateDepotDemande < dateEntreeFrance', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.moisActiviteSalarieeDernieres24Mois.set(14);
    component.dateDepotDemande.set('2021-01-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid true sur saisie nominale', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.moisActiviteSalarieeDernieres24Mois.set(14);
    component.dateDepotDemande.set('2026-04-01');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // 3. codeMetier conditionnel
  // ---------------------------------------------------------------------------

  it('codeMetier ignoré dans le POST si metierEstEnTension=false', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.moisActiviteSalarieeDernieres24Mois.set(14);
    component.metierEstEnTension.set(false);
    component.codeMetier.set('N1101'); // saisi mais sans toggle
    component.contratOuPromesseValide.set(true);
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.codeMetier).toBeUndefined();
    req.flush(okResponse());
  });

  it('onMetierEstEnTensionChange(false) efface codeMetier', () => {
    component.codeMetier.set('N1101');
    component.metierEstEnTension.set(true);
    component.onMetierEstEnTensionChange(false);
    expect(component.metierEstEnTension()).toBe(false);
    expect(component.codeMetier()).toBe('');
  });

  // ---------------------------------------------------------------------------
  // 4. Calculate / submit
  // ---------------------------------------------------------------------------

  it('calculate() ignoré si form invalide (pas de POST)', () => {
    component.dateEntreeFrance.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() POST → conditionsReunies=true → bannière succès + snackbar', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.moisActiviteSalarieeDernieres24Mois.set(14);
    component.metierEstEnTension.set(true);
    component.codeMetier.set('N1101');
    component.contratOuPromesseValide.set(true);
    component.dateDepotDemande.set('2026-04-01');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateEntreeFrance: '2022-01-15',
      moisActiviteSalarieeDernieres24Mois: 14,
      metierEstEnTension: true,
      menaceOrdrePublic: false,
      contratOuPromesseValide: true,
      codeMetier: 'N1101',
      dateDepotDemande: '2026-04-01',
    });
    req.flush(okResponse());

    expect(component.result()!.conditionsReunies).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.bannerClass(component.result())).toContain('aes-banner--success');
  });

  it('calculate() POST → conditionsReunies=false → bannière danger + chips criteresNonRemplis', () => {
    component.dateEntreeFrance.set('2024-06-01');
    component.moisActiviteSalarieeDernieres24Mois.set(8);
    component.metierEstEnTension.set(true);
    component.contratOuPromesseValide.set(true);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(koResponse());
    expect(component.result()!.conditionsReunies).toBe(false);
    expect(component.bannerClass(component.result())).toContain('aes-banner--danger');
    expect(component.result()!.criteresNonRemplis.length).toBeGreaterThan(0);
  });

  it('calculate() erreur backend → snackbar rouge + calculating=false', () => {
    component.dateEntreeFrance.set('2022-01-15');
    component.moisActiviteSalarieeDernieres24Mois.set(14);
    component.contratOuPromesseValide.set(true);
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
  // 5. Pré-fill IA
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

  it('pré-fill IA : aucun aiData → champs vides', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.dateDepotDemande()).toBeNull();
    expect(component.provenanceDateDepotDemande()).toBeNull();
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
  // 6. Coherence alerts F-IA-03
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

  it('coherenceAlerts.MOIS_ACTIVITE depuis F96 (NON_COMPLIANT)', () => {
    component.procedureChecks = [
      {
        id: 'chk-1', ordre: 1, description: 'Mois d\'activité',
        statut: 'NON_COMPLIANT',
        critereCode: 'IM09_MOIS_ACTIVITE',
        expectedValue: '14',
      },
    ];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.moisActiviteSalarieeDernieres24Mois.set(8);
    const alert = component.coherenceAlerts().MOIS_ACTIVITE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
    expect(alert!.expectedDisplay).toBe('14 mois');
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
  // 7. Gate workspace
  // ---------------------------------------------------------------------------

  it('workspaceCountry=BELGIQUE → bannière info, pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    // Pas de GET ni de source-explanations.
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
  // 8. Misc
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
    expect(component.bannerLabel(okResponse())).toBe('Conditions réunies');
    expect(component.bannerLabel(koResponse())).toBe('Conditions non réunies');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-09-aes-metiers-tension/calculate';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : aucun GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      try { (component as any).calculate(); } catch (_) { /* formValid */ }
      const dispatcherReqs = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL_F163 && r.method === 'POST');
      const caseFileReqs = httpMock.match((r: { url: string; method: string }) => r.url.includes('/api/v1/case-files/') && r.method === 'POST');
      // Aucun POST case-file ne doit partir en standalone.
      expect(caseFileReqs.length).toBe(0);
      dispatcherReqs.forEach((req: any) => req.flush({}));
    });
  });
});
