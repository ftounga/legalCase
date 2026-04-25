import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange, SimpleChanges } from '@angular/core';
import { DocumentsFinContratSectionComponent } from './documents-fin-contrat-section.component';
import { DocumentsFinContratResponse } from '../../core/models/documents-fin-contrat.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('DocumentsFinContratSectionComponent', () => {
  let component: DocumentsFinContratSectionComponent;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/documents-fin-contrat';
  const SOURCE_EXPL_URL = '/api/v1/case-files/case-1/source-explanations';

  function frResponse(): DocumentsFinContratResponse {
    return {
      caseFileId: 'case-1',
      dateFinContrat: '2026-04-01',
      certificatTravailRemis: true,
      dateCertificatTravail: '2026-04-05',
      attestationFranceTravailRemise: true,
      dateAttestationFranceTravail: '2026-04-05',
      souldeToutCompteSigne: true,
      dateSouldeToutCompte: '2026-04-15',
      salaireMensuelBrutEur: 3000,
      certificatRemisDansDelai: true,
      attestationRemiseDansDelai: true,
      souldeToutCompteValide: true,
      souldeToutCompteContestable: true,
      totalSanctionsCumulables: 0,
      indemniteRetardCertificatEur: 0,
      indemniteRetardAttestationEur: 0,
      scoreConformiteEmployeur: 100,
      verdictRisqueContentieux: 'FAIBLE',
      baseJuridique: 'Art. L.1234-19 + R.1234-9 + L.1234-20 Code du travail',
      formule: '3 documents requis : certificat OK, attestation OK, STC OK → score 100 = risque contentieux FAIBLE',
      messages: ['Délai légal certificat : remise immédiate (L.1234-19) — tolérance pratique 7 jours.'],
      messagesContentieux: [],
      country: 'FRANCE',
    };
  }

  function expectSourceExplanationCall(): void {
    const reqs = httpMock.match(SOURCE_EXPL_URL);
    reqs.forEach((r) => r.flush([]));
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        DocumentsFinContratSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(DocumentsFinContratSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // ---------------------------------------------------------------------------
  // Mount + GET / POST nominal
  // ---------------------------------------------------------------------------

  it('mount + workspaceCountry FRANCE par défaut', () => {
    expect(component).toBeTruthy();
    expect(component.workspaceCountry).toBe('FRANCE');
    expect(component.collapsed()).toBe(true);
  });

  it('charge l\'analyse existante si présente (GET 200)', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expectSourceExplanationCall();
    expect(component.result()!.scoreConformiteEmployeur).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(component.dateFinContrat()).toBe('2026-04-01');
    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.certificatTravailRemis()).toBe(true);
    expect(component.attestationFranceTravailRemise()).toBe(true);
    expect(component.souldeToutCompteSigne()).toBe(true);
    expect(component.souldeToutCompteContestableDelai6mois()).toBe(true);
  });

  it('reste en mode formulaire si GET 404', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid : true quand date + salaire OK', () => {
    component.dateFinContrat.set('2026-04-01');
    component.salaireMensuelBrutEur.set(3000);
    expect(component.formValid()).toBe(true);
  });

  it('formValid : false si date manquante', () => {
    component.dateFinContrat.set(null);
    component.salaireMensuelBrutEur.set(3000);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : false si salaire ≤ 0', () => {
    component.dateFinContrat.set('2026-04-01');
    component.salaireMensuelBrutEur.set(0);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelBrutEur.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : false si certificatRemis activé sans date associée', () => {
    component.dateFinContrat.set('2026-04-01');
    component.salaireMensuelBrutEur.set(3000);
    component.certificatTravailRemis.set(true);
    component.dateCertificatTravail.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : false si attestationRemise activé sans date associée', () => {
    component.dateFinContrat.set('2026-04-01');
    component.salaireMensuelBrutEur.set(3000);
    component.attestationFranceTravailRemise.set(true);
    component.dateAttestationFranceTravail.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid : false si stcSigne activé sans date associée', () => {
    component.dateFinContrat.set('2026-04-01');
    component.salaireMensuelBrutEur.set(3000);
    component.souldeToutCompteSigne.set(true);
    component.dateSouldeToutCompte.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ---------------------------------------------------------------------------
  // POST + erreurs
  // ---------------------------------------------------------------------------

  it('calculate() POST + affiche résultat + snackbar succès + triggerRefresh', () => {
    component.dateFinContrat.set('2026-04-01');
    component.salaireMensuelBrutEur.set(3000);
    component.certificatTravailRemis.set(true);
    component.dateCertificatTravail.set('2026-04-05');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateFinContrat: '2026-04-01',
      certificatTravailRemis: true,
      dateCertificatTravail: '2026-04-05',
      attestationFranceTravailRemise: false,
      dateAttestationFranceTravail: null,
      souldeToutCompteSigne: false,
      dateSouldeToutCompte: null,
      salaireMensuelBrutEur: 3000,
    });
    req.flush(frResponse());

    expect(component.result()!.scoreConformiteEmployeur).toBe(100);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith('Conformité documents calculée', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() erreur backend → snackbar rouge', () => {
    component.dateFinContrat.set('2026-04-01');
    component.salaireMensuelBrutEur.set(3000);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
  });

  it('calculate() ignoré si form invalide (pas d\'appel HTTP)', () => {
    component.dateFinContrat.set(null);
    component.salaireMensuelBrutEur.set(null);
    component.calculate();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  // ---------------------------------------------------------------------------
  // Gate BELGIQUE
  // ---------------------------------------------------------------------------

  it('BELGIQUE : pas de GET, pas de form, bannière info', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    httpMock.expectNone(SOURCE_EXPL_URL);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA (SF-DT-32-02 / SF-155-04 pattern)
  // ---------------------------------------------------------------------------

  it('SF-DT-32-02 : prefill IA salaire + dateFinContrat depuis aiData (GET 404 → form)', () => {
    component.aiData = {
      salaireBrutMensuel: 3000,
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.dateFinContrat()).toBe('2026-04-01');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceDateFinContrat()).toBe('IA');
  });

  it('SF-DT-32-02 : prefill sans aiData → pas de pré-remplissage', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.dateFinContrat()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceDateFinContrat()).toBeNull();
  });

  it('SF-DT-32-02 : salaireBrutMensuel ≤ 0 → pas de pré-fill salaire', () => {
    component.aiData = {
      salaireBrutMensuel: 0,
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.salaireMensuelBrutEur()).toBeNull();
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.dateFinContrat()).toBe('2026-04-01');
    expect(component.provenanceDateFinContrat()).toBe('IA');
  });

  it('SF-DT-32-02 : onSalaireChange efface le badge IA salaire', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceSalaire()).toBe('IA');
    component.onSalaireChange(3500);
    expect(component.salaireMensuelBrutEur()).toBe(3500);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('SF-DT-32-02 : onDateFinContratChange efface le badge IA date', () => {
    component.aiData = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    expect(component.provenanceDateFinContrat()).toBe('IA');
    component.onDateFinContratChange('2026-03-31');
    expect(component.dateFinContrat()).toBe('2026-03-31');
    expect(component.provenanceDateFinContrat()).toBeNull();
  });

  it('SF-DT-32-02 : loadExisting (GET 200) → pas de badge IA (valeurs persistées prioritaires)', () => {
    component.aiData = {
      salaireBrutMensuel: 9999,
      dateLicenciement: '2026-01-01',
    } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expectSourceExplanationCall();

    expect(component.salaireMensuelBrutEur()).toBe(3000);
    expect(component.dateFinContrat()).toBe('2026-04-01');
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceDateFinContrat()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // F-IA-03 : alertes de cohérence
  // ---------------------------------------------------------------------------

  it('SF-DT-32-02 : coherenceAlerts.SALAIRE présent si écart > 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(5000);

    const alerts = component.coherenceAlerts();
    expect(alerts.SALAIRE).toBeDefined();
    expect(alerts.SALAIRE!.field).toBe('SALAIRE');
    expect(alerts.SALAIRE!.source).toBe('IA');
    expect(alerts.SALAIRE!.contributors).toEqual(['IA']);
    expect(alerts.SALAIRE!.severity).toBe('WARNING');
    expect(alerts.SALAIRE!.expectedDisplay).toContain('€');
  });

  it('SF-DT-32-02 : coherenceAlerts.SALAIRE absent si écart ≤ 10 %', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(3100);

    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('SF-DT-32-02 : coherenceAlerts.DATE_FIN_CONTRAT présent si dates IA et user diffèrent', () => {
    component.aiData = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onDateFinContratChange('2026-03-15');

    const alerts = component.coherenceAlerts();
    expect(alerts.DATE_FIN_CONTRAT).toBeDefined();
    expect(alerts.DATE_FIN_CONTRAT!.field).toBe('DATE_FIN_CONTRAT');
    expect(alerts.DATE_FIN_CONTRAT!.source).toBe('IA');
    expect(alerts.DATE_FIN_CONTRAT!.expectedDisplay).toBe('2026-04-01');
  });

  it('SF-DT-32-02 : coherenceAlerts.DATE_FIN_CONTRAT absent si dates identiques', () => {
    component.aiData = { dateLicenciement: '2026-04-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    // dateFinContrat a été pré-rempli à 2026-04-01 → pas de divergence.
    expect(component.coherenceAlerts().DATE_FIN_CONTRAT).toBeUndefined();
  });

  // ---------------------------------------------------------------------------
  // ngOnChanges + handlers + helpers
  // ---------------------------------------------------------------------------

  it('SF-DT-32-02 : ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    const newAi = {
      salaireBrutMensuel: 2800,
      dateLicenciement: '2026-03-31',
    } as TravailExtractedData;
    component.aiData = newAi;
    const changes: SimpleChanges = {
      aiData: new SimpleChange(null, newAi, false),
    };
    component.ngOnChanges(changes);

    expect(component.salaireMensuelBrutEur()).toBe(2800);
    expect(component.dateFinContrat()).toBe('2026-03-31');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceDateFinContrat()).toBe('IA');
  });

  it('SF-DT-32-02 : ngOnChanges(aiData) après saisie manuelle n\'écrase pas la saisie avocat', () => {
    component.aiData = null;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();

    component.onSalaireChange(4200);
    component.onDateFinContratChange('2026-02-15');
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceDateFinContrat()).toBeNull();

    const newAi = {
      salaireBrutMensuel: 3000,
      dateLicenciement: '2026-04-01',
    } as TravailExtractedData;
    component.aiData = newAi;
    component.ngOnChanges({ aiData: new SimpleChange(null, newAi, false) });

    expect(component.salaireMensuelBrutEur()).toBe(4200);
    expect(component.dateFinContrat()).toBe('2026-02-15');
    expect(component.provenanceSalaire()).toBeNull();
    expect(component.provenanceDateFinContrat()).toBeNull();
  });

  it('SF-DT-32-02 : onCertificatRemisChange à false reset la date associée', () => {
    component.certificatTravailRemis.set(true);
    component.dateCertificatTravail.set('2026-04-05');
    component.onCertificatRemisChange(false);
    expect(component.certificatTravailRemis()).toBe(false);
    expect(component.dateCertificatTravail()).toBeNull();
  });

  it('SF-DT-32-02 : onAttestationRemiseChange à false reset la date associée', () => {
    component.attestationFranceTravailRemise.set(true);
    component.dateAttestationFranceTravail.set('2026-04-05');
    component.onAttestationRemiseChange(false);
    expect(component.attestationFranceTravailRemise()).toBe(false);
    expect(component.dateAttestationFranceTravail()).toBeNull();
  });

  it('SF-DT-32-02 : onSouldeSigneChange à false reset date STC + flag contestable', () => {
    component.souldeToutCompteSigne.set(true);
    component.dateSouldeToutCompte.set('2026-04-15');
    component.souldeToutCompteContestableDelai6mois.set(true);
    component.onSouldeSigneChange(false);
    expect(component.souldeToutCompteSigne()).toBe(false);
    expect(component.dateSouldeToutCompte()).toBeNull();
    expect(component.souldeToutCompteContestableDelai6mois()).toBe(false);
  });

  it('SF-DT-32-02 : alertes masquées après résultat affiché (showForm=false)', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(5000);
    expect(component.coherenceAlerts().SALAIRE).toBeDefined();

    component.showForm.set(false);
    expect(component.coherenceAlerts().SALAIRE).toBeUndefined();
  });

  it('SF-DT-32-02 : toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('SF-DT-32-02 : editMode ré-affiche le form', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Helpers de classe / label des cartes documents
  // ---------------------------------------------------------------------------

  it('SF-DT-32-02 : helpers cartes documents — verdict FAIBLE + tous OK', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expectSourceExplanationCall();

    expect(component.certificatStatutClass()).toBe('dfc-card--ok');
    expect(component.certificatStatutLabel()).toContain('OK');
    expect(component.attestationStatutClass()).toBe('dfc-card--ok');
    expect(component.attestationStatutLabel()).toContain('OK');
    expect(component.stcStatutClass()).toBe('dfc-card--ok');
    expect(component.stcStatutLabel()).toContain('OK');
    expect(component.verdictClass()).toBe('dfc-verdict--faible');
  });

  it('SF-DT-32-02 : helpers cartes documents — verdict ELEVE + tout manquant', () => {
    component.ngOnInit();
    const r = frResponse();
    r.certificatTravailRemis = false;
    r.dateCertificatTravail = null;
    r.certificatRemisDansDelai = false;
    r.attestationFranceTravailRemise = false;
    r.dateAttestationFranceTravail = null;
    r.attestationRemiseDansDelai = false;
    r.souldeToutCompteSigne = false;
    r.dateSouldeToutCompte = null;
    r.souldeToutCompteValide = false;
    r.souldeToutCompteContestable = false;
    r.scoreConformiteEmployeur = 30;
    r.verdictRisqueContentieux = 'ELEVE';
    httpMock.expectOne(BASE_URL).flush(r);
    expectSourceExplanationCall();

    expect(component.certificatStatutClass()).toBe('dfc-card--manquant');
    expect(component.certificatStatutLabel()).toContain('Non remis');
    expect(component.attestationStatutClass()).toBe('dfc-card--manquant');
    expect(component.stcStatutClass()).toBe('dfc-card--manquant');
    expect(component.stcStatutLabel()).toContain('Non signé');
    expect(component.verdictClass()).toBe('dfc-verdict--eleve');
  });

  it('SF-DT-32-02 : helpers cartes documents — certificat remis hors délai (retard)', () => {
    component.ngOnInit();
    const r = frResponse();
    r.certificatRemisDansDelai = false;
    r.indemniteRetardCertificatEur = 300;
    httpMock.expectOne(BASE_URL).flush(r);
    expectSourceExplanationCall();

    expect(component.certificatStatutClass()).toBe('dfc-card--retard');
    expect(component.certificatStatutLabel()).toContain('Hors délai');
  });

  it('SF-DT-32-02 : alertBadgeLabel + alertTooltip avec source IA', () => {
    component.aiData = { salaireBrutMensuel: 3000 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expectSourceExplanationCall();
    component.onSalaireChange(5000);
    const alert = component.coherenceAlerts().SALAIRE!;
    expect(component.alertBadgeLabel(alert)).toContain('Incohérence');
    expect(component.alertTooltip(alert)).toBeTruthy();
  });
});
