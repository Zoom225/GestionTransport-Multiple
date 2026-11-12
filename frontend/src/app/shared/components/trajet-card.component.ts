import { LanguageService } from '../../core/i18n/language.service';
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TrajetDetailComponent } from './trajet-detail.component';
import { MatButtonModule } from '@angular/material/button';
import { TrajetResponse } from '../models/trajet.model';

@Component({
  selector: 'app-trajet-card',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatDialogModule],
  template: `
    <article class="trip-card" [attr.data-trajet-id]="trajet.id">
      <div class="trip-top">
        <span class="type">{{ trajet.typeTrajet }}</span
        ><span>{{ trajet.date | date: 'dd/MM/yyyy' }} · {{ trajet.heureDebut.slice(0, 5) }}</span>
      </div>
      <h3>
        {{ nomVille(trajet.villeDepart) }} <span aria-label="vers">→</span>
        {{ nomVille(trajet.villeArrivee) }}
      </h3>
      <p class="driver">
        {{ language.t('transport.driver') }} ·
        <strong>{{ trajet.chauffeurNom || trajet.organisateurNom }}</strong>
      </p>
      <p>{{ trajet.vehiculeNom }} · {{ trajet.agenceNom }}</p>
      <div class="trip-bottom">
        <span
          >{{ restantes() }} {{ language.t('transport.remaining') }}
          {{ trajet.placesMaximum ?? 4 }}</span
        ><strong
          >{{ trajet.prixParPassager | currency: 'EUR' : 'symbol' : '1.0-2' }}
          <small>{{ language.t('transport.perPerson') }}</small></strong
        >
      </div>
      <div class="actions">
        <button
          mat-flat-button
          type="button"
          (click)="reservationDemandee.emit(trajet)"
          [disabled]="disabled || restantes() === 0 || trajet.statut !== 'PLANIFIE'"
        >
          {{
            restantes() === 0
              ? language.t('transport.full')
              : labelAction || language.t('transport.reserve')
          }}</button
        ><button mat-stroked-button type="button" (click)="openDetails()">{{ language.t('ui.details') }}</button><ng-content />
      </div>
    </article>
  `,
  styles: [`:host { display: block; min-width: 0; height: 100%; }
.trip-card { display: flex; flex-direction: column; height: 100%; padding: 1.5rem; background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius); box-shadow: var(--ui-shadow); transition: box-shadow .2s, border-color .2s; }
.trip-card:hover { box-shadow: var(--ui-shadow-raised); border-color: var(--ui-primary); }
.trip-top, .trip-bottom, .actions { display: flex; justify-content: space-between; align-items: center; gap: .75rem; flex-wrap: wrap; }
.trip-top { font-size: .78rem; color: var(--ui-text-secondary); }
.type { padding: .25rem .6rem; font-size: .7rem; font-weight: 750; border-radius: 7px; background: var(--ui-primary-soft); color: var(--ui-primary-strong); }
h3 { font-size: 1.4rem; color: var(--ui-navy); margin: 1.5rem 0 1.25rem; line-height: 1.4; }
h3 span { color: var(--ui-teal-strong); padding-inline: .2rem; }
p { font-size: .85rem; color: var(--ui-text-secondary); margin: .25rem 0; }
.driver strong { color: var(--ui-navy); font-weight: 650; }
.trip-bottom { border-top: 1px solid var(--ui-border); margin-top: 1.5rem; padding-top: 1.25rem; font-size: .8rem; }
.trip-bottom strong { color: var(--ui-navy); font-size: 1.65rem; font-weight: 750; }
.trip-bottom small { display: block; text-align: right; font-size: .7rem; font-weight: 500; color: var(--ui-text-secondary); }
.actions { margin-top: auto; padding-top: 1.25rem; justify-content: flex-start; }
.actions button { flex: 1 1 auto; }
`],
})
export class TrajetCardComponent {
  readonly language = inject(LanguageService);
  private readonly dialog = inject(MatDialog);
  openDetails(): void {
    this.dialog.open(TrajetDetailComponent, {
      width: '680px', maxWidth: 'calc(100vw - 32px)', autoFocus: 'first-heading',
      data: { trajet: this.trajet, disabled: this.disabled || this.restantes() === 0 || this.trajet.statut !== 'PLANIFIE' }
    }).afterClosed().subscribe(reserver => { if (reserver === true) this.reservationDemandee.emit(this.trajet); });
  }
  @Input({ required: true }) trajet!: TrajetResponse;
  @Input() disabled = false;
  @Input() labelAction = '';
  @Output() readonly reservationDemandee = new EventEmitter<TrajetResponse>();
  restantes(): number {
    return this.trajet.placesRestantes ?? Math.max(0, 4 - this.trajet.nbPassagersActuels);
  }
  nomVille(code?: string | null): string {
    return code
      ? (code.charAt(0) + code.slice(1).toLowerCase()).replace('Liege', 'Liège')
      : this.language.t('ui.cityMissing');
  }
}
