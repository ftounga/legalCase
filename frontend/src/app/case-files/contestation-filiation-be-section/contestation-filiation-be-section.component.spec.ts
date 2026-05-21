import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ContestationFiliationBeSectionComponent } from './contestation-filiation-be-section.component';
import { ContestationFiliationBeResponse } from '../../core/models/contestation-filiation-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('ContestationFiliationBeSectionComponent — SF-217-19', () => {
  let component: ContestationFiliationBeSectionComponent;
  let fixture: ComponentFixture<ContestationFiliationBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/contestation-filiation-be';

  function response(
    verdict: ContestationFiliationBeResponse['verdict'],
  ): ContestationFiliationBeResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse.
      natureActionFiliation: 'PATERNITE_PRESUMEE_MARI',
      qualiteDemandeur: 'PERE_PRESUME_OU_RECONNAISSANT',
      dateNaissanceEnfant: '2023-01-15',
      dateConnaissanceFaitContestation: '2026-01-15',
      possessionEtatConforme: false,
      dureePossessionEtatAnnees: 0,
      expertiseAdnDisponible: true,
      demandeExpertiseAdnEnvisagee: false,
      commentaire: null,
      // Champs calculés.
      verdict,
      dateLimiteAction:
        verdict === 'IRRECEVABLE_POSSESSION_ETAT' || verdict === 'IRRECEVABLE_QUALITE_A_AGIR'
          ? null : '2027-01-15',
      joursRestants:
        verdict === 'IRRECEVABLE_POSSESSION_ETAT' || verdict === 'IRRECEVABLE_QUALITE_A_AGIR'
          ? null
          : verdict === 'ACTION_RECEVABLE_DELAI_CRITIQUE'
            ? 15
            : verdict === 'IRRECEVABLE_DELAI_DEPASSE'
              ? -10
              : 200,
      delaiStatut:
        verdict === 'IRRECEVABLE_POSSESSION_ETAT' || verdict === 'IRRECEVABLE_QUALITE_A_AGIR'
          ? null
          : verdict === 'ACTION_RECEVABLE_DELAI_CRITIQUE'
            ? 'CRITIQUE'
            : verdict === 'IRRECEVABLE_DELAI_DEPASSE'
              ? 'DEPASSE'
              : 'OK',
      voieProcedurale: verdict === 'ACTION_RECEVABLE' || verdict === 'ACTION_RECEVABLE_DELAI_CRITIQUE'
        ? 'REQUETE_TRIBUNAL_FAMILLE'
        : 'AUCUNE_ACTION_RECEVABLE',
      motifsIrrecevabilite: verdict === 'IRRECEVABLE_POSSESSION_ETAT' ? [{
        code: 'POSSESSION_ETAT_CONFORME_5_ANS',
        libelle: 'Possession d\'état conforme depuis 6 ans',
        fondement: 'CC art. 318 § 2 — à vérifier',
      }] : verdict === 'IRRECEVABLE_DELAI_DEPASSE' ? [{
        code: 'DELAI_FORCLUSION',
        libelle: 'Délai d\'action dépassé',
        fondement: 'CC art. 318 — délai 1 an',
      }] : [],
      actionsConcretes: ['Saisir le Tribunal de la famille'],
      basesJuridiques: ['CC art. 318 nouveau (à vérifier)'],
      messages: ['Délai d\'action 1 an à compter de la connaissance.'],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-21T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        ContestationFiliationBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ContestationFiliationBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Statics
  // ---------------------------------------------------------------------------

  it('expose TOOL_LABEL et TOOL_ICON', () => {
    expect(ContestationFiliationBeSectionComponent.TOOL_LABEL).toContain('CONTESTATION DE FILIATION');
    expect(ContestationFiliationBeSectionComponent.TOOL_ICON).toBe('family_restroom');
  });

  it('PREFILL_COUNT_ALWAYS_ZERO === true', () => {
    expect(ContestationFiliationBeSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('getPrefillCount() retourne 0 sur input vide', () => {
    expect(ContestationFiliationBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() retourne 0 même avec aiData rempli (V1)', () => {
    expect(
      ContestationFiliationBeSectionComponent.getPrefillCount({
        aiData: { dateMariageBeDetectee: '2015-06-20' },
      }),
    ).toBe(0);
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
    httpMock.expectOne(BASE_URL).flush(response('ACTION_RECEVABLE'));
    expect(component.result()!.verdict).toBe('ACTION_RECEVABLE');
    expect(component.showForm()).toBe(false);
  });

  it("GET 200 → formulaire ré-hydraté depuis le snapshot d'inputs", () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('ACTION_RECEVABLE'));
    expect(component.natureActionFiliation()).toBe('PATERNITE_PRESUMEE_MARI');
    expect(component.qualiteDemandeur()).toBe('PERE_PRESUME_OU_RECONNAISSANT');
    expect(component.dateNaissanceEnfant()).toBe('2023-01-15');
    expect(component.dateConnaissanceFaitContestation()).toBe('2026-01-15');
    expect(component.expertiseAdnDisponible()).toBe(true);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Possession d'état conditionnelle
  // ---------------------------------------------------------------------------

  it('showDureePossession est false par défaut', () => {
    expect(component.showDureePossession()).toBe(false);
  });

  it('showDureePossession bascule à true quand possessionEtatConforme=true', () => {
    component.onPossessionEtatChange(true);
    expect(component.showDureePossession()).toBe(true);
  });

  it("onPossessionEtatChange(false) remet la durée à 0", () => {
    component.possessionEtatConforme.set(true);
    component.dureePossessionEtatAnnees.set(6);
    component.onPossessionEtatChange(false);
    expect(component.possessionEtatConforme()).toBe(false);
    expect(component.dureePossessionEtatAnnees()).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire
  // ---------------------------------------------------------------------------

  it('formValid faux sans champ requis', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.natureActionFiliation.set('PATERNITE_PRESUMEE_MARI');
    component.qualiteDemandeur.set('PERE_PRESUME_OU_RECONNAISSANT');
    component.dateNaissanceEnfant.set('2023-01-15');
    component.dateConnaissanceFaitContestation.set('2026-01-15');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si tous les champs requis sont remplis', () => {
    component.natureActionFiliation.set('PATERNITE_PRESUMEE_MARI');
    component.qualiteDemandeur.set('PERE_PRESUME_OU_RECONNAISSANT');
    component.dateNaissanceEnfant.set('2023-01-15');
    component.dateConnaissanceFaitContestation.set('2026-01-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si dateConnaissance < dateNaissance', () => {
    component.natureActionFiliation.set('PATERNITE_PRESUMEE_MARI');
    component.qualiteDemandeur.set('PERE_PRESUME_OU_RECONNAISSANT');
    component.dateNaissanceEnfant.set('2023-01-15');
    component.dateConnaissanceFaitContestation.set('2022-01-01');
    expect(component.formValid()).toBe(false);
  });

  it("formValid faux si possession d'état cochée avec durée > 100", () => {
    component.natureActionFiliation.set('PATERNITE_PRESUMEE_MARI');
    component.qualiteDemandeur.set('PERE_PRESUME_OU_RECONNAISSANT');
    component.dateNaissanceEnfant.set('2023-01-15');
    component.dateConnaissanceFaitContestation.set('2026-01-15');
    component.possessionEtatConforme.set(true);
    component.dureePossessionEtatAnnees.set(101);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.natureActionFiliation.set('PATERNITE_PRESUMEE_MARI');
    component.qualiteDemandeur.set('PERE_PRESUME_OU_RECONNAISSANT');
    component.dateNaissanceEnfant.set('2023-01-15');
    component.dateConnaissanceFaitContestation.set('2026-01-15');
    component.expertiseAdnDisponible.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.natureActionFiliation).toBe('PATERNITE_PRESUMEE_MARI');
    expect(req.request.body.qualiteDemandeur).toBe('PERE_PRESUME_OU_RECONNAISSANT');
    expect(req.request.body.dateNaissanceEnfant).toBe('2023-01-15');
    expect(req.request.body.dateConnaissanceFaitContestation).toBe('2026-01-15');
    expect(req.request.body.possessionEtatConforme).toBe(false);
    expect(req.request.body.dureePossessionEtatAnnees).toBe(0);
    expect(req.request.body.expertiseAdnDisponible).toBe(true);
    req.flush(response('ACTION_RECEVABLE'));

    expect(component.result()!.verdict).toBe('ACTION_RECEVABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — durée de possession transmise uniquement si possession cochée', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.natureActionFiliation.set('PATERNITE_PRESUMEE_MARI');
    component.qualiteDemandeur.set('PERE_PRESUME_OU_RECONNAISSANT');
    component.dateNaissanceEnfant.set('2023-01-15');
    component.dateConnaissanceFaitContestation.set('2026-01-15');
    component.possessionEtatConforme.set(true);
    component.dureePossessionEtatAnnees.set(6);
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.possessionEtatConforme).toBe(true);
    expect(req.request.body.dureePossessionEtatAnnees).toBe(6);
    req.flush(response('IRRECEVABLE_POSSESSION_ETAT'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.natureActionFiliation.set('PATERNITE_PRESUMEE_MARI');
    component.qualiteDemandeur.set('PERE_PRESUME_OU_RECONNAISSANT');
    component.dateNaissanceEnfant.set('2023-01-15');
    component.dateConnaissanceFaitContestation.set('2026-01-15');
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
  // Verdicts — rouge réservé aux IRRECEVABLE_*
  // ---------------------------------------------------------------------------

  it('verdict ACTION_RECEVABLE → banner vert --ok (pas rouge)', () => {
    expect(component.verdictBannerClass('ACTION_RECEVABLE')).toContain('--ok');
    expect(component.verdictBannerClass('ACTION_RECEVABLE')).not.toContain('--danger');
  });

  it('verdict ACTION_RECEVABLE_DELAI_CRITIQUE → banner or --medium', () => {
    expect(component.verdictBannerClass('ACTION_RECEVABLE_DELAI_CRITIQUE')).toContain('--medium');
    expect(component.verdictBannerClass('ACTION_RECEVABLE_DELAI_CRITIQUE')).not.toContain('--danger');
  });

  it('verdict IRRECEVABLE_DELAI_DEPASSE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('IRRECEVABLE_DELAI_DEPASSE')).toContain('--danger');
    expect(component.verdictBannerIcon('IRRECEVABLE_DELAI_DEPASSE')).toBe('error');
  });

  it('verdict IRRECEVABLE_QUALITE_A_AGIR → banner rouge --danger', () => {
    expect(component.verdictBannerClass('IRRECEVABLE_QUALITE_A_AGIR')).toContain('--danger');
  });

  it('verdict IRRECEVABLE_POSSESSION_ETAT → banner rouge --danger', () => {
    expect(component.verdictBannerClass('IRRECEVABLE_POSSESSION_ETAT')).toContain('--danger');
  });

  it('verdict QUALIFICATION_INCOMPLETE → banner navy --info', () => {
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).not.toContain('--danger');
  });

  // ---------------------------------------------------------------------------
  // Statut du délai — rouge pour CRITIQUE / DEPASSE
  // ---------------------------------------------------------------------------

  it('delaiStatut OK → classe --ok', () => {
    expect(component.delaiStatutClass('OK')).toContain('--ok');
  });

  it('delaiStatut CRITIQUE → classe --danger', () => {
    expect(component.delaiStatutClass('CRITIQUE')).toContain('--danger');
  });

  it('delaiStatut DEPASSE → classe --danger', () => {
    expect(component.delaiStatutClass('DEPASSE')).toContain('--danger');
  });

  it('delaiStatut null → libellé tiret', () => {
    expect(component.delaiStatutLabel(null)).toBe('—');
  });

  // ---------------------------------------------------------------------------
  // Bannière FRANCE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="cfb-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('droit belge');
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

  // ---------------------------------------------------------------------------
  // Maternité hors scope V1 → backend renvoie 400
  // ---------------------------------------------------------------------------

  it('options nature ne contiennent pas MATERNITE (hors scope V1)', () => {
    expect(component.natureOptions.find(o => o.value === 'MATERNITE')).toBeUndefined();
  });
});
