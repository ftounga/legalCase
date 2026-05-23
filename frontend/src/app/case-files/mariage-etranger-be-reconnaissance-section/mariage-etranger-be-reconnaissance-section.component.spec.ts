import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MariageEtrangerBeReconnaissanceSectionComponent } from './mariage-etranger-be-reconnaissance-section.component';
import { MariageEtrangerBeReconnaissanceResponse } from '../../core/models/mariage-etranger-be-reconnaissance.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';

describe('MariageEtrangerBeReconnaissanceSectionComponent — SF-217-17', () => {
  let component: MariageEtrangerBeReconnaissanceSectionComponent;
  let fixture: ComponentFixture<MariageEtrangerBeReconnaissanceSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/mariage-etranger-be-reconnaissance';

  function response(
    verdict: MariageEtrangerBeReconnaissanceResponse['verdict'],
  ): MariageEtrangerBeReconnaissanceResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse.
      natureActe: 'MARIAGE_CIVIL_ETRANGER',
      paysOrigine: 'MA',
      dateActe: '2018-06-12',
      residenceHabituelleAuMoinsUnePartie: 'BELGIQUE',
      nationaliteAuMoinsUnePartie: 'HORS_UE',
      conformiteDroitFondPersonnel: true,
      conformiteFormeLocusRegitActum: true,
      consentementEpouse: null,
      epousePresente: null,
      procedureContradictoire: null,
      decisionEcriteOfficielle: null,
      conventionBilateraleApplicable: false,
      commentaire: null,
      // Champs calculés.
      verdict,
      motifsRefus: verdict === 'RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC' ? [
        {
          code: 'POLYGAMIE_ORDRE_PUBLIC',
          libelle: 'Mariage polygame',
          fondement: 'CDIP art. 21 (à vérifier)',
          severite: 'HIGH',
        },
      ] : [],
      motifsReserve: [],
      actesAProduire: ['Demande de transcription auprès de l\'officier de l\'état civil.'],
      basesJuridiques: ['CDIP art. 27 (à vérifier)'],
      messages: ['Mariage civil étranger conforme au fond et à la forme.'],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        MariageEtrangerBeReconnaissanceSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MariageEtrangerBeReconnaissanceSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Chargement (GET)
  // ---------------------------------------------------------------------------

  it('mount BELGIQUE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it("mount FRANCE → pas d'appel HTTP", () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('GET 200 → result rechargé + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('RECONNAISSANCE_DE_PLEIN_DROIT'));
    expect(component.result()!.verdict).toBe('RECONNAISSANCE_DE_PLEIN_DROIT');
    expect(component.showForm()).toBe(false);
  });

  it("GET 200 → formulaire ré-hydraté depuis le snapshot d'inputs", () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('RECONNAISSANCE_DE_PLEIN_DROIT'));
    expect(component.natureActe()).toBe('MARIAGE_CIVIL_ETRANGER');
    expect(component.paysOrigine()).toBe('MA');
    expect(component.dateActe()).toBe('2018-06-12');
    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.paysOrigine()).toBe('MA');
  });

  // ---------------------------------------------------------------------------
  // Pays d'origine — normalisation ISO-2
  // ---------------------------------------------------------------------------

  it("onPaysOrigineChange normalise la valeur (majuscule + 2 caractères max)", () => {
    component.onPaysOrigineChange('ma');
    expect(component.paysOrigine()).toBe('MA');
    component.onPaysOrigineChange('FRA');
    expect(component.paysOrigine()).toBe('FR');
    component.onPaysOrigineChange('  dz  ');
    expect(component.paysOrigine()).toBe('DZ');
  });

  // ---------------------------------------------------------------------------
  // Bloc talaq conditionnel
  // ---------------------------------------------------------------------------

  it("bloc talaq isTalaq() est faux par défaut", () => {
    expect(component.isTalaq()).toBe(false);
  });

  it("bloc talaq isTalaq() est vrai si natureActe = TALAQ_REPUDIATION", () => {
    component.natureActe.set('TALAQ_REPUDIATION');
    expect(component.isTalaq()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire
  // ---------------------------------------------------------------------------

  it('formValid faux sans champ requis', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.natureActe.set('MARIAGE_CIVIL_ETRANGER');
    component.paysOrigine.set('MA');
    component.dateActe.set('2018-06-12');
    component.residenceHabituelleAuMoinsUnePartie.set('BELGIQUE');
    component.nationaliteAuMoinsUnePartie.set('HORS_UE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si tous les champs requis sont remplis', () => {
    component.natureActe.set('MARIAGE_CIVIL_ETRANGER');
    component.paysOrigine.set('MA');
    component.dateActe.set('2018-06-12');
    component.residenceHabituelleAuMoinsUnePartie.set('BELGIQUE');
    component.nationaliteAuMoinsUnePartie.set('HORS_UE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si paysOrigine non ISO-2 (3 lettres)', () => {
    component.natureActe.set('MARIAGE_CIVIL_ETRANGER');
    component.paysOrigine.set('FRA');
    component.dateActe.set('2018-06-12');
    component.residenceHabituelleAuMoinsUnePartie.set('BELGIQUE');
    component.nationaliteAuMoinsUnePartie.set('HORS_UE');
    // Le composant normalise via onPaysOrigineChange, mais une affectation
    // directe simule un cas extrême : doit être rejeté par formValid.
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.natureActe.set('MARIAGE_CIVIL_ETRANGER');
    component.paysOrigine.set('MA');
    component.dateActe.set('2018-06-12');
    component.residenceHabituelleAuMoinsUnePartie.set('BELGIQUE');
    component.nationaliteAuMoinsUnePartie.set('HORS_UE');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.natureActe).toBe('MARIAGE_CIVIL_ETRANGER');
    expect(req.request.body.paysOrigine).toBe('MA');
    expect(req.request.body.dateActe).toBe('2018-06-12');
    expect(req.request.body.consentementEpouse).toBeNull(); // pas TALAQ
    req.flush(response('RECONNAISSANCE_DE_PLEIN_DROIT'));

    expect(component.result()!.verdict).toBe('RECONNAISSANCE_DE_PLEIN_DROIT');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — booleans talaq transmis si natureActe = TALAQ_REPUDIATION', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.natureActe.set('TALAQ_REPUDIATION');
    component.paysOrigine.set('MA');
    component.dateActe.set('2024-08-15');
    component.residenceHabituelleAuMoinsUnePartie.set('BELGIQUE');
    component.nationaliteAuMoinsUnePartie.set('HORS_UE');
    component.consentementEpouse.set(true);
    component.epousePresente.set(true);
    component.procedureContradictoire.set(false);
    component.decisionEcriteOfficielle.set(true);
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.consentementEpouse).toBe(true);
    expect(req.request.body.epousePresente).toBe(true);
    expect(req.request.body.procedureContradictoire).toBe(false);
    expect(req.request.body.decisionEcriteOfficielle).toBe(true);
    req.flush(response('RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.natureActe.set('MARIAGE_CIVIL_ETRANGER');
    component.paysOrigine.set('MA');
    component.dateActe.set('2018-06-12');
    component.residenceHabituelleAuMoinsUnePartie.set('BELGIQUE');
    component.nationaliteAuMoinsUnePartie.set('HORS_UE');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(component.showForm()).toBe(true);
  });

  it("calculate() no-op si formulaire invalide (pas d'appel POST)", () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Verdicts — rouge réservé à RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC
  // ---------------------------------------------------------------------------

  it('verdict RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC → banner rouge --danger', () => {
    expect(component.verdictBannerClass('RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC')).toContain('--danger');
    expect(component.verdictBannerIcon('RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC')).toBe('error');
  });

  it('verdict RECONNAISSANCE_DE_PLEIN_DROIT → banner vert --ok (pas rouge)', () => {
    expect(component.verdictBannerClass('RECONNAISSANCE_DE_PLEIN_DROIT')).toContain('--ok');
    expect(component.verdictBannerClass('RECONNAISSANCE_DE_PLEIN_DROIT')).not.toContain('--danger');
  });

  it('verdict RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS → banner or --medium (pas rouge)', () => {
    expect(component.verdictBannerClass('RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS')).toContain('--medium');
    expect(component.verdictBannerClass('RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS')).not.toContain('--danger');
  });

  it('verdict EXEQUATUR_REQUIS → banner or --medium (pas rouge)', () => {
    expect(component.verdictBannerClass('EXEQUATUR_REQUIS')).toContain('--medium');
    expect(component.verdictBannerClass('EXEQUATUR_REQUIS')).not.toContain('--danger');
  });

  it('verdict QUALIFICATION_INCOMPLETE → banner navy --info', () => {
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).not.toContain('--danger');
  });

  // ---------------------------------------------------------------------------
  // Bannière FRANCE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="mer-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('droit belge');
  });

  // ---------------------------------------------------------------------------
  // PREFILL_COUNT_ALWAYS_ZERO = true
  // ---------------------------------------------------------------------------

  it('PREFILL_COUNT_ALWAYS_ZERO === true', () => {
    expect(MariageEtrangerBeReconnaissanceSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('getPrefillCount() retourne 0 sur input vide', () => {
    expect(MariageEtrangerBeReconnaissanceSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() retourne 0 même avec aiData rempli (V1)', () => {
    expect(
      MariageEtrangerBeReconnaissanceSectionComponent.getPrefillCount({
        aiData: { dateMariageBeDetectee: '2015-06-20' },
      }),
    ).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Statics TOOL_LABEL / TOOL_ICON exposés
  // ---------------------------------------------------------------------------

  it('TOOL_LABEL et TOOL_ICON exposés', () => {
    expect(MariageEtrangerBeReconnaissanceSectionComponent.TOOL_LABEL).toContain('MARIAGE');
    expect(MariageEtrangerBeReconnaissanceSectionComponent.TOOL_ICON).toBe('public');
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts F-IA-03 — date de l'acte
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.DATE_ACTE présent si F-96 diverge', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date de l\'acte étranger',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_ACTE_ETRANGER', expectedValue: '2018-07-15',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateActe.set('2018-06-12');
    const alert = component.coherenceAlerts().DATE_ACTE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
  });

  it('coherenceAlerts vide en standaloneMode', () => {
    component.standaloneMode = true;
    component.ngOnInit();
    const reqs = httpMock.match(BASE_URL);
    reqs.forEach(r => r.flush({}, { status: 404, statusText: 'Not Found' }));
    component.dateActe.set('2018-06-12');
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    const checks: ProcedureCheck[] = [{
      id: 'c1', ordre: 1, description: 'Date de l\'acte étranger',
      statut: 'NON_COMPLIANT', critereCode: 'F217_DATE_ACTE_ETRANGER', expectedValue: '2018-07-15',
    }];
    component.procedureChecks = checks;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.dateActe.set('2018-06-12');
    expect(component.coherenceAlerts().DATE_ACTE).toBeDefined();
    component.showForm.set(false);
    expect(component.coherenceAlerts().DATE_ACTE).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // Toggle / editMode
  // ---------------------------------------------------------------------------

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode ré-affiche le form après calcul', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });
});
