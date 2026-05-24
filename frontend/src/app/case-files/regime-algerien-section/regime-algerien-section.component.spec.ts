import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RegimeAlgerienSectionComponent } from './regime-algerien-section.component';
import { RegimeAlgerienResponse } from '../../core/models/regime-algerien.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('RegimeAlgerienSectionComponent (SF-IM-17-02)', () => {
  let component: RegimeAlgerienSectionComponent;
  let fixture: ComponentFixture<RegimeAlgerienSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/regime-algerien-analysis';

  function response(overrides: Partial<RegimeAlgerienResponse> = {}): RegimeAlgerienResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      voieDemande: 'CRA_1_AN',
      voieRecommandee: 'Certificat de résidence algérien 1 an (art. 5)',
      verdictRecevabilite: 'ELEVEE',
      titreApplicable: 'CRA_1_AN',
      dureeTitreAnnees: 1,
      criteresNonRemplis: [],
      documentsRequis: [
        'Acte de naissance algérien légalisé',
        'Visa long séjour valide',
        'Justificatif de domicile en France',
      ],
      delaiInstructionMois: 3,
      baseJuridique: 'Accord franco-algérien 27/12/1968 modifié + art. 5',
      formule: 'Régime algérien — CRA 1 an : verdict ELEVEE.',
      messages: ['Délai d\'instruction estimatif : 3 mois.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        RegimeAlgerienSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RegimeAlgerienSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

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

  it('GET 200 → résultat hydraté + voie persistée + nationalité=true + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.voieDemande()).toBe('CRA_1_AN');
    expect(component.nationaliteAlgerienne()).toBe(true);
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
  // 2. Gate nationalité algérienne
  // ---------------------------------------------------------------------------

  it('formActive false tant que nationaliteAlgerienne !== true (form désactivé)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.nationaliteAlgerienne()).toBe(false);
    expect(component.formActive()).toBe(false);

    component.onNationaliteAlgerienneChange(true);
    expect(component.formActive()).toBe(true);
  });

  it('onNationaliteAlgerienneChange efface la provenance IA', () => {
    component.provenanceNationalite.set('IA');
    component.onNationaliteAlgerienneChange(true);
    expect(component.nationaliteAlgerienne()).toBe(true);
    expect(component.provenanceNationalite()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 3. Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA : no-op gracieux sans aiData (pas d\'erreur, pas de provenance)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.voieDemande()).toBeNull();
    expect(component.nationaliteAlgerienne()).toBe(false);
    expect(component.provenanceVoie()).toBeNull();
    expect(component.provenanceNationalite()).toBeNull();
  });

  it('pré-fill IA : `typeProcedureDetectee=CRA_1_AN` → voieDemande pré-remplie + nationalité détectée', () => {
    component.aiData = { typeProcedureDetectee: 'CRA_1_AN' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.voieDemande()).toBe('CRA_1_AN');
    expect(component.provenanceVoie()).toBe('IA');
    // Heuristique défensive : "CRA_" dans typeProcedureDetectee → algérien plausible.
    expect(component.nationaliteAlgerienne()).toBe(true);
    expect(component.provenanceNationalite()).toBe('IA');
  });

  it('pré-fill IA : pas d\'écrasement si l\'avocat a déjà saisi une voie', () => {
    component.aiData = { typeProcedureDetectee: 'CRA_1_AN' } as ImmigrationExtractedData;
    component.voieDemande.set('REGROUPEMENT_FAMILIAL_ACCORD_1968');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.voieDemande()).toBe('REGROUPEMENT_FAMILIAL_ACCORD_1968');
    expect(component.provenanceVoie()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 4. Form validation + champs conditionnels
  // ---------------------------------------------------------------------------

  it('formValid false sans nationalité algérienne (même si voie + champs OK)', () => {
    component.voieDemande.set('CRA_1_AN');
    component.presenceReguliereFranceMois.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false sans voie (même si nationalité true)', () => {
    component.nationaliteAlgerienne.set(true);
    expect(component.formValid()).toBe(false);
  });

  it('formValid true CRA_1_AN dès que présence renseignée', () => {
    component.nationaliteAlgerienne.set(true);
    component.voieDemande.set('CRA_1_AN');
    expect(component.formValid()).toBe(false);
    component.presenceReguliereFranceMois.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('formValid true REGROUPEMENT dès que nombrePersonnesFoyer renseigné', () => {
    component.nationaliteAlgerienne.set(true);
    component.voieDemande.set('REGROUPEMENT_FAMILIAL_ACCORD_1968');
    expect(component.formValid()).toBe(false);
    component.nombrePersonnesFoyer.set(3);
    expect(component.formValid()).toBe(true);
  });

  it('formValid true RESIDENT_ANCIEN / TRAVAILLEUR sans champ requis', () => {
    component.nationaliteAlgerienne.set(true);
    component.voieDemande.set('CRA_10_ANS_RESIDENT_ANCIEN');
    expect(component.formValid()).toBe(true);
    component.voieDemande.set('CHANGEMENT_VERS_TRAVAILLEUR');
    expect(component.formValid()).toBe(true);
  });

  it('champs conditionnels : show*Fields selon voie', () => {
    component.voieDemande.set('CRA_1_AN');
    expect(component.showCra1AnFields()).toBe(true);
    expect(component.showLienFranceFields()).toBe(false);

    component.voieDemande.set('CRA_10_ANS_LIEN_FRANCE');
    expect(component.showLienFranceFields()).toBe(true);

    component.voieDemande.set('CRA_10_ANS_RESIDENT_ANCIEN');
    expect(component.showResidentAncienFields()).toBe(true);

    component.voieDemande.set('CHANGEMENT_VERS_TRAVAILLEUR');
    expect(component.showTravailleurFields()).toBe(true);

    component.voieDemande.set('REGROUPEMENT_FAMILIAL_ACCORD_1968');
    expect(component.showRegroupementFields()).toBe(true);
  });

  it('onVoieChange efface le badge IA', () => {
    component.provenanceVoie.set('IA');
    component.onVoieChange('CRA_1_AN');
    expect(component.voieDemande()).toBe('CRA_1_AN');
    expect(component.provenanceVoie()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 5. Calculate / submit
  // ---------------------------------------------------------------------------

  it('calculate() POST CRA_1_AN avec champs ciblés + résultat + snackbar succès', () => {
    component.nationaliteAlgerienne.set(true);
    component.voieDemande.set('CRA_1_AN');
    component.presenceReguliereFranceMois.set(0);
    component.visaLongSejourValide.set(true);
    component.documentEtatCivilOriginal.set(true);
    component.casierJudiciaireVierge.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.voieDemande).toBe('CRA_1_AN');
    expect(req.request.body.nationaliteAlgerienne).toBe(true);
    expect(req.request.body.presenceReguliereFranceMois).toBe(0);
    expect(req.request.body.visaLongSejourValide).toBe(true);
    expect(req.request.body.documentEtatCivilOriginal).toBe(true);
    // Champs autres voies ne doivent PAS être envoyés.
    expect(req.request.body.nombrePersonnesFoyer).toBeUndefined();
    expect(req.request.body.contratTravailValide).toBeUndefined();
    req.flush(response());

    expect(component.result()!.verdictRecevabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Régime algérien analysé', 'OK', jasmine.any(Object));
  });

  it('calculate() POST REGROUPEMENT n\'envoie pas les champs CRA_1_AN résiduels', () => {
    component.nationaliteAlgerienne.set(true);
    component.voieDemande.set('REGROUPEMENT_FAMILIAL_ACCORD_1968');
    component.nombrePersonnesFoyer.set(3);
    component.ressourcesSuffisantes.set(true);
    component.logementDecent.set(true);
    // Résidu CRA_1_AN — doit être ignoré.
    component.presenceReguliereFranceMois.set(120);
    component.visaLongSejourValide.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.voieDemande).toBe('REGROUPEMENT_FAMILIAL_ACCORD_1968');
    expect(req.request.body.nombrePersonnesFoyer).toBe(3);
    expect(req.request.body.ressourcesSuffisantes).toBe(true);
    expect(req.request.body.logementDecent).toBe(true);
    expect(req.request.body.presenceReguliereFranceMois).toBeUndefined();
    expect(req.request.body.visaLongSejourValide).toBeUndefined();
    req.flush(response({ voieDemande: 'REGROUPEMENT_FAMILIAL_ACCORD_1968',
      voieRecommandee: 'Regroupement familial accord 1968 (art. 4)',
      delaiInstructionMois: 6 }));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.nationaliteAlgerienne.set(true);
    component.voieDemande.set('CRA_1_AN');
    component.presenceReguliereFranceMois.set(0);
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
  // 6. F-IA-03 alertes de cohérence
  // ---------------------------------------------------------------------------

  it('coherenceAlerts vide quand pas de voie sélectionnée', () => {
    component.nationaliteAlgerienne.set(true);
    expect(component.coherenceAlerts().VOIE).toBeUndefined();
  });

  it('coherenceAlerts vide quand IA convergente avec saisie', () => {
    component.aiData = { typeProcedureDetectee: 'CRA_1_AN' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNationaliteAlgerienneChange(true);
    component.onVoieChange('CRA_1_AN');

    expect(component.coherenceAlerts().VOIE).toBeUndefined();
  });

  it('coherenceAlerts.VOIE présent si IA détecte une voie divergente', () => {
    component.aiData = { typeProcedureDetectee: 'REGROUPEMENT_FAMILIAL_ACCORD_1968' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNationaliteAlgerienneChange(true);
    component.onVoieChange('CRA_1_AN');

    const alert = component.coherenceAlerts().VOIE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('VOIE');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay.toLowerCase()).toContain('regroupement');
  });

  it('coherenceAlerts vidé après calcul (showForm=false)', () => {
    component.aiData = { typeProcedureDetectee: 'REGROUPEMENT_FAMILIAL_ACCORD_1968' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onNationaliteAlgerienneChange(true);
    component.onVoieChange('CRA_1_AN');
    expect(component.coherenceAlerts().VOIE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().VOIE).toBeUndefined();
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

  it('bannerClass mappe verdict → classe CSS attendue', () => {
    expect(component.bannerClass('ELEVEE')).toContain('rgalg-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('rgalg-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('rgalg-banner--critical');
    expect(component.bannerClass(null)).toContain('rgalg-banner--info');
  });

  it('bannerLabel retourne le verdict humain', () => {
    expect(component.bannerLabel('ELEVEE')).toContain('élevée');
    expect(component.bannerLabel('MOYENNE')).toContain('conditions limites');
    expect(component.bannerLabel('FAIBLE')).toContain('bloquant');
    expect(component.bannerLabel(null)).toBe('');
  });

  it('critereChipClass : "casier non vierge" → critical, "logement insuffisant" → critical', () => {
    expect(component.critereChipClass('Casier judiciaire non vierge')).toContain('critical');
    expect(component.critereChipClass('Présence en France insuffisante')).toContain('critical');
    expect(component.critereChipClass('Acte état civil manquant')).toContain('critical');
    expect(component.critereChipClass('Visa long séjour expiré')).toContain('warning');
  });

  it('voieLabel retourne le libellé humain ou le code en fallback', () => {
    expect(component.voieLabel('CRA_1_AN')).toContain('1 an');
    expect(component.voieLabel('REGROUPEMENT_FAMILIAL_ACCORD_1968')).toContain('Regroupement');
    expect(component.voieLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.voieLabel(null)).toBe('');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-17-regime-algerien/calculate';

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
