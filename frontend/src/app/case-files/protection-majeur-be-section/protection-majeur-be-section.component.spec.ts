import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProtectionMajeurBeSectionComponent } from './protection-majeur-be-section.component';
import { ProtectionMajeurBeResponse } from '../../core/models/protection-majeur-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('ProtectionMajeurBeSectionComponent — SF-217-15', () => {
  let component: ProtectionMajeurBeSectionComponent;
  let fixture: ComponentFixture<ProtectionMajeurBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/protection-majeur-be';

  function response(
    verdict: ProtectionMajeurBeResponse['verdict'],
  ): ProtectionMajeurBeResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé.
      natureAlteration: 'MEDICALE_DURABLE',
      graviteIncapacite: 'LES_DEUX',
      mandatExtraJudiciaireSigne: false,
      mandatExtraJudiciaireDateSignature: null,
      declarationAnticipeeExiste: false,
      environnementFamilialProtecteur: true,
      niveauUrgence: 'URGENCE_PATRIMONIALE',
      modeSaisineEnvisage: 'REQUETE_JP',
      commentaire: null,
      // Champs calculés.
      verdict,
      mesureRecommandee: verdict === 'MANDAT_EXTRA_JUDICIAIRE_VALABLE'
        ? 'MANDAT_EXTRA_JUDICIAIRE'
        : verdict === 'URGENCE_MESURE_PROVISOIRE'
          ? 'MESURE_PROVISOIRE_URGENCE'
          : 'ADMINISTRATION_BIENS_ET_PERSONNE',
      juridictionCompetente: verdict === 'MANDAT_EXTRA_JUDICIAIRE_VALABLE'
        || verdict === 'QUALIFICATION_INCOMPLETE'
        ? null
        : 'JUSTICE_PAIX',
      fondementProcedural: 'Loi 17/03/2013 ; CJ art. 594-595 — Justice de paix',
      actesProteges: verdict === 'MESURE_PROTECTION_RECOMMANDEE' ? [
        {
          code: 'DISPOSITION_BIENS_IMMOBILIERS',
          libelle: 'Disposition d\'un bien immobilier',
          necessite: 'AUTORISATION_JP_PREALABLE',
          fondement: 'Loi 17/03/2013 — à vérifier',
        },
        {
          code: 'ACTES_QUOTIDIENS',
          libelle: 'Actes de la vie quotidienne',
          necessite: 'AUTONOMIE_PRESERVEE',
          fondement: 'Loi 17/03/2013 — principe de capacité résiduelle',
        },
      ] : [],
      actionsConcretes: ['Saisir la Justice de paix par requête'],
      basesJuridiques: ['Loi du 17/03/2013 (à vérifier)'],
      messages: ['Incapacité étendue aux biens et à la personne → administration des biens ET de la personne.'],
      country: 'BELGIQUE',
      calculatedAt: '2026-05-21T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        ProtectionMajeurBeSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProtectionMajeurBeSectionComponent);
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
    httpMock.expectOne(BASE_URL).flush(response('MESURE_PROTECTION_RECOMMANDEE'));
    expect(component.result()!.verdict).toBe('MESURE_PROTECTION_RECOMMANDEE');
    expect(component.showForm()).toBe(false);
  });

  it("GET 200 → formulaire ré-hydraté depuis le snapshot d'inputs", () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('MESURE_PROTECTION_RECOMMANDEE'));
    expect(component.natureAlteration()).toBe('MEDICALE_DURABLE');
    expect(component.graviteIncapacite()).toBe('LES_DEUX');
    expect(component.niveauUrgence()).toBe('URGENCE_PATRIMONIALE');
    expect(component.modeSaisineEnvisage()).toBe('REQUETE_JP');
    expect(component.environnementFamilialProtecteur()).toBe(true);
    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.natureAlteration()).toBe('MEDICALE_DURABLE');
  });

  // ---------------------------------------------------------------------------
  // Toggle mandat extra-judiciaire
  // ---------------------------------------------------------------------------

  it('onMandatChange(false) efface la date associée', () => {
    component.mandatExtraJudiciaireSigne.set(true);
    component.mandatExtraJudiciaireDateSignature.set('2020-01-15');
    component.onMandatChange(false);
    expect(component.mandatExtraJudiciaireSigne()).toBe(false);
    expect(component.mandatExtraJudiciaireDateSignature()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire
  // ---------------------------------------------------------------------------

  it('formValid faux sans champ requis', () => {
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.natureAlteration.set('MEDICALE_DURABLE');
    component.graviteIncapacite.set('LES_DEUX');
    component.niveauUrgence.set('URGENCE_PATRIMONIALE');
    component.modeSaisineEnvisage.set('REQUETE_JP');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si tous les champs requis sont remplis', () => {
    component.natureAlteration.set('MEDICALE_DURABLE');
    component.graviteIncapacite.set('LES_DEUX');
    component.niveauUrgence.set('URGENCE_PATRIMONIALE');
    component.modeSaisineEnvisage.set('REQUETE_JP');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si mandat activé sans date', () => {
    component.natureAlteration.set('MEDICALE_DURABLE');
    component.graviteIncapacite.set('LES_DEUX');
    component.niveauUrgence.set('URGENCE_PATRIMONIALE');
    component.modeSaisineEnvisage.set('REQUETE_JP');
    component.mandatExtraJudiciaireSigne.set(true);
    expect(component.formValid()).toBe(false);
    component.mandatExtraJudiciaireDateSignature.set('2020-01-15');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.natureAlteration.set('MEDICALE_DURABLE');
    component.graviteIncapacite.set('LES_DEUX');
    component.niveauUrgence.set('URGENCE_PATRIMONIALE');
    component.modeSaisineEnvisage.set('REQUETE_JP');
    component.environnementFamilialProtecteur.set(true);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.natureAlteration).toBe('MEDICALE_DURABLE');
    expect(req.request.body.graviteIncapacite).toBe('LES_DEUX');
    expect(req.request.body.niveauUrgence).toBe('URGENCE_PATRIMONIALE');
    expect(req.request.body.modeSaisineEnvisage).toBe('REQUETE_JP');
    expect(req.request.body.environnementFamilialProtecteur).toBe(true);
    expect(req.request.body.mandatExtraJudiciaireSigne).toBe(false);
    expect(req.request.body.mandatExtraJudiciaireDateSignature).toBeNull();
    req.flush(response('MESURE_PROTECTION_RECOMMANDEE'));

    expect(component.result()!.verdict).toBe('MESURE_PROTECTION_RECOMMANDEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — date de signature du mandat transmise si activé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.natureAlteration.set('MEDICALE_DURABLE');
    component.graviteIncapacite.set('LES_DEUX');
    component.niveauUrgence.set('AUCUNE_URGENCE');
    component.modeSaisineEnvisage.set('REQUETE_JP');
    component.mandatExtraJudiciaireSigne.set(true);
    component.mandatExtraJudiciaireDateSignature.set('2020-01-15');
    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.mandatExtraJudiciaireSigne).toBe(true);
    expect(req.request.body.mandatExtraJudiciaireDateSignature).toBe('2020-01-15');
    req.flush(response('MANDAT_EXTRA_JUDICIAIRE_VALABLE'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.natureAlteration.set('MEDICALE_DURABLE');
    component.graviteIncapacite.set('LES_DEUX');
    component.niveauUrgence.set('URGENCE_PATRIMONIALE');
    component.modeSaisineEnvisage.set('REQUETE_JP');
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
  // Verdicts — rouge réservé à URGENCE_MESURE_PROVISOIRE
  // ---------------------------------------------------------------------------

  it('verdict URGENCE_MESURE_PROVISOIRE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('URGENCE_MESURE_PROVISOIRE')).toContain('--danger');
    expect(component.verdictBannerIcon('URGENCE_MESURE_PROVISOIRE')).toBe('error');
  });

  it('verdict MANDAT_EXTRA_JUDICIAIRE_VALABLE → banner vert --ok (pas rouge)', () => {
    expect(component.verdictBannerClass('MANDAT_EXTRA_JUDICIAIRE_VALABLE')).toContain('--ok');
    expect(component.verdictBannerClass('MANDAT_EXTRA_JUDICIAIRE_VALABLE')).not.toContain('--danger');
  });

  it('verdict MESURE_PROTECTION_RECOMMANDEE → banner or --medium', () => {
    expect(component.verdictBannerClass('MESURE_PROTECTION_RECOMMANDEE')).toContain('--medium');
    expect(component.verdictBannerClass('MESURE_PROTECTION_RECOMMANDEE')).not.toContain('--danger');
  });

  it('verdict QUALIFICATION_INCOMPLETE → banner navy --info', () => {
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).not.toContain('--danger');
  });

  // ---------------------------------------------------------------------------
  // Nécessité d'un acte protégé — couleurs
  // ---------------------------------------------------------------------------

  it('necessite AUTORISATION_JP_PREALABLE → orange', () => {
    expect(component.necessiteClass('AUTORISATION_JP_PREALABLE')).toContain('--orange');
  });

  it('necessite ADMINISTRATEUR_SEUL → navy', () => {
    expect(component.necessiteClass('ADMINISTRATEUR_SEUL')).toContain('--navy');
  });

  it('necessite AUTONOMIE_PRESERVEE → vert', () => {
    expect(component.necessiteClass('AUTONOMIE_PRESERVEE')).toContain('--green');
  });

  it('necessite INTERDICTION_ABSOLUE → rouge', () => {
    expect(component.necessiteClass('INTERDICTION_ABSOLUE')).toContain('--red');
  });

  // ---------------------------------------------------------------------------
  // Bannière FRANCE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace FRANCE', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="pmb-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('droit belge');
  });

  // ---------------------------------------------------------------------------
  // PREFILL_COUNT_ALWAYS_ZERO = true
  // ---------------------------------------------------------------------------

  it('PREFILL_COUNT_ALWAYS_ZERO === true', () => {
    expect(ProtectionMajeurBeSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
  });

  it('getPrefillCount() retourne 0 sur input vide', () => {
    expect(ProtectionMajeurBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() retourne 0 même avec aiData rempli (V1)', () => {
    expect(
      ProtectionMajeurBeSectionComponent.getPrefillCount({
        aiData: { dateMariageBeDetectee: '2015-06-20' },
      }),
    ).toBe(0);
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
  // Statics TOOL_LABEL / TOOL_ICON
  // ---------------------------------------------------------------------------

  it('expose TOOL_LABEL / TOOL_ICON', () => {
    expect(ProtectionMajeurBeSectionComponent.TOOL_LABEL).toContain('PROTECTION DU MAJEUR');
    expect(ProtectionMajeurBeSectionComponent.TOOL_ICON).toBe('shield_person');
  });
});
