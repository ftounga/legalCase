import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TeletravailAccordSectionComponent } from './teletravail-accord-section.component';
import {
  TeletravailAnalyse,
  TeletravailAccordResponse,
} from '../../core/models/teletravail-accord.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('TeletravailAccordSectionComponent', () => {
  let component: TeletravailAccordSectionComponent;
  let fixture: ComponentFixture<TeletravailAccordSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/teletravail-accord';

  /** Fixture IA complète — les 7 champs `teletravail*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    teletravailCadre: 'ACCORD_COLLECTIF',
    teletravailDoubleVolontariat: true,
    teletravailIndemniteVersee: true,
    teletravailMontantIndemniteJournalier: 2.60,
    teletravailAccidentDomicile: false,
    teletravailRetourBureauImpose: false,
    teletravailRefusCauseIncrimination: false,
  };

  function response(
    analyse: TeletravailAnalyse,
    extras?: Partial<TeletravailAccordResponse>,
  ): TeletravailAccordResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire après recharge).
      cadreTeletravail: analyse === 'NON_CONFORME' ? 'AUCUN' : 'ACCORD_COLLECTIF',
      doubleVolontariatRespectee: true,
      indemniteOccupationVersee: true,
      montantIndemniteJournalierEuros: 2.60,
      accidentDomicileDetecte: false,
      retourBureauImposeUnilateralement: false,
      refusTeletravailCauseIncrimination: false,
      // Sorties calculées.
      analyseTeletravail: analyse,
      scoreTeletravail: analyse === 'CONFORME' ? 90
                       : analyse === 'NON_CONFORME' ? 10
                       : 50,
      pointsTeletravail: [
        {
          code: 'DT82_CADRE_JURIDIQUE',
          libelle: 'Accord collectif encadrant le télétravail',
          fondement: 'L. 1222-9 al. 1 CT',
          conclusion: 'OK — cadre juridique le plus protecteur.',
        },
      ],
      alerteAccidentTravailDomicile: false,
      alerteRefusTeletravailLicenciement: false,
      indemniteDueEstimeeEuros: null,
      basesJuridiques: ['L. 1222-9 à L. 1222-11 CT', 'ANI télétravail 26/11/2020'],
      messages: ['Analyse calculée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-24T10:00:00Z',
      ...extras,
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        TeletravailAccordSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TeletravailAccordSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // F-JU-03 — absorbe les requêtes /jurisprudence-citations émises par
    // ToolJurisprudenceCitationsComponent (imports[] du composant).
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
    const banner = fixture.nativeElement.querySelector('[data-testid="tta-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 7 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.cadreTeletravail()).toBe('ACCORD_COLLECTIF');
    expect(component.doubleVolontariatRespectee()).toBe(true);
    expect(component.indemniteOccupationVersee()).toBe(true);
    expect(component.montantIndemniteJournalierEuros()).toBe(2.60);
    expect(component.accidentDomicileDetecte()).toBe(false);
    expect(component.retourBureauImposeUnilateralement()).toBe(false);
    expect(component.refusTeletravailCauseIncrimination()).toBe(false);
    expect(component.provenanceCadre()).toBe('IA');
    expect(component.provenanceMontant()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.provenanceCadre()).toBeNull();
  });

  it('getPrefillCount = 7 sur fixture IA FR complète', () => {
    expect(TeletravailAccordSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(7);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(TeletravailAccordSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(TeletravailAccordSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + alertes
  // ---------------------------------------------------------------------------

  it('POST → verdict CONFORME + refresh dashboard', () => {
    component.cadreTeletravail.set('ACCORD_COLLECTIF');
    component.doubleVolontariatRespectee.set(true);
    component.indemniteOccupationVersee.set(true);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.cadreTeletravail).toBe('ACCORD_COLLECTIF');
    req.flush(response('CONFORME'));

    expect(component.result()?.analyseTeletravail).toBe('CONFORME');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict NON_CONFORME (cadre AUCUN)', () => {
    component.cadreTeletravail.set('AUCUN');
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('NON_CONFORME'));
    expect(component.result()?.analyseTeletravail).toBe('NON_CONFORME');
  });

  it('POST → verdict LITIGE_IDENTIFIE (accident)', () => {
    component.accidentDomicileDetecte.set(true);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('LITIGE_IDENTIFIE', {
      alerteAccidentTravailDomicile: true,
    }));
    expect(component.result()?.analyseTeletravail).toBe('LITIGE_IDENTIFIE');
  });

  it('Verdict banner affiché après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CONFORME'));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="tta-verdict-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Alertes distinctes (UX critique)
  // ---------------------------------------------------------------------------

  it('Alerte accident à domicile affichée distinctement', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('LITIGE_IDENTIFIE', {
      alerteAccidentTravailDomicile: true,
    }));
    fixture.detectChanges();
    const acc = fixture.nativeElement.querySelector('[data-testid="tta-alerte-accident"]');
    expect(acc).toBeTruthy();
    const refus = fixture.nativeElement.querySelector('[data-testid="tta-alerte-refus"]');
    expect(refus).toBeFalsy();
  });

  it('Alerte refus-licenciement affichée distinctement', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('LITIGE_IDENTIFIE', {
      alerteRefusTeletravailLicenciement: true,
    }));
    fixture.detectChanges();
    const refus = fixture.nativeElement.querySelector('[data-testid="tta-alerte-refus"]');
    expect(refus).toBeTruthy();
    const acc = fixture.nativeElement.querySelector('[data-testid="tta-alerte-accident"]');
    expect(acc).toBeFalsy();
  });

  it('Les 2 alertes peuvent coexister', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('LITIGE_IDENTIFIE', {
      alerteAccidentTravailDomicile: true,
      alerteRefusTeletravailLicenciement: true,
    }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="tta-alerte-accident"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="tta-alerte-refus"]')).toBeTruthy();
  });

  it('Affiche l\'indemnité due estimée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('NON_CONFORME', {
      indemniteDueEstimeeEuros: 520.0,
    }));
    fixture.detectChanges();
    const fig = fixture.nativeElement.querySelector('[data-testid="tta-indemnite-due"]');
    expect(fig).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true avec valeurs par défaut', () => {
    expect(component.formValid()).toBe(true);
  });

  it('formValid = false si montant négatif', () => {
    component.montantIndemniteJournalierEuros.set(-1);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('CONFORME')).toContain('--conforme');
    expect(component.verdictBannerClass('NON_CONFORME')).toContain('--non-conforme');
    expect(component.verdictBannerClass('LITIGE_IDENTIFIE')).toContain('--litige');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('CONFORME')).toContain('conforme');
    expect(component.verdictBannerLabel('NON_CONFORME')).toContain('non conforme');
    expect(component.verdictBannerLabel('LITIGE_IDENTIFIE')).toContain('Litige');
  });

  it('Handler cadre efface la provenance IA', () => {
    component.provenanceCadre.set('IA');
    component.onCadreChange('AUCUN');
    expect(component.cadreTeletravail()).toBe('AUCUN');
    expect(component.provenanceCadre()).toBeNull();
  });

  it('Handler montant accepte une valeur décimale', () => {
    component.onMontantChange(2.60);
    expect(component.montantIndemniteJournalierEuros()).toBe(2.60);
  });

  it('Handler montant rejette les valeurs négatives (ramène à null)', () => {
    component.onMontantChange(-1);
    expect(component.montantIndemniteJournalierEuros()).toBeNull();
  });

  it('Handler montant accepte null (vide le champ)', () => {
    component.montantIndemniteJournalierEuros.set(2.60);
    component.onMontantChange(null);
    expect(component.montantIndemniteJournalierEuros()).toBeNull();
  });
});
