import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LicenciementNulDetectionService } from '../../core/services/licenciement-nul-detection.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { LicenciementNulDetectionSectionComponent } from './licenciement-nul-detection-section.component';
import {
  LicenciementNulDetectionResponse,
} from '../../core/models/licenciement-nul-detection.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('LicenciementNulDetectionSectionComponent', () => {
  let component: LicenciementNulDetectionSectionComponent;
  let fixture: ComponentFixture<LicenciementNulDetectionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/licenciement-nul-detection';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function elevatedResponse(): LicenciementNulDetectionResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationLicenciement: '2026-04-15',
      salarieEnceinte: false,
      dateAccouchement: null,
      salarieAccidentTravail: true,
      dateConsolidationAT: '2024-01-15',
      salarieHarceleAvere: false,
      salarieDiscriminationAlleguee: true,
      salarieMotifLanceurAlerte: false,
      salarieMandatRepresentant: false,
      salarieActionJustice: false,
      salaireMensuelBrutEur: 2500,
      ancienneteAnnees: 8,
      protectionsDetectees: ['ACCIDENT_TRAVAIL', 'DISCRIMINATION'],
      nombreProtectionsActives: 2,
      nulliteProbable: true,
      scoreNullite: 70,
      verdictProbabiliteNullite: 'ELEVEE',
      indemniteMinimumNuliteEur: 15000,
      indemniteMinimumMois: 6,
      reintegrationOuverte: true,
      baseJuridique: 'Art. L.1235-3-1 al. 2 + L.1226-9 (AT) + L.1132-4 (discrimination)',
      formule: 'Plancher 6 mois × 2500,00 € = 15 000,00 € | 2 protections',
      messages: ['Plancher 6 mois (art. L.1235-3-1 al. 2)'],
      country: 'FRANCE',
    };
  }

  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        LicenciementNulDetectionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementNulDetectionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + GET initial
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.workspaceCountry).toBe('FRANCE');
    expect(component.showForm()).toBe(true);
  });

  it('mount BELGIQUE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('GET 200 → restore 7 booléens + dates + salaire + result + showForm=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(elevatedResponse());
    expectSourceExplanationCall();

    expect(component.result()!.verdictProbabiliteNullite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(component.salarieAccidentTravail()).toBe(true);
    expect(component.salarieDiscriminationAlleguee()).toBe(true);
    expect(component.salaireMensuelBrutEur()).toBe(2500);
    expect(component.dateConsolidationAT()).toBe('2024-01-15');
    expect(component.dateNotificationLicenciement()).toBe('2026-04-15');
    expect(component.ancienneteAnnees()).toBe(8);
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid()
  // ---------------------------------------------------------------------------

  it('formValid : exige salaire > 0 + dateNotificationLicenciement non null', () => {
    expect(component.formValid()).toBe(false);

    component.salaireMensuelBrutEur.set(2500);
    expect(component.formValid()).toBe(false);

    component.dateNotificationLicenciement.set('2026-04-15');
    expect(component.formValid()).toBe(true);

    component.salaireMensuelBrutEur.set(0);
    expect(component.formValid()).toBe(false);

    component.salaireMensuelBrutEur.set(2500);
    component.dateNotificationLicenciement.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // POST + snackbar + refresh
  // ---------------------------------------------------------------------------

  it('calculate() POST → result + snackbar succès + triggerRefresh', () => {
    component.dateNotificationLicenciement.set('2026-04-15');
    component.salaireMensuelBrutEur.set(2500);
    component.ancienneteAnnees.set(8);
    component.salarieAccidentTravail.set(true);
    component.dateConsolidationAT.set('2024-01-15');
    component.salarieDiscriminationAlleguee.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationLicenciement: '2026-04-15',
      salarieEnceinte: false,
      dateAccouchement: null,
      salarieAccidentTravail: true,
      dateConsolidationAT: '2024-01-15',
      salarieHarceleAvere: false,
      salarieDiscriminationAlleguee: true,
      salarieMotifLanceurAlerte: false,
      salarieMandatRepresentant: false,
      salarieActionJustice: false,
      salaireMensuelBrutEur: 2500,
      ancienneteAnnees: 8,
    });
    req.flush(elevatedResponse());

    expect(component.result()!.verdictProbabiliteNullite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Détection licenciement nul calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.dateNotificationLicenciement.set('2026-04-15');
    component.salaireMensuelBrutEur.set(2500);
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
  // Pré-fill IA
  // ---------------------------------------------------------------------------

  it('prefill IA : salaireBrutMensuel + dateLicenciement → valeurs + provenance IA', () => {
    component.aiData = {
      salaireBrutMensuel: 2800,
      dateLicenciement: '2026-04-10',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelBrutEur()).toBe(2800);
    expect(component.dateNotificationLicenciement()).toBe('2026-04-10');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('prefill IA : aiData absent → no-op gracieux', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('prefill IA : motifNullitePressenti=DISCRIMINATION → salarieDiscriminationAlleguee=true', () => {
    component.aiData = {
      motifNullitePressenti: 'DISCRIMINATION',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salarieDiscriminationAlleguee()).toBe(true);
    expect(component.provenanceProtections()).toBe('IA');
  });

  it('prefill IA : motifNullitePressenti=HARCELEMENT_MORAL → salarieHarceleAvere=true', () => {
    component.aiData = {
      motifNullitePressenti: 'HARCELEMENT_MORAL',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salarieHarceleAvere()).toBe(true);
    expect(component.provenanceProtections()).toBe('IA');
  });

  it('prefill IA : motifNullitePressenti=ACCIDENT_MP → salarieAccidentTravail=true', () => {
    component.aiData = {
      motifNullitePressenti: 'ACCIDENT_MP',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salarieAccidentTravail()).toBe(true);
  });

  it('onSalaireChange efface badge IA', () => {
    component.aiData = { salaireBrutMensuel: 2800 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(3000);
    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onDateNotificationChange efface badge IA', () => {
    component.aiData = { dateLicenciement: '2026-04-10' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDateNotification()).toBe('IA');
    component.onDateNotificationChange('2026-05-01');
    expect(component.dateNotificationLicenciement()).toBe('2026-05-01');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('onSalarieEnceinteChange(false) reset dateAccouchement', () => {
    component.salarieEnceinte.set(true);
    component.dateAccouchement.set('2026-03-01');
    component.onSalarieEnceinteChange(false);
    expect(component.salarieEnceinte()).toBe(false);
    expect(component.dateAccouchement()).toBeNull();
  });

  it('onSalarieAccidentTravailChange(false) reset dateConsolidationAT', () => {
    component.salarieAccidentTravail.set(true);
    component.dateConsolidationAT.set('2024-01-15');
    component.onSalarieAccidentTravailChange(false);
    expect(component.salarieAccidentTravail()).toBe(false);
    expect(component.dateConsolidationAT()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.SALAIRE présent si écart > 10 %', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(3500); // écart = 40 %

    const alert = component.coherenceAlerts().SALAIRE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('SALAIRE');
    expect(alert!.source).toBe('IA');
  });

  it('coherenceAlerts.SALAIRE absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(2600); // écart = 4 %

    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('coherenceAlerts.DATE_NOTIFICATION présent si divergence stricte', () => {
    component.aiData = { dateLicenciement: '2026-04-10' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateNotificationChange('2026-05-01');

    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toBe('2026-04-10');
  });

  it('coherenceAlerts.PROTECTIONS présent si IA détecte un motif et avocat n\'a pas coché', () => {
    component.aiData = { motifNullitePressenti: 'DISCRIMINATION' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // L'IA pré-remplit à true ; l'avocat repasse à false.
    component.onSalarieDiscriminationAllegueeChange(false);

    const alert = component.coherenceAlerts().PROTECTIONS;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toContain('Discrimination');
  });

  it('coherenceAlerts gate : alertes masquées après calcul (showForm=false)', () => {
    component.aiData = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(3500);
    expect(component.coherenceAlerts().SALAIRE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Verdict UI helpers
  // ---------------------------------------------------------------------------

  it('verdict ELEVEE → banner navy --available', () => {
    expect(component.verdictBannerClass('ELEVEE')).toContain('--available');
    expect(component.verdictBannerLabel('ELEVEE')).toContain('élevée');
    expect(component.verdictBannerIcon('ELEVEE')).toBe('check_circle');
  });

  it('verdict MOYENNE → banner or --medium', () => {
    expect(component.verdictBannerClass('MOYENNE')).toContain('--medium');
    expect(component.verdictBannerLabel('MOYENNE')).toContain('moyenne');
    expect(component.verdictBannerIcon('MOYENNE')).toBe('warning');
  });

  it('verdict FAIBLE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('FAIBLE')).toContain('--danger');
    expect(component.verdictBannerLabel('FAIBLE')).toContain('faible');
    expect(component.verdictBannerIcon('FAIBLE')).toBe('error');
  });

  // ---------------------------------------------------------------------------
  // Toggle / editMode
  // ---------------------------------------------------------------------------

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode ré-affiche le form après calcul', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges aiData
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      salaireBrutMensuel: 2700,
      dateLicenciement: '2026-04-12',
    } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.salaireMensuelBrutEur()).toBe(2700);
    expect(component.dateNotificationLicenciement()).toBe('2026-04-12');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onSalaireChange(3000);
    expect(component.provenanceSalaire()).toBeNull();

    const newAi = { salaireBrutMensuel: 2500 } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.provenanceSalaire()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Helpers chips / protections
  // ---------------------------------------------------------------------------

  it('protectionsList retourne les protections du result', () => {
    component.result.set(elevatedResponse());
    expect(component.protectionsList()).toEqual(['ACCIDENT_TRAVAIL', 'DISCRIMINATION']);
  });

  it('protectionChipClass retourne classe navy --available', () => {
    expect(component.protectionChipClass('MATERNITE')).toContain('--available');
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02b — mode standalone (CA-08, CA-09, CA-10).
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02b — mode standalone', () => {
    const STANDALONE_URL = '/api/v1/simulators/F-DT-16-licenciement-nul-detection/calculate';

    it('CA-08 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-08 : pas de bannière en mode case-file (standaloneMode=false)', () => {
      component.standaloneMode = false;
      fixture.detectChanges();
      const matches = httpMock.match(() => true);
      matches.forEach((r) => { try { r.flush({}, { status: 404, statusText: 'Not Found' }); } catch {} });
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).toBeNull();
    });

    it("CA-08 : coherenceAlerts() retourne vide en standalone", () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const alerts = (component as any).coherenceAlerts ? (component as any).coherenceAlerts() : {};
      expect(Object.keys(alerts)).toHaveLength(0);
    });

    it("CA-09 : exposition du service standalone (route dispatcher)", () => {
      // Garde-fou statique : le service expose le toolId du dispatcher.
      // L'intégration runtime est couverte par CA-09 manuel sur staging
      // (3 outils échantillonnés — cf. mini-spec).
      expect(LicenciementNulDetectionService.STANDALONE_TOOL_ID).toBe('F-DT-16-licenciement-nul-detection');
      expect(STANDALONE_URL).toContain(LicenciementNulDetectionService.STANDALONE_TOOL_ID);
    });
  });

});
