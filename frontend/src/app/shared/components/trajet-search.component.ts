import { LanguageService } from '../../core/i18n/language.service';
import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { TrajetsApiService } from '../../core/api/trajets-api.service';
import { RechercheTrajet, VilleOption } from '../models/trajet.model';

@Component({
  selector: 'app-trajet-search',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
  ],
  template: `
    <form
      [formGroup]="form"
      (ngSubmit)="recherche.emit(form.getRawValue())"
      class="search-form"
      [attr.aria-busy]="chargementVilles()"
      aria-label="{{ language.t('transport.search') }}"
    >
      <mat-form-field appearance="outline"
        ><mat-label>{{ language.t('transport.departure') }}</mat-label>
        <mat-select formControlName="depart"
          ><mat-option value="">{{ language.t('transport.allCities') }}</mat-option>
          @for (ville of villes(); track ville.code) {
            <mat-option [value]="ville.code">{{ ville.libelle }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline"
        ><mat-label>{{ language.t('transport.destination') }}</mat-label>
        <mat-select formControlName="arrivee"
          ><mat-option value="">{{ language.t('transport.allCities') }}</mat-option>
          @for (ville of villes(); track ville.code) {
            <mat-option [value]="ville.code">{{ ville.libelle }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline"
        ><mat-label>{{ language.t('transport.travelDate') }}</mat-label
        ><input matInput type="date" formControlName="date"
      /></mat-form-field>
      <button mat-flat-button type="submit" [disabled]="chargementVilles()">
        {{ language.t('transport.search') }}
      </button>
    </form>
    @if (erreur()) {
      <p role="alert" class="status-error">{{ erreur() }}</p>
    }
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .search-form {
        display: grid;
        grid-template-columns: 1fr 1fr 1fr auto;
        gap: 1rem;
        align-items: start;
      }
      mat-form-field {
        min-width: 0;
      }
      button {
        min-height: 56px;
      }
      @media (max-width: 1000px) {
        .search-form {
          grid-template-columns: 1fr 1fr;
        }
      }
      @media (max-width: 560px) {
        .search-form {
          grid-template-columns: 1fr;
          gap: 0.25rem;
        }
        button {
          width: 100%;
        }
      }
    `,
  ],
})
export class TrajetSearchComponent {
  readonly language = inject(LanguageService);
  @Output() readonly recherche = new EventEmitter<RechercheTrajet>();
  readonly villes = signal<VilleOption[]>([]);
  readonly erreur = signal('');
  readonly chargementVilles = signal(true);
  readonly form = new FormGroup({
    depart: new FormControl('', { nonNullable: true }),
    arrivee: new FormControl('', { nonNullable: true }),
    date: new FormControl('', { nonNullable: true }),
  });
  constructor() {
    inject(TrajetsApiService)
      .getVilles()
      .subscribe({
        next: (villes) => {
          this.villes.set(villes);
          this.chargementVilles.set(false);
        },
        error: () => {
          this.chargementVilles.set(false);
          this.erreur.set(this.language.t('ui.loadCities'));
        },
      });
  }
}
