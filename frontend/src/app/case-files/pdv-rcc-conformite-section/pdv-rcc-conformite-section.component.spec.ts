import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PdvRccConformiteSectionComponent } from './pdv-rcc-conformite-section.component';
import {
  PdvRccAnalyse,
  PdvRccConformiteResponse,
} from '../../core/models/pdv-rcc-conformite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('PdvRccConformiteSectionComponent', () => {
  let component: PdvRccConformiteSectionComponent;
  let fixture: ComponentFixture<PdvRccConformiteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/pdv-rcc-conformite';

  /** Fixture IA complète — les 4 champs du sous-objet `pdvRccDetail`. */
  const FULL_AI_DATA: TravailExtractedData = {
    pdvRccDetail: {
      pdvRccTypeDispositif: 'RCC',
      pdvRccAccordMajoritaire: true,
      pdvRccValidationDREETS: true,
      pdvRccIndemnitesLegales: true,
    },
  };

  function response(
    analyse: PdvRccAnalyse,
    droitAreConfirme = false,
  ): PdvRccConformiteResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs.
      typeDispositif: 'RCC',
      accordCollectifMajoritaire: analyse !== 'NON_CONFORME',
      validationDREETSObtenue: analyse !== 'NON_CONFORME',
      delaiValidation15JRespectee: analyse === 'CONFORME' ? true : false,
      indemnitesAuMoinsLegales: analyse !== 'NON_CONFORME',
      adhesionVolontaireIndividuelle: analyse !== 'NON_CONFORME',
      informationIndividuelleComplete: analyse === 'CONFORME',
      // Sorties calculées.
      analysePdvRcc: analyse,
      scoreConformite: analyse === 'CONFORME' ? 100
                     : analyse === 'PARTIELLEMENT_CONFORME' ? 70
                     : 30,
      pointsIrregularite: analyse === 'CONFORME' ? [] : [
        {
          code: 'DT46_VALIDATION_DREETS',
          libelle: 'Délai DREETS dépassé',
          fondement: 'L. 1237-19-3 CT',
        },
      ],
      droitAreConfirme,
      basesJuridiques: ['L. 1237-19 CT', 'Décret 2017-1718 du 20/12/2017'],
      messages: ['Analyse calculée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-24T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        PdvRccConformiteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PdvRccConformiteSectionComponent);
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
    const banner = fixture.nativeElement.querySelector('[data-testid="prc-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 4 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.typeDispositif()).toBe('RCC');
    expect(component.accordCollectifMajoritaire()).toBe(true);
    expect(component.validationDREETSObtenue()).toBe(true);
    expect(component.indemnitesAuMoinsLegales()).toBe(true);
    expect(component.provenanceType()).toBe('IA');
    expect(component.provenanceAccord()).toBe('IA');
    expect(component.provenanceDreets()).toBe('IA');
    expect(component.provenanceIndemnites()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.provenanceType()).toBeNull();
  });

  it('getPrefillCount = 4 sur fixture IA FR complète', () => {
    expect(PdvRccConformiteSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(PdvRccConformiteSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(PdvRccConformiteSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sur aiData sans pdvRccDetail (cas dette F-DT-46 actuel)', () => {
    expect(PdvRccConformiteSectionComponent.getPrefillCount({
      aiData: {},
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux
  // ---------------------------------------------------------------------------

  it('POST → verdict CONFORME + droit ARE + refresh dashboard', () => {
    component.typeDispositif.set('RCC');
    component.accordCollectifMajoritaire.set(true);
    component.validationDREETSObtenue.set(true);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.typeDispositif).toBe('RCC');
    req.flush(response('CONFORME', true));

    expect(component.result()?.analysePdvRcc).toBe('CONFORME');
    expect(component.result()?.droitAreConfirme).toBe(true);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict PARTIELLEMENT_CONFORME', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PARTIELLEMENT_CONFORME'));
    expect(component.result()?.analysePdvRcc).toBe('PARTIELLEMENT_CONFORME');
    expect(component.result()?.droitAreConfirme).toBe(false);
  });

  it('POST → verdict NON_CONFORME', () => {
    component.accordCollectifMajoritaire.set(false);
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('NON_CONFORME'));
    expect(component.result()?.analysePdvRcc).toBe('NON_CONFORME');
    expect(component.result()?.droitAreConfirme).toBe(false);
  });

  it('Verdict banner affiché après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CONFORME', true));
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="prc-verdict-banner"]');
    expect(banner).toBeTruthy();
  });

  // ── INVARIANT produit : badge droit ARE visible si et seulement si conforme

  it('Badge droit ARE visible si RCC CONFORME', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CONFORME', true));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="prc-badge-are"]');
    expect(badge).toBeTruthy();
    expect(badge.textContent).toContain('ARE');
  });

  it('Badge droit ARE absent si NON_CONFORME', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('NON_CONFORME'));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="prc-badge-are"]');
    expect(badge).toBeFalsy();
  });

  it('Badge droit ARE absent si PARTIELLEMENT_CONFORME', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PARTIELLEMENT_CONFORME'));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="prc-badge-are"]');
    expect(badge).toBeFalsy();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true avec valeurs par défaut (RCC)', () => {
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('CONFORME')).toContain('--conforme');
    expect(component.verdictBannerClass('PARTIELLEMENT_CONFORME')).toContain('--partiel');
    expect(component.verdictBannerClass('NON_CONFORME')).toContain('--nonconforme');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('CONFORME')).toContain('conforme');
    expect(component.verdictBannerLabel('PARTIELLEMENT_CONFORME')).toContain('partiellement');
    expect(component.verdictBannerLabel('NON_CONFORME')).toContain('non conforme');
  });

  it('Handler type efface la provenance IA', () => {
    component.provenanceType.set('IA');
    component.onTypeChange('PDV');
    expect(component.typeDispositif()).toBe('PDV');
    expect(component.provenanceType()).toBeNull();
  });

  it('Handler accord efface la provenance IA', () => {
    component.provenanceAccord.set('IA');
    component.onAccordChange(true);
    expect(component.accordCollectifMajoritaire()).toBe(true);
    expect(component.provenanceAccord()).toBeNull();
  });

  it('onDreetsChange(false) réinitialise le délai 15 j', () => {
    component.delaiValidation15JRespectee.set(true);
    component.onDreetsChange(false);
    expect(component.delaiValidation15JRespectee()).toBeNull();
  });

  it('Handler indemnités efface la provenance IA', () => {
    component.provenanceIndemnites.set('IA');
    component.onIndemnitesChange(true);
    expect(component.indemnitesAuMoinsLegales()).toBe(true);
    expect(component.provenanceIndemnites()).toBeNull();
  });
});
