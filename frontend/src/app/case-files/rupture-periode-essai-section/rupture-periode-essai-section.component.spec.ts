import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { RupturePeriodeEssaiSectionComponent } from './rupture-periode-essai-section.component';
import { RupturePeriodeEssaiResponse } from '../../core/models/rupture-periode-essai.model';

/**
 * SF-DT-38-02 : tests du composant outil "Rupture de période d'essai" (F-DT-38).
 */
describe('RupturePeriodeEssaiSectionComponent', () => {
  let component: RupturePeriodeEssaiSectionComponent;
  let fixture: ComponentFixture<RupturePeriodeEssaiSectionComponent>;
  let httpMock: HttpTestingController;

  function regulierResponse(): RupturePeriodeEssaiResponse {
    return {
      caseFileId: 'cf-1',
      categorieSocioProfessionnelle: 'CADRE',
      typeContrat: 'CDI',
      dureeCddMois: null,
      dateDebutContrat: '2025-01-01',
      dateRupture: '2025-04-10',
      dureePeriodeEssaiContractuelleMois: 4,
      renouvellementInvoque: false,
      accordBrancheRenouvellement: null,
      accordEcritSalarieRenouvellement: null,
      auteurRupture: 'EMPLOYEUR',
      delaiPrevenanceJoursAppliques: 30,
      motifInvoque: 'Insuffisance',
      motifLieAuxCompetencesProfessionnelles: true,
      motifEconomiqueOuOrganisationnel: false,
      discriminationInvoquee: null,
      grossesseAuMomentRupture: false,
      arretAccidentTravailEnCours: false,
      atteinteLiberteFondamentale: null,
      lettreRuptureMotivee: true,
      motifsAveresParPieces: true,
      conventionCollectiveApplicable: false,
      conventionCollectivePlusFavorableRespectee: null,
      salaireMensuelBrut: 4500,
      verdict: 'REGULIERE',
      scoreIrregularite: 0,
      ancienneteJoursAuMomentRupture: 99,
      dureeLegaleMaximaleMois: 4,
      delaiPrevenanceLegalJours: 30,
      delaiPrevenanceRespecte: true,
      anomaliesDetectees: [],
      indemniteEstimee: null,
      remedeReintegration: false,
      basesJuridiques: ['Art. L.1221-19 C. trav.', 'Art. L.1221-25 C. trav.'],
      messages: ['Rupture conforme aux articles L.1221-19 à L.1221-25.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RupturePeriodeEssaiSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        MatSnackBarModule,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(RupturePeriodeEssaiSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('crée le composant', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/case-files/cf-1/rupture-periode-essai').flush(null, { status: 404, statusText: 'Not Found' });
    expect(component).toBeTruthy();
  });

  it('charge le résultat existant au ngOnInit', fakeAsync(() => {
    fixture.detectChanges();
    const req = httpMock.expectOne('/api/v1/case-files/cf-1/rupture-periode-essai');
    expect(req.request.method).toBe('GET');
    req.flush(regulierResponse());
    tick();
    expect(component.result()?.verdict).toBe('REGULIERE');
    expect(component.showForm()).toBe(false);
  }));

  it('404 sur le GET initial reste en mode formulaire', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/case-files/cf-1/rupture-periode-essai')
        .flush(null, { status: 404, statusText: 'Not Found' });
    tick();
    expect(component.result()).toBeNull();
    expect(component.showForm()).toBe(true);
  }));

  it('ne fait pas de GET en workspace BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    fixture.detectChanges();
    httpMock.expectNone('/api/v1/case-files/cf-1/rupture-periode-essai');
  });

  it('calculate envoie le POST avec tous les champs', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/case-files/cf-1/rupture-periode-essai')
        .flush(null, { status: 404, statusText: 'Not Found' });
    tick();

    component.dateDebutContrat.set('2025-01-01');
    component.dateRupture.set('2025-04-10');
    component.dureePeriodeEssaiContractuelleMois.set(4);
    component.salaireMensuelBrut.set(4500);

    component.calculate();
    const postReq = httpMock.expectOne('/api/v1/case-files/cf-1/rupture-periode-essai');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.categorieSocioProfessionnelle).toBe('CADRE');
    expect(postReq.request.body.typeContrat).toBe('CDI');
    expect(postReq.request.body.auteurRupture).toBe('EMPLOYEUR');
    postReq.flush(regulierResponse());
    tick();
    expect(component.result()?.verdict).toBe('REGULIERE');
  }));

  it('formValid retourne false sans dates', () => {
    expect(component.formValid()).toBe(false);
    component.dateDebutContrat.set('2025-01-01');
    expect(component.formValid()).toBe(false);
    component.dateRupture.set('2025-04-10');
    expect(component.formValid()).toBe(true);
  });

  it('formValid retourne false pour workspace BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.dateDebutContrat.set('2025-01-01');
    component.dateRupture.set('2025-04-10');
    expect(component.formValid()).toBe(false);
  });

  it('verdictBannerClass retourne la bonne classe selon verdict', () => {
    expect(component.verdictBannerClass('REGULIERE')).toContain('available');
    expect(component.verdictBannerClass('RISQUE_ABUSIVE')).toContain('medium');
    expect(component.verdictBannerClass('NULLE')).toContain('danger');
    expect(component.verdictBannerClass('ILLEGALE_REQUALIF_LICENCIEMENT')).toContain('danger');
  });

  it('verdictBannerLabel inclut "réintégration" pour NULLE', () => {
    expect(component.verdictBannerLabel('NULLE')).toContain('réintégration');
  });

  it('verdictBannerLabel inclut "requalification licenciement" pour ILLEGALE', () => {
    expect(component.verdictBannerLabel('ILLEGALE_REQUALIF_LICENCIEMENT')).toContain('requalification');
  });

  it('anomalieGraviteClass retourne la bonne classe', () => {
    expect(component.anomalieGraviteClass('AVERE')).toContain('avere');
    expect(component.anomalieGraviteClass('PROBABLE')).toContain('probable');
  });

  it('toggleCollapse alterne l\'état', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('pré-fill IA depuis aiData (FRANCE)', () => {
    component.aiData = {
      typeContrat: 'CDD',
      dateEntree: '2024-12-01',
      dateLicenciement: '2025-02-15',
      motifLicenciement: 'Insuffisance',
      atMpDetecte: true,
      salaireBrutMensuel: 3200,
      conventionCollective: 'IDCC_1486',
    } as any;
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/case-files/cf-1/rupture-periode-essai')
        .flush(null, { status: 404, statusText: 'Not Found' });
    expect(component.typeContrat()).toBe('CDD');
    expect(component.dateDebutContrat()).toBe('2024-12-01');
    expect(component.dateRupture()).toBe('2025-02-15');
    expect(component.motifInvoque()).toBe('Insuffisance');
    expect(component.arretAccidentTravailEnCours()).toBe(true);
    expect(component.salaireMensuelBrut()).toBe(3200);
    expect(component.conventionCollectiveApplicable()).toBe(true);
    expect(component.provenanceTypeContrat()).toBe('IA');
    expect(component.provenanceDateDebutContrat()).toBe('IA');
  });

  it('pré-fill IA grossesse depuis motifNullitePressenti = MATERNITE_PATERNITE', () => {
    component.aiData = { motifNullitePressenti: 'MATERNITE_PATERNITE' } as any;
    fixture.detectChanges();
    httpMock.expectOne('/api/v1/case-files/cf-1/rupture-periode-essai')
        .flush(null, { status: 404, statusText: 'Not Found' });
    expect(component.grossesseAuMomentRupture()).toBe(true);
    expect(component.provenanceGrossesse()).toBe('IA');
    expect(component.discriminationInvoquee()).toBeNull();
  });

  it('pas de pré-fill IA en BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = { typeContrat: 'CDD', dateEntree: '2024-12-01' } as any;
    fixture.detectChanges();
    expect(component.typeContrat()).toBe('CDI'); // valeur par défaut
    expect(component.dateDebutContrat()).toBeNull();
  });

  it('getPrefillCount static délègue au helper', () => {
    const count = RupturePeriodeEssaiSectionComponent.getPrefillCount({
      aiData: { typeContrat: 'CDI', dateEntree: '2025-01-01' } as any,
      workspaceCountry: 'FRANCE',
    });
    expect(count).toBe(2);
  });

  it('handler onLettreMotiveeChange désactive motifsAveres si décoché', () => {
    component.lettreRuptureMotivee.set(true);
    component.motifsAveresParPieces.set(true);
    component.onLettreMotiveeChange(false);
    expect(component.lettreRuptureMotivee()).toBe(false);
    expect(component.motifsAveresParPieces()).toBe(false);
  });

  it('handler onRenouvellementChange désactive les sous-champs si décoché', () => {
    component.renouvellementInvoque.set(true);
    component.accordBrancheRenouvellement.set(true);
    component.accordEcritSalarieRenouvellement.set(true);
    component.onRenouvellementChange(false);
    expect(component.accordBrancheRenouvellement()).toBe(false);
    expect(component.accordEcritSalarieRenouvellement()).toBe(false);
  });

  it('handler onConventionApplicableChange remet respectee=true si décoché', () => {
    component.conventionCollectiveApplicable.set(true);
    component.conventionCollectivePlusFavorableRespectee.set(false);
    component.onConventionApplicableChange(false);
    expect(component.conventionCollectivePlusFavorableRespectee()).toBe(true);
  });

  it('discrimination AUCUNE → null', () => {
    component.discriminationInvoquee.set('SEXE');
    component.onDiscriminationChange('AUCUNE');
    expect(component.discriminationInvoquee()).toBeNull();
  });

  it('formatEuros affiche correctement', () => {
    const formatted = component.formatEuros(4500);
    expect(formatted).toContain('4');
    expect(formatted).toContain('500');
    expect(formatted).toContain('€');
    expect(component.formatEuros(null)).toBe('—');
  });
});
