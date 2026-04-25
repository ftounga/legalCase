import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { MesuresProvisoiresSectionComponent } from './mesures-provisoires-section.component';
import {
  MesuresProvisoiresAiData,
  MesuresProvisoiresResponse,
} from '../../core/models/mesures-provisoires.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('MesuresProvisoiresSectionComponent', () => {
  let component: MesuresProvisoiresSectionComponent;
  let fixture: ComponentFixture<MesuresProvisoiresSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/mesures-provisoires';

  function defaultResponse(): MesuresProvisoiresResponse {
    return {
      caseFileId: 'case-1',
      dateAudienceAOMP: '2026-06-15',
      revenusEpouxDemandeurEur: 3500,
      revenusEpouxDefendeurEur: 2000,
      logementCommunDescription: 'Maison Lyon',
      logementProprietaire: 'EN_INDIVISION',
      enfantsMineurs: [{ prenom: 'Léa', age: 8 }],
      souhaitResidenceEnfants: 'ALTERNEE',
      violencesAlleguees: false,
      patrimoineCommunIsignificatif: true,
      demandeMesureConservatoire: false,
      country: 'FRANCE',
      differentielRevenus: 1500,
      pensionAlimentairePropose: 750,
      attributionLogementRecommande: 'DEFENDEUR',
      residenceEnfantsRecommande: 'ALTERNEE',
      contributionCharges: 1750,
      mesureConservatoireRecommande: false,
      scoreCohesionMesures: 100,
      verdictAcceptabilite: 'ELEVEE',
      formule: 'Mesures provisoires (art. 254 Cciv) : score 100/100',
      baseJuridique: 'Art. 254-256 Cciv + jurisprudence Cass. 1ère civ.',
      messages: ['Demande à formuler à l\'AOMP'],
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        MesuresProvisoiresSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MesuresProvisoiresSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + form validators
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE)', () => {
    expect(component).toBeTruthy();
    expect(component.logementOptions.length).toBe(4);
    expect(component.residenceOptions.length).toBe(3);
  });

  it('formValid faux si dateAudienceAOMP null', () => {
    component.revenusEpouxDemandeurEur.set(3500);
    component.revenusEpouxDefendeurEur.set(2000);
    component.logementProprietaire.set('EN_INDIVISION');
    component.souhaitResidenceEnfants.set('ALTERNEE');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai si tous champs requis remplis', () => {
    component.dateAudienceAOMP.set('2026-06-15');
    component.revenusEpouxDemandeurEur.set(3500);
    component.revenusEpouxDefendeurEur.set(2000);
    component.logementProprietaire.set('EN_INDIVISION');
    component.souhaitResidenceEnfants.set('ALTERNEE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid faux si revenu négatif', () => {
    component.dateAudienceAOMP.set('2026-06-15');
    component.revenusEpouxDemandeurEur.set(-10);
    component.revenusEpouxDefendeurEur.set(2000);
    component.logementProprietaire.set('EN_INDIVISION');
    component.souhaitResidenceEnfants.set('ALTERNEE');
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué, valeurs persistées affichées, pas de badge IA', () => {
    component.aiData = {
      revenusEpouxDemandeurEur: 9999,
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());

    expect(component.result()!.scoreCohesionMesures).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(component.revenusEpouxDemandeurEur()).toBe(3500);
    expect(component.provenanceRevenusDemandeur()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire ; pré-fill IA appliqué', () => {
    component.aiData = {
      dateAudienceAOMP: '2026-06-15',
      revenusEpouxDemandeurEur: 3500,
      revenusEpouxDefendeurEur: 2000,
      violencesAlleguees: false,
      patrimoineCommunSignificatif: true,
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.showForm()).toBe(true);
    expect(component.dateAudienceAOMP()).toBe('2026-06-15');
    expect(component.revenusEpouxDemandeurEur()).toBe(3500);
    expect(component.revenusEpouxDefendeurEur()).toBe(2000);
    expect(component.patrimoineCommunIsignificatif()).toBe(true);
    expect(component.provenanceDateAudience()).toBe('IA');
    expect(component.provenanceRevenusDemandeur()).toBe('IA');
    expect(component.provenanceRevenusDefendeur()).toBe('IA');
    expect(component.provenancePatrimoine()).toBe('IA');
  });

  it('pré-fill IA accepte revenusAnnuelsEpoux1Eur (annuel /12)', () => {
    component.aiData = {
      revenusAnnuelsEpoux1Eur: 42000,
      revenusAnnuelsEpoux2Eur: 24000,
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.revenusEpouxDemandeurEur()).toBe(3500); // 42000/12
    expect(component.revenusEpouxDefendeurEur()).toBe(2000); // 24000/12
    expect(component.provenanceRevenusDemandeur()).toBe('IA');
  });

  it('calculate() POST → résultat affiché + snackbar succès + dashboardRefresh', () => {
    component.dateAudienceAOMP.set('2026-06-15');
    component.revenusEpouxDemandeurEur.set(3500);
    component.revenusEpouxDefendeurEur.set(2000);
    component.logementProprietaire.set('EN_INDIVISION');
    component.souhaitResidenceEnfants.set('ALTERNEE');
    component.violencesAlleguees.set(false);
    component.patrimoineCommunIsignificatif.set(true);
    component.demandeMesureConservatoire.set(false);
    component.enfantsMineurs.set([{ prenom: 'Léa', age: 8 }]);

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.dateAudienceAOMP).toBe('2026-06-15');
    expect(req.request.body.revenusEpouxDemandeurEur).toBe(3500);
    expect(req.request.body.logementProprietaire).toBe('EN_INDIVISION');
    expect(req.request.body.enfantsMineurs).toEqual([{ prenom: 'Léa', age: 8 }]);
    req.flush(defaultResponse());

    expect(component.result()!.verdictAcceptabilite).toBe('ELEVEE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Mesures provisoires calculées',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.dateAudienceAOMP.set('2026-06-15');
    component.revenusEpouxDemandeurEur.set(3500);
    component.revenusEpouxDefendeurEur.set(2000);
    component.logementProprietaire.set('EN_INDIVISION');
    component.calculate();

    httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL)
      .flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onRevenusDemandeurChange efface le badge IA', () => {
    component.aiData = {
      revenusEpouxDemandeurEur: 3500,
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceRevenusDemandeur()).toBe('IA');
    component.onRevenusDemandeurChange(4000);
    expect(component.revenusEpouxDemandeurEur()).toBe(4000);
    expect(component.provenanceRevenusDemandeur()).toBeNull();
  });

  it('onDateAudienceChange efface le badge IA', () => {
    component.aiData = {
      dateAudienceAOMP: '2026-06-15',
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.provenanceDateAudience()).toBe('IA');
    component.onDateAudienceChange('2026-07-01');
    expect(component.dateAudienceAOMP()).toBe('2026-07-01');
    expect(component.provenanceDateAudience()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03 (CoherenceAlertBuilder)
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.REVENUS_DEMANDEUR présent si écart > 10 %', () => {
    component.aiData = {
      revenusEpouxDemandeurEur: 3500,
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenusDemandeurChange(5000); // écart ~43 %

    const alert = component.coherenceAlerts().REVENUS_DEMANDEUR;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('REVENUS_DEMANDEUR');
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.severity).toBe('WARNING');
  });

  it('coherenceAlerts.REVENUS_DEMANDEUR absent si écart ≤ 10 %', () => {
    component.aiData = {
      revenusEpouxDemandeurEur: 3500,
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenusDemandeurChange(3600); // écart ~3 %

    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeUndefined();
  });

  it('coherenceAlerts.DATE_AUDIENCE présent si IA et user divergent', () => {
    component.aiData = {
      dateAudienceAOMP: '2026-06-15',
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onDateAudienceChange('2026-09-01');
    const alert = component.coherenceAlerts().DATE_AUDIENCE;
    expect(alert).toBeDefined();
    expect(alert!.expectedDisplay).toBe('2026-06-15');
    expect(alert!.source).toBe('IA');
  });

  it('coherenceAlerts.VIOLENCES sévérité CRITICAL si IA détecte violences mais avocat à false', () => {
    component.aiData = {
      violencesAlleguees: true,
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    // Le pré-fill a posé violences=true ; on simule l'avocat qui efface (false).
    component.onViolencesChange(false);
    const alert = component.coherenceAlerts().VIOLENCES;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('CRITICAL');
    expect(alert!.expectedDisplay).toBe('Oui');
  });

  it('coherenceAlerts.REVENUS_DEMANDEUR multi-source (IA + F96 + PIECE_MANQUANTE)', () => {
    component.aiData = {
      revenusEpouxDemandeurEur: 3500,
    } as unknown as MesuresProvisoiresAiData;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Bulletin de salaire', statut: 'NON_COMPLIANT',
        critereCode: 'FA12_REVENUS_DEMANDEUR', expectedValue: '3500', raison: 'bulletin produit',
      } as ProcedureCheck,
    ];
    component.piecesManquantes = [
      { texte: 'Avis d\'imposition demandeur', critereCode: 'AVIS_IMPOSITION_DEMANDEUR' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.onRevenusDemandeurChange(5000); // écart 43 %
    const alert = component.coherenceAlerts().REVENUS_DEMANDEUR;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.pieceTexte).toBe('Avis d\'imposition demandeur');
  });

  it('alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = {
      revenusEpouxDemandeurEur: 3500,
    } as unknown as MesuresProvisoiresAiData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.onRevenusDemandeurChange(5000);
    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().REVENUS_DEMANDEUR).toBeUndefined();
  });

  it('alertBadgeLabel reflète la source (IA / F96 / MULTI)', () => {
    const alertIA = {
      field: 'REVENUS_DEMANDEUR' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: '3 500 €',
      reason: 'r',
    };
    const alertMulti = { ...alertIA, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('3 500 €');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const single = {
      field: 'DATE_AUDIENCE' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: '2026-06-15',
      reason: 'simple',
    };
    const multi = { ...single, source: 'MULTI' as const, contributors: ['IA' as const, 'F96' as const] };
    expect(component.alertTooltip(single)).toBe('simple');
    expect(component.alertTooltip(multi)).toContain('Contredit');
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const newAi = {
      dateAudienceAOMP: '2026-07-01',
      revenusEpouxDemandeurEur: 4000,
    } as unknown as MesuresProvisoiresAiData;
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.dateAudienceAOMP()).toBe('2026-07-01');
    expect(component.revenusEpouxDemandeurEur()).toBe(4000);
    expect(component.provenanceDateAudience()).toBe('IA');
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('gate BELGIQUE → form non rendu, GET non appelé', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.isFranceGate()).toBe(false);
  });

  it('gate FRANCE → load() appelé', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.isFranceGate()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // UI helpers + enfants list
  // ---------------------------------------------------------------------------

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('addEnfant / updateEnfantPrenom / updateEnfantAge / removeEnfant', () => {
    component.addEnfant();
    component.addEnfant();
    expect(component.enfantsMineurs().length).toBe(2);
    component.updateEnfantPrenom(0, 'Léa');
    component.updateEnfantAge(0, 8);
    expect(component.enfantsMineurs()[0]).toEqual({ prenom: 'Léa', age: 8 });
    component.removeEnfant(1);
    expect(component.enfantsMineurs().length).toBe(1);
  });

  it('verdictBannerClass renvoie strong/medium/weak selon verdict', () => {
    expect(component.verdictBannerClass('ELEVEE')).toContain('strong');
    expect(component.verdictBannerClass('MOYENNE')).toContain('medium');
    expect(component.verdictBannerClass('FAIBLE')).toContain('weak');
  });

  it('residenceCardClass mappe les 4 valeurs', () => {
    expect(component.residenceCardClass('ALTERNEE')).toContain('ok');
    expect(component.residenceCardClass('EXCLUSIVE_DEMANDEUR')).toContain('neutral');
    expect(component.residenceCardClass('EXCLUSIVE_DEFENDEUR')).toContain('neutral');
    expect(component.residenceCardClass('SANS_OBJET')).toContain('muted');
  });
});
