import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ElectionsCseConformiteSectionComponent } from './elections-cse-conformite-section.component';
import {
  ElectionsCseAnalyse,
  ElectionsCseConformiteResponse,
} from '../../core/models/elections-cse-conformite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ElectionsCseConformiteSectionComponent', () => {
  let component: ElectionsCseConformiteSectionComponent;
  let fixture: ComponentFixture<ElectionsCseConformiteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/elections-cse-conformite';

  /** Fixture IA complète — les 4 champs `electionCse*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    electionCseDateElection: '2026-05-15',
    electionCsePapNegocie: true,
    electionCseCollegesConformes: true,
    electionCseResultatsContestes: false,
  };

  function response(
    verdict: ElectionsCseAnalyse,
    alerteDelai = false,
  ): ElectionsCseConformiteResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire).
      effectifEntreprise: 25,
      papNegocieAvecOS: verdict !== 'IRREGULARITE_MAJEURE',
      delaiInvitationOSRespectee: true,
      collegesConformes: verdict !== 'IRREGULARITE_MAJEURE',
      resultatsContestes: false,
      dateElection: '2026-05-15',
      motifContestation: null,
      // Sorties calculées.
      analyseElectionsCse: verdict,
      scoreConformite: verdict === 'CONFORME' ? 100
                       : verdict === 'IRREGULARITE_MINEURE' ? 85
                       : 35,
      pointsIrregularite: verdict === 'CONFORME' ? [] : [
        {
          code: 'DT65_PAP_NEGOCIE',
          libelle: 'PAP absent',
          fondement: 'L. 2314-6 CT',
        },
      ],
      delaiContestationJours: 15,
      dateLimitContestationSiConnue: '2026-05-30',
      alerteDelaiContestation: alerteDelai,
      basesJuridiques: ['L. 2314-1 CT', 'Ordonnance n°2017-1386 du 22/09/2017'],
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
        ElectionsCseConformiteSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ElectionsCseConformiteSectionComponent);
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
    const banner = fixture.nativeElement.querySelector('[data-testid="cse-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 4 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.papNegocieAvecOS()).toBe(true);
    expect(component.collegesConformes()).toBe(true);
    expect(component.resultatsContestes()).toBe(false);
    expect(component.dateElection()).toBeTruthy();
    expect(component.provenanceDateElection()).toBe('IA');
    expect(component.provenancePapNegocie()).toBe('IA');
    expect(component.provenanceColleges()).toBe('IA');
    expect(component.provenanceResultatsContestes()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.papNegocieAvecOS()).toBe(false);
    expect(component.dateElection()).toBeNull();
    expect(component.provenanceDateElection()).toBeNull();
  });

  it('getPrefillCount = 4 sur fixture IA FR complète', () => {
    expect(ElectionsCseConformiteSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(ElectionsCseConformiteSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(ElectionsCseConformiteSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — verdict 3 niveaux + alerte délai 15 jours
  // ---------------------------------------------------------------------------

  it('POST → verdict CONFORME + refresh dashboard', () => {
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.effectifEntreprise).toBe(11);
    req.flush(response('CONFORME'));

    expect(component.result()?.analyseElectionsCse).toBe('CONFORME');
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('POST → verdict IRREGULARITE_MINEURE', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('IRREGULARITE_MINEURE'));
    expect(component.result()?.analyseElectionsCse).toBe('IRREGULARITE_MINEURE');
  });

  it('POST → verdict IRREGULARITE_MAJEURE + points d\'irrégularité', () => {
    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('IRREGULARITE_MAJEURE'));
    expect(component.result()?.analyseElectionsCse).toBe('IRREGULARITE_MAJEURE');
    expect(component.result()?.pointsIrregularite.length).toBeGreaterThan(0);
  });

  it('Alerte délai 15 j affichée si alerteDelaiContestation=true', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('IRREGULARITE_MAJEURE', true));
    fixture.detectChanges();

    const alerte = fixture.nativeElement.querySelector('[data-testid="cse-alerte-delai"]');
    expect(alerte).toBeTruthy();
    expect(alerte.textContent).toContain('15 jours');
  });

  it('Délai contestation 15 j affiché dans le bloc figures', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response('CONFORME'));
    fixture.detectChanges();

    const delaiBlock = fixture.nativeElement.querySelector('[data-testid="cse-figure-delai"]');
    expect(delaiBlock).toBeTruthy();
    expect(delaiBlock.textContent).toContain('15 jours');
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si effectif < 1', () => {
    component.effectifEntreprise.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true sur baseline', () => {
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers — effacement du badge IA sur modif manuelle
  // ---------------------------------------------------------------------------

  it('onPapChange efface le badge IA', () => {
    component.provenancePapNegocie.set('IA');
    component.onPapChange(true);
    expect(component.provenancePapNegocie()).toBeNull();
  });

  it('onCollegesChange efface le badge IA', () => {
    component.provenanceColleges.set('IA');
    component.onCollegesChange(true);
    expect(component.provenanceColleges()).toBeNull();
  });

  it('onResultatsContestesChange efface le badge IA', () => {
    component.provenanceResultatsContestes.set('IA');
    component.onResultatsContestesChange(true);
    expect(component.provenanceResultatsContestes()).toBeNull();
  });

  it('onDateElectionChange efface le badge IA', () => {
    component.provenanceDateElection.set('IA');
    component.onDateElectionChange(new Date('2026-05-15'));
    expect(component.provenanceDateElection()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('verdictBannerClass mappe les 3 niveaux', () => {
    expect(component.verdictBannerClass('CONFORME')).toContain('--conforme');
    expect(component.verdictBannerClass('IRREGULARITE_MINEURE')).toContain('--mineure');
    expect(component.verdictBannerClass('IRREGULARITE_MAJEURE')).toContain('--majeure');
  });

  it('verdictBannerLabel mappe les 3 niveaux', () => {
    expect(component.verdictBannerLabel('CONFORME')).toBe('Procédure conforme');
    expect(component.verdictBannerLabel('IRREGULARITE_MINEURE')).toBe('Irrégularité mineure');
    expect(component.verdictBannerLabel('IRREGULARITE_MAJEURE')).toBe('Irrégularité majeure');
  });

  it('formatDelaiContestation contient "15 jours"', () => {
    expect(component.formatDelaiContestation()).toContain('15 jours');
  });
});
