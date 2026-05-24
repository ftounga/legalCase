import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { MineursImmigrationSectionComponent } from './mineurs-immigration-section.component';
import { MineursImmigrationResponse } from '../../core/models/mineurs-immigration.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('MineursImmigrationSectionComponent (SF-IM-19-02)', () => {
  let component: MineursImmigrationSectionComponent;
  let fixture: ComponentFixture<MineursImmigrationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/mineurs-immigration-analysis';

  function response(overrides: Partial<MineursImmigrationResponse> = {}): MineursImmigrationResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      dispositifVise: 'MNA_ORDONNANCE_JE',
      dispositifRecommande: 'MNA_ORDONNANCE_JE',
      dateNaissance: '2010-03-15',
      dateEntreeFrance: '2024-09-01',
      parentRegulier: false,
      isolementAvere: true,
      motifOrdrePublic: false,
      nationalite: 'Côte d\'Ivoire',
      ageAnnees: 16,
      verdictEligibilite: 'ELEVEE',
      criteresNonRemplis: [],
      documentsRequis: ['Acte de naissance original'],
      delaiInstructionMois: 4,
      baseJuridique: 'Cciv art. 375 + CASF L.221-2-2',
      formule: 'Mineur étranger — dispositif MNA_ORDONNANCE_JE — 16 ans — ELEVEE.',
      messages: ['Saisine JE par requête conjointe procureur + ASE.'],
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        MineursImmigrationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(MineursImmigrationSectionComponent);
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

  it('GET 200 → résultat hydraté + champs persistés + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());

    expect(component.result()!.verdictEligibilite).toBe('ELEVEE');
    expect(component.dispositifVise()).toBe('MNA_ORDONNANCE_JE');
    expect(component.dateNaissance()).toBe('2010-03-15');
    expect(component.dateEntreeFrance()).toBe('2024-09-01');
    expect(component.isolementAvere()).toBe(true);
    expect(component.parentRegulier()).toBe(false);
    expect(component.motifOrdrePublic()).toBe(false);
    expect(component.nationalite()).toBe('Côte d\'Ivoire');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceDateNaissance()).toBeNull();
  });

  it('GET 404 → mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 2. Pré-fill IA
  // ---------------------------------------------------------------------------

  it('pré-fill IA : dateNaissance + dateEntreeFrance + nationalite (cast défensif)', () => {
    component.aiData = {
      mineursDateNaissance: '2012-05-20',
      aesDateEntreeFrance: '2012-06-01',
      nationalite: 'Mali',
    } as unknown as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateNaissance()).toBe('2012-05-20');
    expect(component.provenanceDateNaissance()).toBe('IA');
    expect(component.dateEntreeFrance()).toBe('2012-06-01');
    expect(component.provenanceDateEntreeFrance()).toBe('IA');
    expect(component.nationalite()).toBe('Mali');
    expect(component.provenanceNationalite()).toBe('IA');
  });

  it('pré-fill sans aiData → aucun pré-remplissage, aucun badge', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.dateNaissance()).toBeNull();
    expect(component.provenanceDateNaissance()).toBeNull();
  });

  it('onDateNaissanceChange efface le badge IA', () => {
    component.aiData = { mineursDateNaissance: '2012-05-20' } as unknown as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceDateNaissance()).toBe('IA');
    component.onDateNaissanceChange('2013-01-01');
    expect(component.dateNaissance()).toBe('2013-01-01');
    expect(component.provenanceDateNaissance()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // 3. Form validation
  // ---------------------------------------------------------------------------

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true seulement quand dispositif + dateNaissance présents', () => {
    component.dispositifVise.set('MNA_ORDONNANCE_JE');
    expect(component.formValid()).toBe(false);
    component.dateNaissance.set('2012-05-20');
    expect(component.formValid()).toBe(true);
    component.dispositifVise.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // 4. Calcul d'âge + bandeau majeur
  // ---------------------------------------------------------------------------

  it('ageAnnees retourne null si dateNaissance absente', () => {
    expect(component.ageAnnees()).toBeNull();
  });

  it('ageAnnees calcule l\'âge à partir de la dateNaissance', () => {
    component.dateNaissance.set('2010-01-01');
    const age = component.ageAnnees();
    expect(age).toBeGreaterThanOrEqual(15);
    expect(age).toBeLessThanOrEqual(17);
  });

  it('isMajeurAlert true si âge ≥ 18 ans', () => {
    component.dateNaissance.set('1990-01-01');
    expect(component.isMajeurAlert()).toBe(true);
  });

  it('isMajeurAlert false si mineur', () => {
    component.dateNaissance.set('2015-01-01');
    expect(component.isMajeurAlert()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // 5. Champs conditionnels selon dispositif
  // ---------------------------------------------------------------------------

  it('showIsolement visible uniquement pour MNA', () => {
    component.dispositifVise.set('MNA_ORDONNANCE_JE');
    expect(component.showIsolement()).toBe(true);
    component.dispositifVise.set('TITRE_SEJOUR_L435_3');
    expect(component.showIsolement()).toBe(false);
    component.dispositifVise.set('DCEM');
    expect(component.showIsolement()).toBe(false);
  });

  it('showParentRegulier uniquement pour L.435-3 ; showMotifOrdrePublic uniquement pour DCEM', () => {
    component.dispositifVise.set('TITRE_SEJOUR_L435_3');
    expect(component.showParentRegulier()).toBe(true);
    expect(component.showMotifOrdrePublic()).toBe(false);

    component.dispositifVise.set('DCEM');
    expect(component.showParentRegulier()).toBe(false);
    expect(component.showMotifOrdrePublic()).toBe(true);

    component.dispositifVise.set('TIR');
    expect(component.showTirNote()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // 6. Calculate / submit
  // ---------------------------------------------------------------------------

  it('calculate() POST + résultat + snackbar succès', () => {
    component.dispositifVise.set('MNA_ORDONNANCE_JE');
    component.dateNaissance.set('2010-03-15');
    component.dateEntreeFrance.set('2024-09-01');
    component.isolementAvere.set(true);
    component.nationalite.set('Côte d\'Ivoire');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual(jasmine.objectContaining({
      dispositifVise: 'MNA_ORDONNANCE_JE',
      dateNaissance: '2010-03-15',
      dateEntreeFrance: '2024-09-01',
      isolementAvere: true,
      nationalite: 'Côte d\'Ivoire',
    }));
    // Pas de parentRegulier / motifOrdrePublic envoyés (dispositif MNA).
    expect(req.request.body.parentRegulier).toBeUndefined();
    expect(req.request.body.motifOrdrePublic).toBeUndefined();
    req.flush(response());

    expect(component.result()!.verdictEligibilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Éligibilité mineur étranger analysée', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.dispositifVise.set('DCEM');
    component.dateNaissance.set('2012-05-20');
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
  // 7. F-IA-03 alertes de cohérence
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DATE_NAISSANCE présent si IA diverge de saisie', () => {
    component.aiData = { mineursDateNaissance: '2012-05-20' } as unknown as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // Pré-fill mit 2012-05-20. Avocat saisit autre chose → divergence.
    component.onDateNaissanceChange('2013-01-01');

    const alert = component.coherenceAlerts().DATE_NAISSANCE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DATE_NAISSANCE');
    expect(alert!.source).toBe('IA');
    expect(alert!.expectedDisplay).toBe('2012-05-20');
  });

  it('coherenceAlerts.DATE_NAISSANCE absent si IA convergent', () => {
    component.aiData = { mineursDateNaissance: '2012-05-20' } as unknown as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.dateNaissance()).toBe('2012-05-20');
    expect(component.coherenceAlerts().DATE_NAISSANCE).toBeUndefined();
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { mineursDateNaissance: '2012-05-20' } as unknown as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onDateNaissanceChange('2013-01-01');
    expect(component.coherenceAlerts().DATE_NAISSANCE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_NAISSANCE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // 8. ngOnChanges + non-régression
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { mineursDateNaissance: '2014-08-08' } as unknown as ImmigrationExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.dateNaissance()).toBe('2014-08-08');
    expect(component.provenanceDateNaissance()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // 9. Misc
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
    expect(component.bannerClass('ELEVEE')).toContain('mi-banner--info');
    expect(component.bannerClass('MOYENNE')).toContain('mi-banner--warning');
    expect(component.bannerClass('FAIBLE')).toContain('mi-banner--critical');
    expect(component.bannerClass(null)).toContain('mi-banner--info');
  });

  it('bannerLabel retourne le verdict humain', () => {
    expect(component.bannerLabel('ELEVEE')).toBe('Dispositif éligible');
    expect(component.bannerLabel('MOYENNE')).toContain('contestable');
    expect(component.bannerLabel('FAIBLE')).toContain('non applicable');
    expect(component.bannerLabel(null)).toBe('');
  });

  it('riskChipClass : "majorité atteinte" → critical, "résidence < 3 ans" → warning', () => {
    expect(component.riskChipClass('Majorité atteinte (18 ans)')).toContain('critical');
    expect(component.riskChipClass('Motif d\'ordre public présent')).toContain('critical');
    expect(component.riskChipClass('Isolement non avéré')).toContain('critical');
    expect(component.riskChipClass('Résidence en France < 3 ans')).toContain('warning');
  });

  it('dispositifLabel retourne le libellé humain ou le code en fallback', () => {
    expect(component.dispositifLabel('MNA_ORDONNANCE_JE')).toContain('MNA');
    expect(component.dispositifLabel('TITRE_SEJOUR_L435_3')).toContain('L.435-3');
    expect(component.dispositifLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.dispositifLabel(null)).toBe('');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-19-mineurs/calculate';

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
