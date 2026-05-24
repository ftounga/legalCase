import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TransfertEntrepriseFrSectionComponent } from './transfert-entreprise-fr-section.component';
import {
  TransfertEntrepriseFrResponse,
  TransfertEntrepriseFrVerdict,
} from '../../core/models/transfert-entreprise-fr.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('TransfertEntrepriseFrSectionComponent', () => {
  let component: TransfertEntrepriseFrSectionComponent;
  let fixture: ComponentFixture<TransfertEntrepriseFrSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/transfert-entreprise-l1224-1';

  /** Fixture IA complète — les 5 champs `transfert*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    transfertTypeTransfert: 'FUSION',
    transfertEeaIdentifiee: true,
    transfertActivitePreservee: true,
    transfertLicenciementsPreTransfert: false,
    transfertDateTransfert: '2024-09-01',
  };

  function response(
    verdict: TransfertEntrepriseFrVerdict,
  ): TransfertEntrepriseFrResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire après recharge).
      typeTransfert: 'CESSION',
      eeaIdentifieeAvantTransfert: verdict !== 'L1224_INAPPLICABLE',
      activiteEconomiquePreservee: verdict === 'L1224_APPLICABLE',
      salariesTransferes: true,
      contratsModifiesParRepreneur: false,
      licenciementsPreTransfert: false,
      nbLicenciementsPreTransfert: 0,
      informationConsultationCseRealisee: true,
      dateTransfert: null,
      // Sorties calculées.
      analyseL1224_1: verdict,
      scoreApplicabilite: verdict === 'L1224_APPLICABLE' ? 85
                         : verdict === 'L1224_INCERTAIN' ? 45
                         : 15,
      pointsAnalyse: [
        {
          code: 'DT72_EEA_IDENTIFIEE',
          libelle: 'EEA identifiée',
          fondement: 'Cass. soc. 18/07/2000',
          conclusion: 'Test conclusion.',
        },
      ],
      alerteLicenciementsFrauduleux: false,
      alerteDefautConsultationCse: false,
      basesJuridiques: ['L. 1224-1 CT', 'L. 1224-3 CT'],
      messages: ['Analyse calculée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-23T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        TransfertEntrepriseFrSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TransfertEntrepriseFrSectionComponent);
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
    const banner = fixture.nativeElement.querySelector('[data-testid="tef-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-fill FRANCE : 5 champs renseignés depuis aiData + provenance IA', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.typeTransfert()).toBe('FUSION');
    expect(component.eeaIdentifieeAvantTransfert()).toBe(true);
    expect(component.activiteEconomiquePreservee()).toBe(true);
    expect(component.licenciementsPreTransfert()).toBe(false);
    expect(component.dateTransfert()).toBe('2024-09-01');
    expect(component.provenanceTypeTransfert()).toBe('IA');
    expect(component.provenanceEea()).toBe('IA');
    expect(component.provenanceActivite()).toBe('IA');
    expect(component.provenanceLicenciementsPre()).toBe('IA');
    expect(component.provenanceDateTransfert()).toBe('IA');
  });

  it('pré-fill BELGIQUE → 0 champ renseigné (gate FR-only)', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    expect(component.typeTransfert()).toBe('CESSION'); // valeur par défaut
    expect(component.provenanceTypeTransfert()).toBeNull();
  });

  it('getPrefillCount FRANCE complet = 5', () => {
    const n = TransfertEntrepriseFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(5);
  });

  it('getPrefillCount BELGIQUE = 0 (gate FR-only)', () => {
    const n = TransfertEntrepriseFrSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    });
    expect(n).toBe(0);
  });

  it('getPrefillCount aiData partiel = nombre exact de champs présents', () => {
    const n = TransfertEntrepriseFrSectionComponent.getPrefillCount({
      aiData: {
        transfertTypeTransfert: 'CESSION',
        transfertEeaIdentifiee: true,
      } as TravailExtractedData,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(2);
  });

  it('getPrefillCount type transfert hors whitelist ignoré', () => {
    const n = TransfertEntrepriseFrSectionComponent.getPrefillCount({
      aiData: {
        transfertTypeTransfert: 'LIQUIDATION_JUDICIAIRE',
      } as TravailExtractedData,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(0);
  });

  it('getPrefillCount date non ISO ignorée', () => {
    const n = TransfertEntrepriseFrSectionComponent.getPrefillCount({
      aiData: {
        transfertDateTransfert: '01/09/2024',
      } as TravailExtractedData,
      workspaceCountry: 'FRANCE',
    });
    expect(n).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Validation du formulaire
  // ---------------------------------------------------------------------------

  it('formValid FRANCE par défaut → true', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.formValid()).toBe(true);
  });

  it('formValid BELGIQUE → false', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Verdict + alertes + bannière
  // ---------------------------------------------------------------------------

  it('verdictBannerClass / label / icon — 3 niveaux', () => {
    expect(component.verdictBannerLabel('L1224_APPLICABLE')).toContain('applicable');
    expect(component.verdictBannerLabel('L1224_INCERTAIN')).toContain('incertain');
    expect(component.verdictBannerLabel('L1224_INAPPLICABLE')).toContain('inapplicable');
    expect(component.verdictBannerClass('L1224_APPLICABLE')).toContain('applicable');
    expect(component.verdictBannerClass('L1224_INCERTAIN')).toContain('incertain');
    expect(component.verdictBannerClass('L1224_INAPPLICABLE')).toContain('inapplicable');
    expect(component.verdictBannerIcon('L1224_APPLICABLE')).toBe('check_circle');
    expect(component.verdictBannerIcon('L1224_INCERTAIN')).toBe('help_outline');
    expect(component.verdictBannerIcon('L1224_INAPPLICABLE')).toBe('cancel');
  });

  it('result L1224_APPLICABLE → bannière + verdict', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const get = httpMock.expectOne(BASE_URL);
    get.flush(response('L1224_APPLICABLE'));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="tef-verdict-banner"]');
    expect(banner).toBeTruthy();
    expect(banner.textContent.toLowerCase()).toContain('applicable');
  });

  it('alerte licenciements frauduleux affichée si alerteLicenciementsFrauduleux=true', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const get = httpMock.expectOne(BASE_URL);
    const r = response('L1224_APPLICABLE');
    r.alerteLicenciementsFrauduleux = true;
    get.flush(r);
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('[data-testid="tef-alert-frauduleux"]');
    expect(alert).toBeTruthy();
  });

  it('alerte défaut CSE affichée si alerteDefautConsultationCse=true', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const get = httpMock.expectOne(BASE_URL);
    const r = response('L1224_APPLICABLE');
    r.alerteDefautConsultationCse = true;
    get.flush(r);
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('[data-testid="tef-alert-cse"]');
    expect(alert).toBeTruthy();
  });

  it('alertes distinctes visuellement (classes CSS différentes)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const get = httpMock.expectOne(BASE_URL);
    const r = response('L1224_APPLICABLE');
    r.alerteLicenciementsFrauduleux = true;
    r.alerteDefautConsultationCse = true;
    get.flush(r);
    fixture.detectChanges();
    const frauduleux = fixture.nativeElement.querySelector('[data-testid="tef-alert-frauduleux"]');
    const cse = fixture.nativeElement.querySelector('[data-testid="tef-alert-cse"]');
    expect(frauduleux).toBeTruthy();
    expect(cse).toBeTruthy();
    expect(frauduleux.className).toContain('frauduleux');
    expect(cse.className).toContain('cse');
  });

  // ---------------------------------------------------------------------------
  // Soumission du formulaire (POST)
  // ---------------------------------------------------------------------------

  it('calculate → POST avec snapshot d\'inputs + refresh dashboard', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.typeTransfert.set('FUSION');
    component.eeaIdentifieeAvantTransfert.set(true);
    component.activiteEconomiquePreservee.set(true);
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.typeTransfert).toBe('FUSION');
    expect(post.request.body.eeaIdentifieeAvantTransfert).toBe(true);
    expect(post.request.body.activiteEconomiquePreservee).toBe(true);
    post.flush(response('L1224_APPLICABLE'));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    expect(component.showForm()).toBe(false);
  });

  it('calculate erreur backend → snackBar erreur + formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalled();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers — modification manuelle efface le badge IA
  // ---------------------------------------------------------------------------

  it('onTypeTransfertChange manuel → badge IA effacé', () => {
    component.provenanceTypeTransfert.set('IA');
    component.onTypeTransfertChange('FUSION');
    expect(component.typeTransfert()).toBe('FUSION');
    expect(component.provenanceTypeTransfert()).toBeNull();
  });

  it('onLicenciementsPreChange(false) reset nb à 0', () => {
    component.nbLicenciementsPreTransfert.set(5);
    component.onLicenciementsPreChange(false);
    expect(component.nbLicenciementsPreTransfert()).toBe(0);
  });

  it('onEeaChange manuel → badge IA effacé', () => {
    component.provenanceEea.set('IA');
    component.onEeaChange(false);
    expect(component.eeaIdentifieeAvantTransfert()).toBe(false);
    expect(component.provenanceEea()).toBeNull();
  });

  it('onNbLicenciementsChange négatif → 0', () => {
    component.onNbLicenciementsChange(-5);
    expect(component.nbLicenciementsPreTransfert()).toBe(0);
  });
});
