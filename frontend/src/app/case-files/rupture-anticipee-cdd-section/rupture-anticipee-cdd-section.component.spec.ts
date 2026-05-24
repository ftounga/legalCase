import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RuptureAnticipeeCddSectionComponent } from './rupture-anticipee-cdd-section.component';
import {
  RacAnalyse,
  RuptureAnticipeeCddResponse,
} from '../../core/models/rupture-anticipee-cdd.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('RuptureAnticipeeCddSectionComponent', () => {
  let component: RuptureAnticipeeCddSectionComponent;
  let fixture: ComponentFixture<RuptureAnticipeeCddSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/rupture-anticipee-cdd';

  // Pas de pré-fill IA actuellement pour F-DT-43 — cf. doc d'en-tête du
  // helper rupture-anticipee-cdd-section-prefill-rules.ts.
  const FULL_AI_DATA: TravailExtractedData = {};

  function response(analyse: RacAnalyse): RuptureAnticipeeCddResponse {
    return {
      caseFileId: 'case-1',
      auteurRupture: analyse === 'ILLEGITIME_SALARIE' ? 'SALARIE' : 'EMPLOYEUR',
      motifRupture: analyse === 'LEGITIME' ? 'ACCORD_PARTIES' : 'AUTRE',
      dateTermeCdd: '2026-07-01',
      dateRupture: '2026-01-01',
      salaireMensuelBrutEuros: 2500.0,
      remunerationBruteTotaleContratEuros: 10000.0,
      analyseRuptureAnticipee: analyse,
      moisRestantsContrat: 6,
      pointsAnalyse: [
        {
          code: 'DT43_AUTEUR_RUPTURE',
          libelle: 'Rupture à l\'initiative de l\'EMPLOYEUR',
          fondement: 'L. 1243-1 CT',
          conclusion: 'OK',
        },
      ],
      indemnitesRuptureAnticipeeEuros: analyse === 'ILLEGITIME_EMPLOYEUR' ? 15000.0 : 0.0,
      indemnitePrecariteEuros: analyse === 'ILLEGITIME_EMPLOYEUR' ? 1000.0 : 0.0,
      totalDuSalarieEuros: analyse === 'ILLEGITIME_EMPLOYEUR' ? 16000.0 : 0.0,
      basesJuridiques: ['L. 1243-1 CT', 'L. 1243-4 CT'],
      messages: [],
      country: 'FRANCE',
      calculatedAt: new Date().toISOString(),
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [RuptureAnticipeeCddSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(RuptureAnticipeeCddSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // F-JU-03 SF-JU-03-99b — absorber les requêtes /jurisprudence-citations
    httpMock.match(req => req.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ citations: [] }));
    httpMock.verify();
  });

  it('crée le composant', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialisation FR', () => {
    it('GET sur init → 204 reste sur le formulaire', () => {
      component.workspaceCountry = 'FRANCE';
      component.forceExpanded = true;
      fixture.detectChanges();

      const req = httpMock.expectOne(BASE_URL);
      expect(req.request.method).toBe('GET');
      req.flush(null, { status: 204, statusText: 'No Content' });

      expect(component.result()).toBeNull();
      expect(component.showForm()).toBe(true);
    });

    it('GET sur init → 200 hydrate le snapshot', () => {
      component.workspaceCountry = 'FRANCE';
      component.forceExpanded = true;
      fixture.detectChanges();

      const req = httpMock.expectOne(BASE_URL);
      req.flush(response('ILLEGITIME_EMPLOYEUR'));

      expect(component.result()).toBeTruthy();
      expect(component.showForm()).toBe(false);
    });
  });

  describe('Pré-fill IA', () => {
    it('Pas de pré-fill IA actuellement (limite JVM backend — cf. doc helper)', () => {
      component.workspaceCountry = 'FRANCE';
      component.aiData = FULL_AI_DATA;
      component.forceExpanded = true;
      fixture.detectChanges();
      httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

      // Pré-fill IA = no-op tant que TravailExtractedData n'expose pas le
      // sous-objet ruptureAnticipeeCddDetail (SF de rattrapage à prévoir).
      expect(component.provenanceAuteur()).toBeNull();
      expect(component.provenanceMotif()).toBeNull();
      expect(component.provenanceDateTerme()).toBeNull();
    });

    it('Aucun pré-fill si BELGIQUE', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.aiData = FULL_AI_DATA;
      component.forceExpanded = true;
      fixture.detectChanges();
      // Pas de GET pour BE
      httpMock.expectNone(BASE_URL);

      expect(component.provenanceAuteur()).toBeNull();
      expect(component.provenanceMotif()).toBeNull();
    });

    it('Aucun pré-fill si standaloneMode', () => {
      component.workspaceCountry = 'FRANCE';
      component.standaloneMode = true;
      component.aiData = FULL_AI_DATA;
      component.forceExpanded = true;
      fixture.detectChanges();
      httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

      expect(component.provenanceAuteur()).toBeNull();
    });
  });

  describe('getPrefillCount (parité statique)', () => {
    it('retourne 0 actuellement (pas de pré-fill IA — limite JVM backend)', () => {
      expect(
        RuptureAnticipeeCddSectionComponent.getPrefillCount({
          aiData: FULL_AI_DATA,
          workspaceCountry: 'FRANCE',
        }),
      ).toBe(0);
    });

    it('retourne 0 hors France', () => {
      expect(
        RuptureAnticipeeCddSectionComponent.getPrefillCount({
          aiData: FULL_AI_DATA,
          workspaceCountry: 'BELGIQUE',
        }),
      ).toBe(0);
    });
  });

  describe('Form validation', () => {
    beforeEach(() => {
      component.workspaceCountry = 'FRANCE';
      component.forceExpanded = true;
      fixture.detectChanges();
      httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    });

    it('formValid false si dates manquantes', () => {
      expect(component.formValid()).toBe(false);
    });

    it('formValid true avec dates + salaires renseignés', () => {
      component.dateTermeCdd.set('2026-07-01');
      component.dateRupture.set('2026-01-01');
      component.salaireMensuelBrutEuros.set(2500);
      component.remunerationBruteTotaleContratEuros.set(10000);
      expect(component.formValid()).toBe(true);
    });

    it('formValid false si BELGIQUE', () => {
      component.workspaceCountry = 'BELGIQUE';
      component.dateTermeCdd.set('2026-07-01');
      component.dateRupture.set('2026-01-01');
      expect(component.formValid()).toBe(false);
    });
  });

  describe('Calcul POST', () => {
    beforeEach(() => {
      component.workspaceCountry = 'FRANCE';
      component.forceExpanded = true;
      fixture.detectChanges();
      httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
      component.dateTermeCdd.set('2026-07-01');
      component.dateRupture.set('2026-01-01');
      component.salaireMensuelBrutEuros.set(2500);
      component.remunerationBruteTotaleContratEuros.set(10000);
    });

    it('POST + snackbar OK + refresh dashboard', () => {
      component.calculate();
      const req = httpMock.expectOne(BASE_URL);
      expect(req.request.method).toBe('POST');
      req.flush(response('ILLEGITIME_EMPLOYEUR'));

      expect(component.result()?.analyseRuptureAnticipee).toBe('ILLEGITIME_EMPLOYEUR');
      expect(snackSpy.open).toHaveBeenCalled();
      expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
    });

    it('Error → snackbar + reste sur le formulaire', () => {
      component.calculate();
      const req = httpMock.expectOne(BASE_URL);
      req.flush({ message: 'Erreur' }, { status: 500, statusText: 'Server Error' });

      expect(component.result()).toBeNull();
      expect(snackSpy.open).toHaveBeenCalled();
    });

    it('Pas de refresh si standaloneMode', () => {
      component.standaloneMode = true;
      component.calculate();
      httpMock.expectOne(BASE_URL).flush(response('LEGITIME'));
      expect(refreshSpy.triggerRefresh).not.toHaveBeenCalled();
    });
  });

  describe('Helpers verdict', () => {
    it('verdictBannerClass — 3 niveaux distincts', () => {
      expect(component.verdictBannerClass('LEGITIME')).toContain('legitime');
      expect(component.verdictBannerClass('ILLEGITIME_EMPLOYEUR')).toContain('employeur');
      expect(component.verdictBannerClass('ILLEGITIME_SALARIE')).toContain('salarie');
    });

    it('verdictBannerLabel — labels distincts', () => {
      expect(component.verdictBannerLabel('LEGITIME')).toContain('légitime');
      expect(component.verdictBannerLabel('ILLEGITIME_EMPLOYEUR')).toContain('employeur');
      expect(component.verdictBannerLabel('ILLEGITIME_SALARIE')).toContain('salarié');
    });

    it('verdictBannerIcon — icônes distinctes', () => {
      expect(component.verdictBannerIcon('LEGITIME')).toBe('check_circle');
      expect(component.verdictBannerIcon('ILLEGITIME_EMPLOYEUR')).toBe('error_outline');
      expect(component.verdictBannerIcon('ILLEGITIME_SALARIE')).toBe('warning_amber');
    });
  });

  describe('Handlers de changement', () => {
    beforeEach(() => {
      component.workspaceCountry = 'FRANCE';
      component.forceExpanded = true;
      fixture.detectChanges();
      httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });
    });

    it('onAuteurChange efface le badge IA', () => {
      component.provenanceAuteur.set('IA');
      component.onAuteurChange('SALARIE');
      expect(component.auteurRupture()).toBe('SALARIE');
      expect(component.provenanceAuteur()).toBeNull();
    });

    it('onMotifChange efface le badge IA', () => {
      component.provenanceMotif.set('IA');
      component.onMotifChange('FORCE_MAJEURE');
      expect(component.motifRupture()).toBe('FORCE_MAJEURE');
      expect(component.provenanceMotif()).toBeNull();
    });

    it('onSalaireMensuelChange normalise les valeurs invalides', () => {
      component.onSalaireMensuelChange('not a number');
      expect(component.salaireMensuelBrutEuros()).toBe(0);
      component.onSalaireMensuelChange('-100');
      expect(component.salaireMensuelBrutEuros()).toBe(0);
      component.onSalaireMensuelChange('2500.5');
      expect(component.salaireMensuelBrutEuros()).toBe(2500.5);
    });
  });
});
