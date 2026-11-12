import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { TrajetsApiService } from '../../core/api/trajets-api.service';
import { RechercheTrajet, VilleOption } from '../models/trajet.model';

@Component({
  selector: 'app-trajet-search', standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatSelectModule, MatInputModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="recherche.emit(form.getRawValue())" class="search-form" aria-label="Rechercher un trajet">
      <mat-form-field appearance="outline"><mat-label>Ville de départ</mat-label>
        <mat-select formControlName="depart"><mat-option value="">Toutes les villes</mat-option>
          @for (ville of villes(); track ville.code) { <mat-option [value]="ville.code">{{ ville.libelle }}</mat-option> }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Destination</mat-label>
        <mat-select formControlName="arrivee"><mat-option value="">Toutes les villes</mat-option>
          @for (ville of villes(); track ville.code) { <mat-option [value]="ville.code">{{ ville.libelle }}</mat-option> }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Date de voyage</mat-label><input matInput type="date" formControlName="date" /></mat-form-field>
      <button mat-flat-button type="submit">Rechercher un trajet</button>
    </form>
    @if (erreur()) { <p role="alert" class="status-error">{{ erreur() }}</p> }
  `,
  styles: [`:host{display:block} .search-form{display:grid;grid-template-columns:1fr 1fr 1fr auto;gap:1rem;align-items:start} mat-form-field{min-width:0} button{min-height:56px} @media(max-width:1000px){.search-form{grid-template-columns:1fr 1fr}} @media(max-width:560px){.search-form{grid-template-columns:1fr;gap:.25rem}button{width:100%}}`]
})
export class TrajetSearchComponent {
  @Output() readonly recherche = new EventEmitter<RechercheTrajet>();
  readonly villes = signal<VilleOption[]>([]);
  readonly erreur = signal('');
  readonly form = new FormGroup({depart: new FormControl('', {nonNullable:true}), arrivee: new FormControl('', {nonNullable:true}), date: new FormControl('', {nonNullable:true})});
  constructor() {
    inject(TrajetsApiService).getVilles().subscribe({next: villes => this.villes.set(villes), error: () => this.erreur.set('Les villes ne peuvent pas être chargées. Réessayez dans quelques instants.')});
  }
}
