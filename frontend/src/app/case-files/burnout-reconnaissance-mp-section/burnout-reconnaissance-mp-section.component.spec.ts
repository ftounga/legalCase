import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BurnoutReconnaissanceMpSectionComponent } from './burnout-reconnaissance-mp-section.component';
import {
  BurnoutAnalyseChances,
  BurnoutReconnaissanceMpResponse,
} from '../../core/models/burnout-reconnaissance-mp.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('BurnoutReconnaissanceMpSectionComponent', () => {
  let component: BurnoutReconnaissanceMpSectionComponent;
  let fixture: ComponentFixture<BurnoutReconnaissanceMpSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/burnout-reconnaissance-mp';

  /** Fixture IA complète — les 4 champs `burnout*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    burnoutDiagnostic: true,
    burnoutTauxIpp: 35,
    burnoutSurchargeDocumentee: true,
    burnoutArretsMaladie: true,
  };

  function response(
    verdict: BurnoutAnalyseChances,
    conditionIPPRemplie = true,
    alerteIPP = false,
  ): BurnoutReconnaissanceMpResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire).
      diagnosticBurnoutPose: true,
      tauxIPPEstime: 30,
      anneesExpositionProfessionnelle: 7,
      surchargeChargeDocumentee: true,
      manquementsSecuriteDocumentes: true,
      harcelementConcomitant: false,
      arretsMaladieMultiples: true,
      lienCausalDirectEtabli: true,
      // Sorties calculées.
      analyseChancesCRRMP: verdict,
      scoreChances: verdict === 'CHANCES_IMPORTANTES' ? 90
                    : verdict === 'CHANCES_MODEREES' ? 55
                    : 20,
      conditionIPPRemplie,
      alerteIPPInsuffisante: alerteIPP,
      delaiInstructionMois: 4,
      facteursDossier: [
        {
          code: 'DT64_DIAGNOSTIC_POSE',
          libelle: 'Diagnostic posé',
          fondement: 'Circulaire DGT 2016/01',
          poids: 20,
        },
      ],
      basesJuridiques: ['L. 461-1 CSS', 'Circulaire DGT 2016/01'],
      messages: ['Analyse calculée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-25T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        BurnoutReconnaissanceMpSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BurnoutReconnaissanceMpSectionComponent);
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
  // Chargement (GET) et gate pays
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount BELGIQUE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('BELGIQUE → bannière info pays affichée', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="bo-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 4 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.diagnosticBurnoutPose()).toBe(true);
    expect(component.tauxIPPEstime()).toBe(35);
    expect(component.surchargeChargeDocumentee()).toBe(true);
    expect(component.arretsMaladieMultiples()).toBe(true);
    expect(component.provenanceDiagnostic()).toBe('IA');
    expect(component.provenanceTauxIpp()).toBe('IA');
    expect(component.provenanceSurcharge()).toBe('IA');
    expect(component.provenanceArretsMaladie()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    // Valeurs par défaut conservées.
    expect(component.diagnosticBurnoutPose()).toBe(false);
    expect(component.tauxIPPEstime()).toBe(0);
    expect(component.provenanceDiagnostic()).toBeNull();
  });

  it('getPrefillCount = 4 sur fixture IA FR complète', () => {
    expect(BurnoutReconnaissanceMpSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(BurnoutReconnaissanceMpSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(BurnoutReconnaissanceMpSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + alerte IPP
  // ---------------------------------------------------------------------------

  it('POST → verdict CHANCES_IMPORTANTES + refresh dashboard', () => {
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.diagnosticBurnoutPose).toBe(false);
    req.flush(response('CHANCES_IMPORTANTES'));

    expect(component.result()?.analyseChancesCRRMP).toBe('CHANCES_IMPORTANTES');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict CHANCES_MODEREES', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CHANCES_MODEREES'));
    expect(component.result()?.analyseChancesCRRMP).toBe('CHANCES_MODEREES');
  });

  it('POST → verdict CHANCES_FAIBLES + alerte IPP', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CHANCES_FAIBLES', false, true));
    expect(component.result()?.analyseChancesCRRMP).toBe('CHANCES_FAIBLES');
    expect(component.result()?.alerteIPPInsuffisante).toBe(true);
    expect(component.result()?.conditionIPPRemplie).toBe(false);
  });

  it('Alerte IPP affichée si alerteIPPInsuffisante=true', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CHANCES_FAIBLES', false, true));
    fixture.detectChanges();

    const alerte = fixture.nativeElement.querySelector('[data-testid="bo-alerte-ipp"]');
    expect(alerte).toBeTruthy();
    expect(alerte.textContent).toContain('25');
  });

  it('Délai instruction CRRMP 4-6 mois affiché', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CHANCES_IMPORTANTES'));
    fixture.detectChanges();

    const delaiBlock = fixture.nativeElement.querySelector('[data-testid="bo-figure-delai"]');
    expect(delaiBlock).toBeTruthy();
    expect(delaiBlock.textContent).toContain('4 à 6 mois');
  });

  // ---------------------------------------------------------------------------
  // formValid + bornes IPP
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si taux IPP > 100', () => {
    component.tauxIPPEstime.set(150);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si taux IPP < 0', () => {
    component.tauxIPPEstime.set(-5);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si années expo négatives', () => {
    component.anneesExpositionProfessionnelle.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true sur baseline', () => {
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers — effacement du badge IA sur modif manuelle
  // ---------------------------------------------------------------------------

  it('onDiagnosticChange efface le badge IA', () => {
    component.provenanceDiagnostic.set('IA');
    component.onDiagnosticChange(true);
    expect(component.provenanceDiagnostic()).toBeNull();
  });

  it('onTauxIppChange efface le badge IA', () => {
    component.provenanceTauxIpp.set('IA');
    component.onTauxIppChange(40);
    expect(component.provenanceTauxIpp()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('CHANCES_IMPORTANTES')).toContain('--importantes');
    expect(component.verdictBannerClass('CHANCES_MODEREES')).toContain('--moderees');
    expect(component.verdictBannerClass('CHANCES_FAIBLES')).toContain('--faibles');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('CHANCES_IMPORTANTES')).toBe('Chances importantes');
    expect(component.verdictBannerLabel('CHANCES_MODEREES')).toBe('Chances modérées');
    expect(component.verdictBannerLabel('CHANCES_FAIBLES')).toBe('Chances faibles');
  });

  it('formatDelaiInstruction retourne "4 à 6 mois"', () => {
    expect(component.formatDelaiInstruction()).toBe('4 à 6 mois');
  });
});
