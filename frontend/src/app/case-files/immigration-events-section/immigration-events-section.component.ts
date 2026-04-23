import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatExpansionModule } from '@angular/material/expansion';
import { DatePipe } from '@angular/common';
import { ImmigrationTriggerEvent } from '../../core/models/case-analysis.model';

/**
 * F-150 SF-150-02 : carte "Événements détectés" affichée dans la synthèse
 * d'un dossier immigration. Pour chaque événement factuel identifié par l'IA
 * (mariage, doctorat, CDI...), affiche la base légale CESEDA et le titre
 * de séjour suggéré.
 */
@Component({
  selector: 'app-immigration-events-section',
  standalone: true,
  imports: [DatePipe, MatIconModule, MatTooltipModule, MatExpansionModule],
  templateUrl: './immigration-events-section.component.html',
  styleUrl: './immigration-events-section.component.scss',
})
export class ImmigrationEventsSectionComponent {
  @Input() events: ImmigrationTriggerEvent[] = [];

  iconFor(eventCode: string): string {
    switch (eventCode) {
      case 'MARIAGE_RESSORTISSANT_FR':
      case 'PACS_RESSORTISSANT_FR':
        return 'favorite';
      case 'NAISSANCE_ENFANT_FR':
        return 'child_friendly';
      case 'DOCTORAT_OBTENU':
        return 'school';
      case 'CDI_OBTENU_SALARIE':
        return 'work';
      case 'ENTREE_LEGALE_10ANS':
        return 'schedule';
      case 'VIOLENCES_CONJUGALES_CONSTATEES':
        return 'shield';
      case 'DEMANDE_ASILE_ACCORDEE_OFPRA':
        return 'verified_user';
      case 'ENFANT_NE_FR_13ANS_PRESENCE':
        return 'family_restroom';
      case 'REGROUPEMENT_FAMILIAL_AUTORISE':
        return 'groups';
      default:
        return 'new_releases';
    }
  }
}
