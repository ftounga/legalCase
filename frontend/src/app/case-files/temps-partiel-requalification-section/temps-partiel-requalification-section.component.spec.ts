import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TempsPartielRequalificationSectionComponent } from './temps-partiel-requalification-section.component';
import {
  TempsPartielAnalyseRequalification,
  TempsPartielRequalificationResponse,
} from '../../core/models/temps-partiel-requalification.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('TempsPartielRequalificationSectionComponent', () => {
  let component: TempsPartielRequalificationSectionComponent;
  let fixture: ComponentFixture<TempsPartielRequalificationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/temps-partiel-requalification';

  /** Fixture IA complète — les 4 champs `tempsPartiel*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    tempsPartielDureeContractuelle: 24,
    tempsPartielMentionsDuree: false,
    tempsPartielMentionsRepartition: true,
    tempsPartielHCMoyenne: 6,
  };

  function response(
    verdict: TempsPartielAnalyseRequalification,
    rappelEuros: number | null = null,
  ): TempsPartielRequalificationResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire).
      dureeContractuelleHeures: 24,
      mentionsDureePresentes: false,
      mentionsRepartitionPresentes: true,
      mentionsHCPresentes: true,
      heuresComplementairesReelleMoyenneHeures: 6,
      modificationRepartitionUnilaterale: false,
      ancienneteMois: 36,
      salaireMensuelContractuelEuros: 2000,
      // Sorties calculées.
      analyseRequalification: verdict,
      scoreRequalification: verdict === 'REQUALIFICATION_PROBABLE' ? 70
                            : verdict === 'REQUALIFICATION_POSSIBLE' ? 30
                            : 5,
      facteursRequalification: [
        {
          code: 'DT49_MENTIONS_DUREE',
          libelle: 'Mention durée absente',
          fondement: 'L. 3123-6 CT',
          poids: 35,
        },
      ],
      rappelSalaire3AnsEstimeEuros: rappelEuros,
      basesJuridiques: ['L. 3123-6 CT', 'L. 3245-1 CT'],
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
        TempsPartielRequalificationSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TempsPartielRequalificationSectionComponent);
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
    const banner = fixture.nativeElement.querySelector('[data-testid="tpr-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 4 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.dureeContractuelleHeures()).toBe(24);
    expect(component.mentionsDureePresentes()).toBe(false);
    expect(component.mentionsRepartitionPresentes()).toBe(true);
    expect(component.heuresComplementairesReelleMoyenneHeures()).toBe(6);
    expect(component.provenanceDuree()).toBe('IA');
    expect(component.provenanceMentionsDuree()).toBe('IA');
    expect(component.provenanceMentionsRepartition()).toBe('IA');
    expect(component.provenanceHCMoyenne()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    // Valeurs par défaut conservées.
    expect(component.dureeContractuelleHeures()).toBe(24);
    expect(component.mentionsDureePresentes()).toBe(true);
    expect(component.provenanceDuree()).toBeNull();
  });

  it('getPrefillCount = 4 sur fixture IA FR complète', () => {
    expect(TempsPartielRequalificationSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(TempsPartielRequalificationSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(TempsPartielRequalificationSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + rappel salaire
  // ---------------------------------------------------------------------------

  it('POST → verdict REQUALIFICATION_PROBABLE + refresh dashboard', () => {
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dureeContractuelleHeures).toBe(24);
    req.flush(response('REQUALIFICATION_PROBABLE', 33000));

    expect(component.result()?.analyseRequalification).toBe('REQUALIFICATION_PROBABLE');
    expect(component.result()?.rappelSalaire3AnsEstimeEuros).toBe(33000);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict REQUALIFICATION_POSSIBLE', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('REQUALIFICATION_POSSIBLE', 12000));
    expect(component.result()?.analyseRequalification).toBe('REQUALIFICATION_POSSIBLE');
  });

  it('POST → verdict PAS_DE_REQUALIFICATION sans rappel salaire', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('PAS_DE_REQUALIFICATION', null));
    expect(component.result()?.analyseRequalification).toBe('PAS_DE_REQUALIFICATION');
    expect(component.result()?.rappelSalaire3AnsEstimeEuros).toBeNull();
  });

  it('Rappel salaire affiché si rappel > 0', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('REQUALIFICATION_PROBABLE', 33000));
    fixture.detectChanges();

    const block = fixture.nativeElement.querySelector('[data-testid="tpr-rappel-block"]');
    expect(block).toBeTruthy();
  });

  it('formatRappelSalaire = null si pas de rappel', () => {
    component.result.set(response('PAS_DE_REQUALIFICATION', null));
    expect(component.formatRappelSalaire()).toBeNull();
  });

  it('formatRappelSalaire formate en EUR', () => {
    component.result.set(response('REQUALIFICATION_PROBABLE', 33000));
    const v = component.formatRappelSalaire();
    expect(v).not.toBeNull();
    expect(v!).toContain('33');
    expect(v!).toContain('€');
  });

  // ---------------------------------------------------------------------------
  // formValid + bornes durée
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si durée >= 35h (incohérent avec temps partiel)', () => {
    component.dureeContractuelleHeures.set(35);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si durée <= 0', () => {
    component.dureeContractuelleHeures.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si HC < 0', () => {
    component.heuresComplementairesReelleMoyenneHeures.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si ancienneté < 0', () => {
    component.ancienneteMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true sur baseline', () => {
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers — effacement du badge IA sur modif manuelle
  // ---------------------------------------------------------------------------

  it('onDureeChange efface le badge IA', () => {
    component.provenanceDuree.set('IA');
    component.onDureeChange(28);
    expect(component.provenanceDuree()).toBeNull();
  });

  it('onMentionsDureeChange efface le badge IA', () => {
    component.provenanceMentionsDuree.set('IA');
    component.onMentionsDureeChange(true);
    expect(component.provenanceMentionsDuree()).toBeNull();
  });

  it('onHCMoyenneChange efface le badge IA', () => {
    component.provenanceHCMoyenne.set('IA');
    component.onHCMoyenneChange(8);
    expect(component.provenanceHCMoyenne()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('REQUALIFICATION_PROBABLE')).toContain('--probable');
    expect(component.verdictBannerClass('REQUALIFICATION_POSSIBLE')).toContain('--possible');
    expect(component.verdictBannerClass('PAS_DE_REQUALIFICATION')).toContain('--pas-de');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('REQUALIFICATION_PROBABLE')).toBe('Requalification probable');
    expect(component.verdictBannerLabel('REQUALIFICATION_POSSIBLE')).toBe('Requalification possible');
    expect(component.verdictBannerLabel('PAS_DE_REQUALIFICATION')).toBe('Pas de requalification');
  });
});
