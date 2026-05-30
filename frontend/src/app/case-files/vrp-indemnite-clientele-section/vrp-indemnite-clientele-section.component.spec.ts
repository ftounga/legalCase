import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { VrpIndemniteClienteleSectionComponent } from './vrp-indemnite-clientele-section.component';
import {
  VrpEligibiliteClientele,
  VrpIndemniteClienteleResponse,
} from '../../core/models/vrp-indemnite-clientele.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('VrpIndemniteClienteleSectionComponent', () => {
  let component: VrpIndemniteClienteleSectionComponent;
  let fixture: ComponentFixture<VrpIndemniteClienteleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/vrp-indemnite-clientele-analysis';

  /** Fixture IA complète — les 3 champs pré-remplissables F-DT-104. */
  const FULL_AI_DATA: TravailExtractedData = {
    dateEntree: '2020-01-01',
    dateRuptureContrat: '2023-06-30',
    vrpCommissionsAnnuelles: 48000,
  };

  function response(eligibilite: VrpEligibiliteClientele): VrpIndemniteClienteleResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé (ré-édition du formulaire).
      dateEntree: '2020-01-01',
      dateRupture: '2023-06-30',
      causeRupture: eligibilite === 'DUE' ? 'LICENCIEMENT_CAUSE_REELLE' : 'FAUTE_GRAVE',
      typeVrp: 'EXCLUSIF',
      commissionsAnnuellesMoyennes: 48000,
      salaireMensuelMoyen: 4000,
      clienteleDeveloppee: true,
      // Champs calculés.
      dureePreavisMois: 3,
      eligibiliteClientele: eligibilite,
      motifNonDue: eligibilite === 'NON_DUE'
        ? 'La faute grave exclut le droit à l\'indemnité de clientèle (L.7313-13).'
        : null,
      indemniteClienteleMin: 48000,
      indemniteClienteleMax: 96000,
      indemniteLegaleLicenciement: 3500,
      optionRecommandee: eligibilite === 'DUE' ? 'INDEMNITE_CLIENTELE' : 'INDEMNITE_LEGALE',
      baseJuridique: 'Art. L.7311-1 et s. ; L.7313-13 ; L.7313-9 C. trav.',
      country: 'FRANCE',
      calculatedAt: '2026-05-30T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        VrpIndemniteClienteleSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VrpIndemniteClienteleSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Chargement (GET) + gate FR
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount BELGIQUE → pas d\'appel HTTP (gate FR)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('formValid() false en BELGIQUE même avec dates valides (gate FR)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.dateEntree.set('2020-01-01');
    component.dateRupture.set('2023-06-30');
    expect(component.formValid()).toBe(false);
  });

  it('rendu formulaire après 404 + bannière BE si pays ≠ FR', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="vrp-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Validation du formulaire
  // ---------------------------------------------------------------------------

  it('formValid() false si dateRupture < dateEntree', () => {
    component.dateEntree.set('2023-06-30');
    component.dateRupture.set('2020-01-01');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() true si FR + dates cohérentes + montants ≥ 0', () => {
    component.dateEntree.set('2020-01-01');
    component.dateRupture.set('2023-06-30');
    component.commissionsAnnuellesMoyennes.set(48000);
    component.salaireMensuelMoyen.set(4000);
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — éligibilité DUE / NON_DUE + préavis + option
  // ---------------------------------------------------------------------------

  it('POST → éligibilité DUE rendue (bannière navy, pas de motif)', () => {
    component.dateEntree.set('2020-01-01');
    component.dateRupture.set('2023-06-30');
    component.commissionsAnnuellesMoyennes.set(48000);
    component.salaireMensuelMoyen.set(4000);
    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.causeRupture).toBe('LICENCIEMENT_CAUSE_REELLE');
    req.flush(response('DUE'));
    expect(component.result()!.eligibiliteClientele).toBe('DUE');
    expect(component.eligibiliteBannerClass('DUE')).toContain('vrp-verdict-banner--due');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST FAUTE_GRAVE → éligibilité NON_DUE + motif + bannière rouge', () => {
    component.dateEntree.set('2020-01-01');
    component.dateRupture.set('2023-06-30');
    component.causeRupture.set('FAUTE_GRAVE');
    component.commissionsAnnuellesMoyennes.set(48000);
    component.salaireMensuelMoyen.set(4000);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('NON_DUE'));
    expect(component.result()!.eligibiliteClientele).toBe('NON_DUE');
    expect(component.result()!.motifNonDue).toContain('faute grave');
    expect(component.eligibiliteBannerClass('NON_DUE')).toContain('vrp-verdict-banner--non-due');
  });

  it('préavis affiché = dureePreavisMois (ancienneté > 2 ans → 3 mois)', () => {
    component.dateEntree.set('2020-01-01');
    component.dateRupture.set('2023-06-30');
    component.commissionsAnnuellesMoyennes.set(48000);
    component.salaireMensuelMoyen.set(4000);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DUE'));
    expect(component.result()!.dureePreavisMois).toBe(3);
  });

  it('option recommandée label correct (INDEMNITE_CLIENTELE)', () => {
    expect(component.optionRecommandeeLabel('INDEMNITE_CLIENTELE')).toContain('clientèle');
    expect(component.optionRecommandeeLabel('INDEMNITE_LEGALE')).toContain('légale');
  });

  it('rendu résultat affiche fourchette + disclaimer juge + option', () => {
    component.forceExpanded = true;
    fixture.detectChanges(); // déclenche ngOnInit → GET initial
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateEntree.set('2020-01-01');
    component.dateRupture.set('2023-06-30');
    component.commissionsAnnuellesMoyennes.set(48000);
    component.salaireMensuelMoyen.set(4000);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('DUE'));
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="vrp-fourchette-clientele"]')?.textContent).toContain('48');
    expect(el.querySelector('[data-testid="vrp-option-recommandee"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="vrp-fr-banner"]')).toBeTruthy();
    expect(el.textContent).toContain('évaluation souveraine du juge');
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + provenance + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-fill IA → dateEntree / dateRupture / commissions + badges provenance', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.dateEntree()).toBe('2020-01-01');
    expect(component.dateRupture()).toBe('2023-06-30');
    expect(component.commissionsAnnuellesMoyennes()).toBe(48000);
    expect(component.provenanceDateEntree()).toBe('IA');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.provenanceCommissions()).toBe('IA');
  });

  it('modification manuelle d\'un champ pré-rempli efface le badge IA', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onCommissionsChange(60000);
    expect(component.commissionsAnnuellesMoyennes()).toBe(60000);
    expect(component.provenanceCommissions()).toBeNull();
  });

  it('getPrefillCount = 0 (aucune donnée IA)', () => {
    expect(VrpIndemniteClienteleSectionComponent.getPrefillCount({
      aiData: null, workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('getPrefillCount = partiel (1 champ)', () => {
    expect(VrpIndemniteClienteleSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2020-01-01' }, workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('getPrefillCount = nominal (3 champs)', () => {
    expect(VrpIndemniteClienteleSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA, workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('getPrefillCount = 0 hors FRANCE (gate)', () => {
    expect(VrpIndemniteClienteleSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA, workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('pré-fill no-op en standaloneMode', () => {
    component.standaloneMode = true;
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.dateEntree()).toBeNull();
    expect(component.commissionsAnnuellesMoyennes()).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // F-IA-03 — alertes de cohérence sur les dates
  // ---------------------------------------------------------------------------

  it('F-IA-03 : divergence dateEntree vs F-96 → alerte DATE_ENTREE', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date entrée VRP', statut: 'NON_COMPLIANT',
      critereCode: 'DT104_DATE_ENTREE', expectedValue: '2019-09-01', raison: 'Contrat antérieur',
    }];
    component.procedureChecks = checks;
    component.dateEntree.set('2020-01-01');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_ENTREE).toBeTruthy();
    expect(alerts.DATE_ENTREE!.source).toBe('F96');
  });

  it('F-IA-03 : aucune alerte en standaloneMode', () => {
    component.standaloneMode = true;
    component.procedureChecks = [{
      id: 'c1', ordre: 1, description: 'x', statut: 'NON_COMPLIANT',
      critereCode: 'DT104_DATE_ENTREE', expectedValue: '2019-09-01',
    }];
    component.dateEntree.set('2020-01-01');
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(Object.keys(component.coherenceAlerts()).length).toBe(0);
  });
});
