import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { ChangementEtatCivilSectionComponent } from './changement-etat-civil-section.component';
import { ChangementEtatCivilResponse } from '../../core/models/changement-etat-civil.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('ChangementEtatCivilSectionComponent', () => {
  let component: ChangementEtatCivilSectionComponent;
  let fixture: ComponentFixture<ChangementEtatCivilSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/changement-etat-civil';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function defaultResponse(): ChangementEtatCivilResponse {
    return {
      caseFileId: 'case-1',
      typeChangement: 'PRENOM',
      motifInvoque: 'INTERET_LEGITIME',
      preuvesProduites: ['LIVRET_FAMILLE', 'CERTIFICAT_NAISSANCE'],
      majeurDemandeur: true,
      consentementParental: false,
      datesDocsConcordants: true,
      dejaChangeAuparavant: false,
      dateNaissanceDemandeur: '1985-03-12',
      departementDeclaration: '75',
      competenceProcedure: 'MAIRIE',
      delaiInstructionMoisPrevisionnel: 3,
      scoreAcceptabilite: 78,
      verdictAcceptabilite: 'ELEVEE',
      documentsRequisManquants: [],
      baseJuridique: 'Art. 60 Cciv ; loi 2016-1547',
      formule: 'score = 78/100 — compétence MAIRIE',
      messages: ['Procédure simplifiée loi 2016-1547 — déclaration en mairie possible.'],
      country: 'FRANCE',
    };
  }

  /**
   * Absorbe la requête source-explanations émise par `loadSourceExplanations()`
   * dans `ngOnInit` (fail-open). Empêche `httpMock.verify()` de crasher.
   */
  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);

    await TestBed.configureTestingModule({
      imports: [
        ChangementEtatCivilSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ChangementEtatCivilSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + options
  // ---------------------------------------------------------------------------

  it('mount sans erreur (FRANCE) avec options enums', () => {
    expect(component).toBeTruthy();
    expect(component.typesOptions.length).toBe(4);
    expect(component.motifsOptions.length).toBe(5);
    expect(component.preuvesOptions.length).toBe(7);
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid faux si typeChangement null', () => {
    component.typeChangement.set(null);
    component.motifInvoque.set('INTERET_LEGITIME');
    component.preuvesProduites.set(['LIVRET_FAMILLE']);
    component.dateNaissanceDemandeur.set('1985-03-12');
    component.departementDeclaration.set('75');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si motifInvoque null', () => {
    component.typeChangement.set('PRENOM');
    component.motifInvoque.set(null);
    component.preuvesProduites.set(['LIVRET_FAMILLE']);
    component.dateNaissanceDemandeur.set('1985-03-12');
    component.departementDeclaration.set('75');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si preuves vides', () => {
    component.typeChangement.set('PRENOM');
    component.motifInvoque.set('INTERET_LEGITIME');
    component.preuvesProduites.set([]);
    component.dateNaissanceDemandeur.set('1985-03-12');
    component.departementDeclaration.set('75');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si dateNaissance non ISO', () => {
    component.typeChangement.set('PRENOM');
    component.motifInvoque.set('INTERET_LEGITIME');
    component.preuvesProduites.set(['LIVRET_FAMILLE']);
    component.dateNaissanceDemandeur.set('12/03/1985');
    component.departementDeclaration.set('75');
    expect(component.formValid()).toBe(false);
  });

  it('formValid faux si departement vide', () => {
    component.typeChangement.set('PRENOM');
    component.motifInvoque.set('INTERET_LEGITIME');
    component.preuvesProduites.set(['LIVRET_FAMILLE']);
    component.dateNaissanceDemandeur.set('1985-03-12');
    component.departementDeclaration.set('  ');
    expect(component.formValid()).toBe(false);
  });

  it('formValid vrai sur saisie minimale complète', () => {
    component.typeChangement.set('PRENOM');
    component.motifInvoque.set('INTERET_LEGITIME');
    component.preuvesProduites.set(['LIVRET_FAMILLE']);
    component.dateNaissanceDemandeur.set('1985-03-12');
    component.departementDeclaration.set('75');
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  it('GET 200 → form masqué + valeurs persistées + pas de badge IA', () => {
    component.aiData = {
      typeChangementDetecte: 'NOM',
    } as FamilleExtractedData;
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(defaultResponse());
    expectSourceExplanationCall();

    expect(component.result()!.scoreAcceptabilite).toBe(78);
    expect(component.showForm()).toBe(false);
    expect(component.typeChangement()).toBe('PRENOM'); // persisté
    expect(component.provenanceTypeChangement()).toBeNull();
    expect(component.preuvesProduites().length).toBe(2);
  });

  it('GET 404 → mode formulaire + pré-fill IA appliqué (5 fields)', () => {
    component.aiData = {
      typeChangementDetecte: 'PRENOM',
      motifChangementDetecte: 'INTERET_LEGITIME',
      dateNaissanceDemandeurDetectee: '1985-03-12',
      majeurDemandeurDetected: true,
      consentementParentalDetected: true,
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.typeChangement()).toBe('PRENOM');
    expect(component.motifInvoque()).toBe('INTERET_LEGITIME');
    expect(component.dateNaissanceDemandeur()).toBe('1985-03-12');
    expect(component.majeurDemandeur()).toBe(true);
    expect(component.consentementParental()).toBe(true);
    expect(component.provenanceTypeChangement()).toBe('IA');
    expect(component.provenanceMotifInvoque()).toBe('IA');
    expect(component.provenanceDateNaissance()).toBe('IA');
    expect(component.provenanceMajeurDemandeur()).toBe('IA');
    expect(component.provenanceConsentementParental()).toBe('IA');
  });

  it('GET 404 + aiData absent → no-op gracieux (pas de pré-fill)', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.showForm()).toBe(true);
    expect(component.typeChangement()).toBeNull();
    expect(component.motifInvoque()).toBeNull();
    expect(component.dateNaissanceDemandeur()).toBeNull();
  });

  it('prefillFromAi ignore les enums hors liste', () => {
    component.aiData = {
      typeChangementDetecte: 'NATIONALITE', // hors enum
      motifChangementDetecte: 'INCONNU',    // hors enum
    } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.typeChangement()).toBeNull();
    expect(component.motifInvoque()).toBeNull();
    expect(component.provenanceTypeChangement()).toBeNull();
  });

  it('calculate() POST → result + snackbar succès + dashboardRefresh', () => {
    component.typeChangement.set('PRENOM');
    component.motifInvoque.set('INTERET_LEGITIME');
    component.preuvesProduites.set(['LIVRET_FAMILLE', 'CERTIFICAT_NAISSANCE']);
    component.majeurDemandeur.set(true);
    component.consentementParental.set(false);
    component.datesDocsConcordants.set(true);
    component.dejaChangeAuparavant.set(false);
    component.dateNaissanceDemandeur.set('1985-03-12');
    component.departementDeclaration.set('75');

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      typeChangement: 'PRENOM',
      motifInvoque: 'INTERET_LEGITIME',
      preuvesProduites: ['LIVRET_FAMILLE', 'CERTIFICAT_NAISSANCE'],
      majeurDemandeur: true,
      consentementParental: false,
      datesDocsConcordants: true,
      dejaChangeAuparavant: false,
      dateNaissanceDemandeur: '1985-03-12',
      departementDeclaration: '75',
    });
    req.flush(defaultResponse());

    expect(component.result()!.verdictAcceptabilite).toBe('ELEVEE');
    expect(component.result()!.competenceProcedure).toBe('MAIRIE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse changement état civil calculée',
      'OK',
      jasmine.any(Object),
    );
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur 400 → snackbar rouge', () => {
    component.typeChangement.set('PRENOM');
    component.motifInvoque.set('INTERET_LEGITIME');
    component.preuvesProduites.set(['LIVRET_FAMILLE']);
    component.dateNaissanceDemandeur.set('1985-03-12');
    component.departementDeclaration.set('75');
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
    component.typeChangement.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Provenance handlers (badge IA effacé sur edit manuel)
  // ---------------------------------------------------------------------------

  it('onTypeChangementChange efface le badge IA', () => {
    component.aiData = { typeChangementDetecte: 'PRENOM' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceTypeChangement()).toBe('IA');
    component.onTypeChangementChange('NOM');
    expect(component.typeChangement()).toBe('NOM');
    expect(component.provenanceTypeChangement()).toBeNull();
  });

  it('onMotifInvoqueChange efface le badge IA', () => {
    component.aiData = { motifChangementDetecte: 'MARIAGE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceMotifInvoque()).toBe('IA');
    component.onMotifInvoqueChange('AUTRE');
    expect(component.motifInvoque()).toBe('AUTRE');
    expect(component.provenanceMotifInvoque()).toBeNull();
  });

  it('onDateNaissanceChange efface le badge IA', () => {
    component.aiData = { dateNaissanceDemandeurDetectee: '1985-03-12' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDateNaissance()).toBe('IA');
    component.onDateNaissanceChange('1990-07-01');
    expect(component.dateNaissanceDemandeur()).toBe('1990-07-01');
    expect(component.provenanceDateNaissance()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Coherence alerts F-IA-03
  // ---------------------------------------------------------------------------

  it('coherenceAlerts.TYPE_CHANGEMENT si IA dit autre chose', () => {
    component.aiData = { typeChangementDetecte: 'NOM' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onTypeChangementChange('PRENOM');

    const alerts = component.coherenceAlerts();
    expect(alerts.TYPE_CHANGEMENT).toBeDefined();
    expect(alerts.TYPE_CHANGEMENT!.field).toBe('TYPE_CHANGEMENT');
    expect(alerts.TYPE_CHANGEMENT!.source).toBe('IA');
    expect(alerts.TYPE_CHANGEMENT!.expectedDisplay).toContain('nom');
  });

  it('coherenceAlerts.DATE_NAISSANCE multi-source IA + F96 + PIECE_MANQUANTE', () => {
    component.aiData = {
      dateNaissanceDemandeurDetectee: '1985-03-12',
    } as FamilleExtractedData;
    component.procedureChecks = [
      {
        id: 'c1', ordre: 1, description: 'Date naissance', statut: 'NON_COMPLIANT',
        critereCode: 'FA26_DATE_NAISSANCE', expectedValue: '1985-03-12', raison: 'IA détecté',
      },
    ] as ProcedureCheck[];
    component.piecesManquantes = [
      { texte: 'Certificat de naissance', critereCode: 'CERTIFICAT_NAISSANCE' },
    ] as PieceManquanteEntry[];
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onDateNaissanceChange('1990-01-01'); // divergence

    const alert = component.coherenceAlerts().DATE_NAISSANCE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('DATE_NAISSANCE');
    expect(alert!.contributors).toContain('IA');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.source).toBe('MULTI');
    expect(alert!.pieceTexte).toBe('Certificat de naissance');
  });

  it('coherenceAlerts.CONSENTEMENT_PARENTAL si IA dit consentement et user dit non', () => {
    component.aiData = { consentementParentalDetected: true } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    // pré-fill a coché true → simulation décocheage manuel.
    component.onConsentementParentalChange(false);
    const alert = component.coherenceAlerts().CONSENTEMENT_PARENTAL;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('CONSENTEMENT_PARENTAL');
    expect(alert!.expectedDisplay).toContain('Consentement');
  });

  it('coherenceAlerts.MOTIF_INVOQUE divergence enum simple', () => {
    component.aiData = { motifChangementDetecte: 'IDENTIFICATION_GENRE' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onMotifInvoqueChange('AUTRE');

    const alert = component.coherenceAlerts().MOTIF_INVOQUE;
    expect(alert).toBeDefined();
    expect(alert!.field).toBe('MOTIF_INVOQUE');
    expect(alert!.expectedDisplay).toContain('genre');
  });

  it('alertes masquées après résultat (showForm=false, anti-bug SF-IA-03-12)', () => {
    component.aiData = { typeChangementDetecte: 'NOM' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onTypeChangementChange('PRENOM');
    expect(component.coherenceAlerts().TYPE_CHANGEMENT).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().TYPE_CHANGEMENT).toBeUndefined();
  });

  it('alertBadgeLabel reflète le type de source (IA / MULTI)', () => {
    const alertIA = {
      field: 'TYPE_CHANGEMENT' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Nom',
      reason: 'r',
    };
    const alertMulti = {
      ...alertIA, source: 'MULTI' as const,
      contributors: ['IA' as const, 'F96' as const],
    };
    expect(component.alertBadgeLabel(alertIA)).toContain('Incohérence détectée');
    expect(component.alertBadgeLabel(alertIA)).toContain('Nom');
    expect(component.alertBadgeLabel(alertMulti)).toContain('multiple');
  });

  it('alertTooltip ajoute "Contredit" si > 1 contributor', () => {
    const alertSingle = {
      field: 'TYPE_CHANGEMENT' as const,
      source: 'IA' as const,
      contributors: ['IA' as const],
      severity: 'WARNING' as const,
      expectedDisplay: 'Nom',
      reason: 'simple',
    };
    const alertMulti = {
      ...alertSingle, source: 'MULTI' as const,
      contributors: ['IA' as const, 'F96' as const],
    };
    expect(component.alertTooltip(alertSingle)).toBe('simple');
    expect(component.alertTooltip(alertMulti)).toContain('Contredit');
  });

  it('explanationFor retourne [] si aucune source explanation chargée (fail-open)', () => {
    expect(component.explanationFor('TYPE_CHANGEMENT')).toEqual([]);
    expect(component.explanationFor('MOTIF_INVOQUE')).toEqual([]);
    expect(component.explanationFor('DATE_NAISSANCE')).toEqual([]);
    expect(component.explanationFor('MAJEUR_DEMANDEUR')).toEqual([]);
    expect(component.explanationFor('CONSENTEMENT_PARENTAL')).toEqual([]);
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges — propagation des inputs
  // ---------------------------------------------------------------------------

  it('ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      typeChangementDetecte: 'PRENOM',
      motifChangementDetecte: 'INTERET_LEGITIME',
    } as FamilleExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({
      aiData: new SimpleChange(null, newAi, false),
    });

    expect(component.typeChangement()).toBe('PRENOM');
    expect(component.motifInvoque()).toBe('INTERET_LEGITIME');
    expect(component.provenanceTypeChangement()).toBe('IA');
  });

  it('ngOnChanges propage les nouveaux procedureChecks', () => {
    component.aiData = { typeChangementDetecte: 'NOM' } as FamilleExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newChecks: ProcedureCheck[] = [
      {
        id: 'c2', ordre: 2, description: 'Type', statut: 'NON_COMPLIANT',
        critereCode: 'FA26_TYPE_CHANGEMENT', expectedValue: 'Changement de nom',
      } as ProcedureCheck,
    ];
    component.procedureChecks = newChecks;
    component.ngOnChanges({
      procedureChecks: new SimpleChange(null, newChecks, false),
    });

    component.onTypeChangementChange('PRENOM'); // divergence avec IA NOM
    const alert = component.coherenceAlerts().TYPE_CHANGEMENT;
    expect(alert).toBeDefined();
    expect(alert!.contributors).toContain('F96');
  });

  // ---------------------------------------------------------------------------
  // Gate workspaceCountry
  // ---------------------------------------------------------------------------

  it('gate BELGIQUE → form non rendu, GET non appelé', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    expect(component.isFrance()).toBe(false);
  });

  it('gate FRANCE → load() appelé', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.isFrance()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // UI helpers
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

  it('verdictBannerClass renvoie la classe attendue (strong/medium/weak)', () => {
    expect(component.verdictBannerClass('ELEVEE')).toContain('strong');
    expect(component.verdictBannerClass('MOYENNE')).toContain('medium');
    expect(component.verdictBannerClass('FAIBLE')).toContain('weak');
  });

  it('competenceCardClass renvoie la classe attendue (mairie/juge/tribunal)', () => {
    expect(component.competenceCardClass('MAIRIE')).toContain('mairie');
    expect(component.competenceCardClass('JUGE')).toContain('juge');
    expect(component.competenceCardClass('TRIBUNAL_JUDICIAIRE')).toContain('tribunal');
  });

  it('competenceIcon renvoie une icône non vide pour chaque compétence', () => {
    expect(component.competenceIcon('MAIRIE')).toBe('account_balance');
    expect(component.competenceIcon('JUGE')).toBe('gavel');
    expect(component.competenceIcon('TRIBUNAL_JUDICIAIRE')).toBe('balance');
  });

  it('competenceProcedureLabel mappe les 3 codes', () => {
    expect(component.competenceProcedureLabel('MAIRIE')).toBe('Mairie');
    expect(component.competenceProcedureLabel('JUGE')).toContain('Juge');
    expect(component.competenceProcedureLabel('TRIBUNAL_JUDICIAIRE')).toContain('Tribunal');
  });

  it('typeChangementLabel + motifInvoqueLabel + preuveEtatCivilLabel mappent les codes', () => {
    expect(component.typeChangementLabel('NOM')).toContain('nom');
    expect(component.motifInvoqueLabel('IDENTIFICATION_GENRE')).toContain('genre');
    expect(component.preuveEtatCivilLabel('EXPERTISE_MEDICALE')).toContain('Expertise');
  });
});
