import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { LanguageService } from '../../core/i18n/language.service';
import { TrajetResponse } from '../models/trajet.model';

@Component({
  selector: 'app-trajet-detail',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>
      {{ ville(data.trajet.villeDepart) }} → {{ ville(data.trajet.villeArrivee) }}
    </h2>
    <mat-dialog-content>
      <p class="ui-eyebrow">{{ language.t('ui.tripOverview') }}</p>
      <dl class="detail-grid">
        <div>
          <dt>{{ language.t('transport.travelDate') }}</dt>
          <dd>{{ data.trajet.date | date: 'dd/MM/yyyy' }}</dd>
        </div>
        <div>
          <dt>{{ language.t('voyageur.public.time') }}</dt>
          <dd>{{ data.trajet.heureDebut.slice(0, 5) }} – {{ data.trajet.heureFin.slice(0, 5) }}</dd>
        </div>
        <div>
          <dt>{{ language.t('transport.driver') }}</dt>
          <dd>{{ data.trajet.chauffeurNom || data.trajet.organisateurNom }}</dd>
        </div>
        <div>
          <dt>{{ language.t('ui.vehicle') }}</dt>
          <dd>{{ data.trajet.vehiculeNom }}</dd>
        </div>
        <div>
          <dt>{{ language.t('ui.availability') }}</dt>
          <dd>
            {{ data.trajet.placesRestantes ?? 4 - data.trajet.nbPassagersActuels }} /
            {{ data.trajet.placesMaximum ?? 4 }}
          </dd>
        </div>
        <div>
          <dt>{{ language.t('voyageur.public.typeLabel') }}</dt>
          <dd>
            {{
              data.trajet.typeTrajet === 'PUBLIC'
                ? language.t('voyageur.create.public')
                : language.t('voyageur.create.private')
            }}
          </dd>
        </div>
        <div>
          <dt>{{ language.t('admin.trajets.status') }}</dt>
          <dd>
            {{
              data.trajet.statut === 'PLANIFIE'
                ? language.t('common.trajetPlanned')
                : data.trajet.statut === 'COMPLET'
                  ? language.t('common.trajetFull')
                  : language.t('common.trajetCanceled')
            }}
          </dd>
        </div>
      </dl>
      <div class="detail-price">
        <span>{{ language.t('ui.unitPrice') }}</span><strong>{{
          data.trajet.prixParPassager | currency: 'EUR'
        }}</strong>
      </div>
      <p class="ui-payment-note">
        {{ language.t('ui.pendingNote') }} {{ language.t('transport.paymentDemo') }}
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>{{ language.t('ui.close') }}</button>
      <button mat-flat-button [mat-dialog-close]="true" [disabled]="data.disabled">
        {{ language.t('transport.reserve') }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .detail-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 1.25rem;
        margin: 1.5rem 0;
      }
      dt {
        color: var(--ui-text-secondary);
        font-size: 0.8rem;
        margin-bottom: 0.35rem;
      }
      dd {
        margin: 0;
        font-weight: 650;
        color: var(--ui-navy);
        overflow-wrap: anywhere;
      }
      .detail-price {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid var(--ui-border);
        padding: 1.25rem 0;
        color: var(--ui-text);
      }
      .detail-price strong {
        font-size: 1.75rem;
      }
      @media (max-width: 480px) {
        .detail-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class TrajetDetailComponent {
  readonly language = inject(LanguageService);
  readonly data = inject<{ trajet: TrajetResponse; disabled: boolean }>(MAT_DIALOG_DATA);
  ville(code?: string | null): string {
    return code
      ? (code.charAt(0) + code.slice(1).toLowerCase()).replace('Liege', 'Liège')
      : this.language.t('ui.cityMissing');
  }
}
