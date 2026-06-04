/**
 * SF-223-09 — Tests Jest pour `EtatCivilBeModificationSectionComponent`.
 *
 * Vérifie :
 *  - getPrefillCount délègue au helper (F-246 : 0 à 3 champs) ;
 *  - country gate FR → formValid false ;
 *  - type requis ;
 *  - calculate() appelle le service avec le bon body (booleans de fond
 *    uniquement pour la branche concernée ; consentement mineur uniquement si
 *    mineur) ;
 *  - GET 404 → mode formulaire ;
 *  - pré-fill IA (type + majorité + nationalité) depuis aiData ;
 *  - TOOL_LABEL / TOOL_ICON statiques exposés ;
 *  - verdictBannerLabel/Class/Icon mappent les 4 verdicts ;
 *  - aucune citation jurisprudentielle BE forcée (F-JU-04 parké).
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { EtatCivilBeModificationSectionComponent } from './etat-civil-be-modification-section.component';
import { EtatCivilBeModificationService } from '../../core/services/etat-civil-be-modification.service';
import { EtatCivilBeModificationResponse } from '../../core/models/etat-civil-be-modification.model';

describe('EtatCivilBeModificationSectionComponent (SF-223-09)', () => {
  let component: EtatCivilBeModificationSectionComponent;
  let fixture: ComponentFixture<EtatCivilBeModificationSectionComponent>;
  let serviceSpy: jasmine.SpyObj<EtatCivilBeModificationService>;

  const prenomResponse: EtatCivilBeModificationResponse = {
    caseFileId: 'case-1',
    typeModification: 'CHANGEMENT_PRENOM',
    personneMajeure: true,
    nationaliteBelgeOuResident: true,
    motifLegitime: null,
    secondeDemandePrenom: false,
    declarationSexeReiteree: null,
    consentementRepresentantsSiMineur: null,
    verdict: 'MODIFICATION_RECEVABLE',
    autoriteCompetente: "Officier de l'état civil de la commune",
    motifs: ['Le changement de prénom relève de l\'officier de l\'état civil.'],
    conseils: ['Vérifier le tarif de la redevance.'],
    demarches: ['Déposer la demande auprès de l\'officier de l\'état civil.'],
    basesJuridiques: ['Loi du 18/06/2018 — à vérifier par avocat belge'],
    messages: ['Modification recevable (loi du 18/06/2018).'],
    country: 'BELGIQUE',
    calculatedAt: '2026-06-04T10:00:00Z',
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<EtatCivilBeModificationService>(
      'EtatCivilBeModificationService', ['calculate', 'get']);
    spy.get.and.returnValue(throwError(() => ({ status: 404 })));
    spy.calculate.and.returnValue(of(prenomResponse));

    await TestBed.configureTestingModule({
      imports: [EtatCivilBeModificationSectionComponent, NoopAnimationsModule, HttpClientTestingModule],
      providers: [
        { provide: EtatCivilBeModificationService, useValue: spy },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EtatCivilBeModificationSectionComponent);
    component = fixture.componentInstance;
    serviceSpy = TestBed.inject(EtatCivilBeModificationService) as jasmine.SpyObj<EtatCivilBeModificationService>;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
  });

  describe('static metadata', () => {
    it('TOOL_LABEL / TOOL_ICON exposés', () => {
      expect(EtatCivilBeModificationSectionComponent.TOOL_LABEL).toContain('ÉTAT CIVIL');
      expect(EtatCivilBeModificationSectionComponent.TOOL_ICON).toBe('badge');
    });

    it('getPrefillCount délègue au helper (0 sur input vide)', () => {
      expect(EtatCivilBeModificationSectionComponent.getPrefillCount({})).toBe(0);
    });

    it('getPrefillCount compte type + majorité + nationalité (BE)', () => {
      expect(EtatCivilBeModificationSectionComponent.getPrefillCount({
        aiData: {
          etatCivilModificationTypeDetecte: 'CHANGEMENT_NOM',
          etatCivilModificationMajeurDetecte: true,
          etatCivilModificationNationaliteResidentDetectee: true,
        },
        workspaceCountry: 'BELGIQUE',
      })).toBe(3);
    });
  });

  describe('validation formulaire', () => {
    it('country gate FR → formValid false', () => {
      component.workspaceCountry = 'FRANCE';
      fixture.detectChanges();
      component.typeModification.set('CHANGEMENT_PRENOM');
      expect(component.formValid()).toBe(false);
    });

    it('type absent → formValid false', () => {
      fixture.detectChanges();
      expect(component.formValid()).toBe(false);
    });

    it('type renseigné (BE) → formValid true', () => {
      fixture.detectChanges();
      component.typeModification.set('CHANGEMENT_PRENOM');
      expect(component.formValid()).toBe(true);
    });
  });

  describe('calculate()', () => {
    it('prénom majeur : secondeDemandePrenom envoyé, autres branches → null', () => {
      fixture.detectChanges();
      component.typeModification.set('CHANGEMENT_PRENOM');
      component.secondeDemandePrenom.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        typeModification: 'CHANGEMENT_PRENOM',
        secondeDemandePrenom: true,
        motifLegitime: null,
        declarationSexeReiteree: null,
        consentementRepresentantsSiMineur: null,
      }));
      expect(component.result()).toEqual(prenomResponse);
      expect(component.showForm()).toBe(false);
    });

    it('sexe : declarationSexeReiteree envoyé, motif / prénom → null', () => {
      fixture.detectChanges();
      component.typeModification.set('CHANGEMENT_SEXE');
      component.declarationSexeReiteree.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        declarationSexeReiteree: true,
        motifLegitime: null,
        secondeDemandePrenom: null,
      }));
    });

    it('mineur : consentementRepresentantsSiMineur envoyé', () => {
      fixture.detectChanges();
      component.typeModification.set('CHANGEMENT_NOM');
      component.personneMajeure.set(false);
      component.consentementRepresentantsSiMineur.set(true);
      component.calculate();
      expect(serviceSpy.calculate).toHaveBeenCalledWith('case-1', jasmine.objectContaining({
        consentementRepresentantsSiMineur: true,
      }));
    });

    it('formValid false → pas d\'appel HTTP', () => {
      fixture.detectChanges();
      component.calculate();
      expect(serviceSpy.calculate).not.toHaveBeenCalled();
    });
  });

  describe('pré-fill IA', () => {
    it('type + majorité + nationalité pré-remplis depuis aiData (BE)', () => {
      component.aiData = {
        etatCivilModificationTypeDetecte: 'CHANGEMENT_SEXE',
        etatCivilModificationMajeurDetecte: false,
        etatCivilModificationNationaliteResidentDetectee: false,
      } as unknown as never;
      fixture.detectChanges();
      expect(component.typeModification()).toBe('CHANGEMENT_SEXE');
      expect(component.personneMajeure()).toBe(false);
      expect(component.nationaliteBelgeOuResident()).toBe(false);
    });
  });

  describe('GET au chargement', () => {
    it('404 → reste en mode formulaire', () => {
      fixture.detectChanges();
      expect(component.showForm()).toBe(true);
      expect(component.result()).toBeNull();
    });
  });

  describe('helpers d\'affichage', () => {
    it('verdictBannerLabel mappe les 4 verdicts', () => {
      expect(component.verdictBannerLabel('MODIFICATION_RECEVABLE')).toContain('recevable');
      expect(component.verdictBannerLabel('MODIFICATION_RECEVABLE_SOUS_CONDITIONS')).toContain('sous conditions');
      expect(component.verdictBannerLabel('MODIFICATION_IRRECEVABLE')).toContain('irrecevable');
      expect(component.verdictBannerLabel('QUALIFICATION_INCOMPLETE')).toContain('incomplète');
    });

    it('verdictBannerClass : ok / warn / ko / info', () => {
      expect(component.verdictBannerClass('MODIFICATION_RECEVABLE')).toContain('--ok');
      expect(component.verdictBannerClass('MODIFICATION_RECEVABLE_SOUS_CONDITIONS')).toContain('--warn');
      expect(component.verdictBannerClass('MODIFICATION_IRRECEVABLE')).toContain('--ko');
      expect(component.verdictBannerClass('QUALIFICATION_INCOMPLETE')).toContain('--info');
    });

    it('toolIdForJurisprudence = etat-civil-be-modification (BE parké, pas de citation forcée)', () => {
      expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
        .toBe('etat-civil-be-modification');
    });
  });
});
