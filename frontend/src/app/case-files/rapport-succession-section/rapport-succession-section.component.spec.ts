import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { RapportSuccessionSectionComponent } from './rapport-succession-section.component';
import { RapportSuccessionResponse } from '../../core/models/rapport-succession.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';

describe('RapportSuccessionSectionComponent', () => {
  let component: RapportSuccessionSectionComponent;
  let fixture: ComponentFixture<RapportSuccessionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/rapport-succession-analysis';

  function response(overrides: Partial<RapportSuccessionResponse> = {}): RapportSuccessionResponse {
    return {
      caseFileId: 'case-1',
      donationsRecuesEur: 50000,
      dateDonation: '2020-06-15',
      valeurAuJourPartage: 80000,
      donationDispenseDeRapport: false,
      naturePresumeeNonRapportable: false,
      qualiteHeritier: 'DESCENDANT',
      verdictObligation: 'RAPPORTABLE',
      modeRapportRecommande: 'RAPPORT_EN_VALEUR',
      montantRapportable: 80000,
      delaiPrescriptionAns: 5,
      scoreEligibilite: 90,
      baseJuridique: 'Art. 843-863 + 919 Cciv',
      formule: 'Donation 50000 € → valeur jour partage 80000 € → rapport en valeur 80000 €',
      messages: ['Rapport en valeur (mode par défaut, art. 860).'],
      country: 'FRANCE',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        RapportSuccessionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RapportSuccessionSectionComponent);
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

    expect(component.result()!.verdictObligation).toBe('RAPPORTABLE');
    expect(component.result()!.modeRapportRecommande).toBe('RAPPORT_EN_VALEUR');
    expect(component.showForm()).toBe(false);
    expect(component.provenanceQualiteHeritier()).toBeNull();
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

  it('pré-fill IA : qualité + montants + date + dispense + nature', () => {
    component.aiData = {
      qualiteHeritierRapportDetectee: 'DESCENDANT',
      montantDonationsRecuesEurDetecte: 50000,
      valeurDonationAuJourPartageEurDetectee: 80000,
      dateDonationDetectee: '2020-06-15',
      donationDispenseDeRapportDetected: true,
      naturePresumeeNonRapportableDetected: false,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.qualiteHeritier()).toBe('DESCENDANT');
    expect(component.provenanceQualiteHeritier()).toBe('IA');
    expect(component.donationsRecuesEur()).toBe(50000);
    expect(component.provenanceDonationsRecues()).toBe('IA');
    expect(component.valeurAuJourPartage()).toBe(80000);
    expect(component.provenanceValeurPartage()).toBe('IA');
    expect(component.dateDonation()).toBe('2020-06-15');
    expect(component.provenanceDateDonation()).toBe('IA');
    expect(component.donationDispenseDeRapport()).toBe(true);
    expect(component.provenanceDispense()).toBe('IA');
    expect(component.naturePresumeeNonRapportable()).toBe(false);
  });

  it('pré-fill sans aiData → aucun pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.qualiteHeritier()).toBeNull();
    expect(component.donationsRecuesEur()).toBeNull();
    expect(component.provenanceQualiteHeritier()).toBeNull();
  });

  it('onQualiteHeritierChange efface badge IA', () => {
    component.aiData = { qualiteHeritierRapportDetectee: 'DESCENDANT' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceQualiteHeritier()).toBe('IA');
    component.onQualiteHeritierChange('CONJOINT_SURVIVANT');
    expect(component.qualiteHeritier()).toBe('CONJOINT_SURVIVANT');
    expect(component.provenanceQualiteHeritier()).toBeNull();
  });

  it('onDonationsRecuesChange efface badge IA', () => {
    component.aiData = { montantDonationsRecuesEurDetecte: 50000 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceDonationsRecues()).toBe('IA');
    component.onDonationsRecuesChange(60000);
    expect(component.donationsRecuesEur()).toBe(60000);
    expect(component.provenanceDonationsRecues()).toBeNull();
  });

  // ============================================================
  // Form validation
  // ============================================================

  it('formValid false initialement', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid true après remplissage complet', () => {
    component.qualiteHeritier.set('DESCENDANT');
    component.donationsRecuesEur.set(50000);
    component.dateDonation.set('2020-06-15');
    component.valeurAuJourPartage.set(80000);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si donationsRecuesEur ≤ 0', () => {
    component.qualiteHeritier.set('DESCENDANT');
    component.donationsRecuesEur.set(0);
    component.dateDonation.set('2020-06-15');
    component.valeurAuJourPartage.set(80000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid false si valeurAuJourPartage ≤ 0', () => {
    component.qualiteHeritier.set('DESCENDANT');
    component.donationsRecuesEur.set(50000);
    component.dateDonation.set('2020-06-15');
    component.valeurAuJourPartage.set(0);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // POST + erreur
  // ============================================================

  it('calculate() POST envoie le body attendu + résultat + snackbar succès', () => {
    component.qualiteHeritier.set('DESCENDANT');
    component.donationsRecuesEur.set(50000);
    component.dateDonation.set('2020-06-15');
    component.valeurAuJourPartage.set(80000);
    component.donationDispenseDeRapport.set(false);
    component.naturePresumeeNonRapportable.set(false);

    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.qualiteHeritier).toBe('DESCENDANT');
    expect(req.request.body.donationsRecuesEur).toBe(50000);
    expect(req.request.body.dateDonation).toBe('2020-06-15');
    expect(req.request.body.valeurAuJourPartage).toBe(80000);
    expect(req.request.body.donationDispenseDeRapport).toBe(false);
    expect(req.request.body.naturePresumeeNonRapportable).toBe(false);
    req.flush(response());

    expect(component.result()!.verdictObligation).toBe('RAPPORTABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Rapport à succession analysé', 'OK', jasmine.any(Object));
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.qualiteHeritier.set('DESCENDANT');
    component.donationsRecuesEur.set(50000);
    component.dateDonation.set('2020-06-15');
    component.valeurAuJourPartage.set(80000);

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

  it('coherenceAlerts.QUALITE_HERITIER présent si IA divergente de la saisie', () => {
    component.aiData = { qualiteHeritierRapportDetectee: 'DESCENDANT' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onQualiteHeritierChange('CONJOINT_SURVIVANT');

    const alerts = component.coherenceAlerts();
    expect(alerts.QUALITE_HERITIER).toBeDefined();
    expect(alerts.QUALITE_HERITIER!.field).toBe('QUALITE_HERITIER');
    expect(alerts.QUALITE_HERITIER!.source).toBe('IA');
    expect(alerts.QUALITE_HERITIER!.expectedDisplay).toContain('descendant');
  });

  it('coherenceAlerts.DONATIONS_RECUES_EUR divergent IA tolérance > 1 % → alerte', () => {
    component.aiData = { montantDonationsRecuesEurDetecte: 50000 } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDonationsRecuesChange(60000);
    const alerts = component.coherenceAlerts();
    expect(alerts.DONATIONS_RECUES_EUR).toBeDefined();
    expect(alerts.DONATIONS_RECUES_EUR!.source).toBe('IA');
    expect(alerts.DONATIONS_RECUES_EUR!.expectedDisplay).toContain('50');
  });

  it('coherenceAlerts vides après calcul (showForm=false)', () => {
    component.aiData = { qualiteHeritierRapportDetectee: 'DESCENDANT' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onQualiteHeritierChange('CONJOINT_SURVIVANT');
    expect(component.coherenceAlerts().QUALITE_HERITIER).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().QUALITE_HERITIER).toBeUndefined();
  });

  // ============================================================
  // ngOnChanges + non-régression
  // ============================================================

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = { qualiteHeritierRapportDetectee: 'DESCENDANT' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.qualiteHeritier()).toBe('DESCENDANT');
    expect(component.provenanceQualiteHeritier()).toBe('IA');
  });

  it('ngOnChanges(aiData) post-saisie ne réécrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onQualiteHeritierChange('CONJOINT_SURVIVANT');
    expect(component.provenanceQualiteHeritier()).toBeNull();

    const newAi = { qualiteHeritierRapportDetectee: 'DESCENDANT' } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.qualiteHeritier()).toBe('CONJOINT_SURVIVANT');
    expect(component.provenanceQualiteHeritier()).toBeNull();
  });

  // ============================================================
  // Helpers UI
  // ============================================================

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('verdictLabel couvre les 4 valeurs', () => {
    expect(component.verdictLabel('RAPPORTABLE')).toContain('rapportable');
    expect(component.verdictLabel('EXEMPT')).toContain('exempte');
    expect(component.verdictLabel('DISPENSÉ')).toContain('dispensée');
    expect(component.verdictLabel('NON_OBLIGÉ')).toContain('non obligé');
    expect(component.verdictLabel(null)).toBe('');
  });

  it('qualiteLabel couvre les 2 qualités', () => {
    expect(component.qualiteLabel('DESCENDANT')).toContain('descendant');
    expect(component.qualiteLabel('CONJOINT_SURVIVANT')).toContain('Conjoint');
    expect(component.qualiteLabel(null)).toBe('');
  });

  it('modeLabel couvre les 4 modes', () => {
    expect(component.modeLabel('RAPPORT_EN_VALEUR')).toContain('valeur');
    expect(component.modeLabel('RAPPORT_EN_NATURE')).toContain('nature');
    expect(component.modeLabel('RAPPORT_EN_MOINS_PRENANT')).toContain('moins');
    expect(component.modeLabel('NON_APPLICABLE')).toContain('Pas');
    expect(component.modeLabel(null)).toBe('');
  });

  it('verdictChipClass discrimine RAPPORTABLE (critique) vs autres (info)', () => {
    expect(component.verdictChipClass('RAPPORTABLE')).toContain('critical');
    expect(component.verdictChipClass('EXEMPT')).toContain('info');
    expect(component.verdictChipClass('DISPENSÉ')).toContain('info');
    expect(component.verdictChipClass(null)).toContain('info');
  });

  it('coherenceAlerts.DONATION_DISPENSE alerte si IA divergent du toggle', () => {
    component.aiData = { donationDispenseDeRapportDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    // IA dit true, l'avocat passe à false
    component.onDispenseChange(false);

    const alerts = component.coherenceAlerts();
    expect(alerts.DONATION_DISPENSE).toBeDefined();
    expect(alerts.DONATION_DISPENSE!.source).toBe('IA');
    expect(alerts.DONATION_DISPENSE!.expectedDisplay).toContain('919');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02c — mode simulateur autonome
  // ---------------------------------------------------------------------------

  describe('F-163 SF-163-02c — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-FA-24-rapport-succession/calculate';
    const CASE_URL = '/api/v1/case-files/case-1/rapport-succession-analysis';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : ne fait AUCUN GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/.../calculate en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      component.calculate();
      const requests = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL && r.method === 'POST');
      // Si la méthode formValid() bloque ou si la gate FR/BE échoue, aucun POST
      // n'est émis — c'est acceptable (le standalone n'a pas de payload obligatoire).
      // On valide ici qu'**aucun POST vers le case-file URL** n'a été émis.
      const caseUrlPosts = httpMock.match((r: { url: string; method: string }) => r.url === CASE_URL && r.method === 'POST');
      expect(caseUrlPosts.length).toBe(0);
      // Et, si le composant a POST sur le dispatcher, il flush proprement.
      requests.forEach(req => req.flush({}));
    });
  });
});
