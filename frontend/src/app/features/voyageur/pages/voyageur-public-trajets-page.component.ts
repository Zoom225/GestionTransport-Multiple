import { TrajetSearchComponent } from '../../../shared/components/trajet-search.component';
import { TrajetCardComponent } from '../../../shared/components/trajet-card.component';
import { RechercheTrajet } from '../../../shared/models/trajet.model';
import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { AgenceResponse } from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-voyageur-public-trajets-page',
  standalone: true,
  imports: [
    CommonModule,
    TrajetSearchComponent,
    TrajetCardComponent,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <section class="page-shell">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="title-gradient ds-section-title">{{ language.t('voyageur.public.title') }}</h1>
          <p class="ds-subtitle">{{ language.t('voyageur.public.subtitle') }}</p>
        </div>
        <div class="toolbar-actions">
          <a mat-stroked-button routerLink="/voyageur/profile">{{
            language.t('voyageur.public.myProfile')
          }}</a>
          <a mat-flat-button color="primary" routerLink="/voyageur/trajets/new">{{
            language.t('voyageur.public.create')
          }}</a>
        </div>
      </div>

      <app-trajet-search (recherche)="rechercher($event)" />
      <mat-card class="card-soft">
        <mat-card-content class="grid gap-4 pt-4 md:grid-cols-3">
          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('voyageur.public.filter') }}</mat-label>
            <mat-select [value]="selectedAgenceId()" (valueChange)="selectedAgenceId.set($event)">
              <mat-option [value]="0">{{ language.t('common.allAgences') }}</mat-option>
              @for (agence of agences(); track agence.id) {
                <mat-option [value]="agence.id">{{ agence.nom }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="md:col-span-2">
            <mat-label>{{ language.t('voyageur.public.search') }}</mat-label>
            <input
              matInput
              [value]="search()"
              (input)="search.set($any($event.target).value)"
              [placeholder]="language.t('voyageur.public.searchPlaceholder')"
            />
          </mat-form-field>
        </mat-card-content>
      </mat-card>

      @if (loading()) {
        <div class="py-4">
          <mat-spinner
            [attr.aria-label]="language.t('transport.searching')"
            diameter="32"
          ></mat-spinner>
        </div>
      }

      @if (message()) {
        <p role="status" aria-live="polite" class="status-success">{{ message() }}</p>
      }

      @if (errorMessage()) {
        <p role="alert" class="status-error">{{ errorMessage() }}</p>
      }

      <a
        routerLink="/voyageur/trajets/new"
        class="card-soft block rounded-2xl border border-sky-200 bg-gradient-to-r from-sky-50 to-indigo-50 p-5 no-underline transition hover:-translate-y-0.5 hover:shadow-md"
      >
        <p class="text-sm font-medium uppercase tracking-wide text-sky-700">
          {{ language.t('voyageur.public.noTrajetPrompt') }}
        </p>
        <div class="mt-2 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p class="text-xl font-semibold text-slate-900">
              {{ language.t('voyageur.public.create') }}
            </p>
            <p class="text-sm text-slate-600">{{ language.t('voyageur.public.createBody') }}</p>
          </div>
          <span class="ds-btn-primary">{{ language.t('voyageur.public.create') }}</span>
        </div>
      </a>

      <div class="grid gap-4 lg:grid-cols-2">
        @for (trajet of filteredTrajets(); track trajet.id) {
          <mat-card class="card-soft">
            <app-trajet-card
              [trajet]="trajet"
              [disabled]="joiningTrajetId() === trajet.id || !canJoin(trajet)"
              (reservationDemandee)="joinTrajet($event)"
            >
              @if (isOrganizer(trajet)) {
                <span class="ds-badge ds-badge-info">{{
                  language.t('voyageur.public.myTrajet')
                }}</span>
                <button
                  mat-stroked-button
                  type="button"
                  (click)="startEdit(trajet)"
                  [disabled]="!canModify(trajet) || actionTrajetId() === trajet.id"
                >
                  {{ language.t('voyageur.public.modify') }}
                </button>
                <button
                  mat-stroked-button
                  type="button"
                  (click)="cancelOwnTrajet(trajet)"
                  [disabled]="actionTrajetId() === trajet.id"
                >
                  {{ language.t('voyageur.public.delete') }}
                </button>
              }
            </app-trajet-card>
            @if (isOrganizer(trajet) && !canModify(trajet)) {
              <p class="px-4 pb-3 text-xs text-amber-700">
                {{ language.t('voyageur.public.editHint') }}
              </p>
            }

            @if (editingTrajetId() === trajet.id) {
              <mat-card-content class="grid gap-3 border-t border-slate-100 pt-3 md:grid-cols-4">
                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('voyageur.public.date') }}</mat-label>
                  <input matInput type="date" [formControl]="editForm.controls.date" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('voyageur.public.time') }}</mat-label>
                  <input matInput type="time" [formControl]="editForm.controls.heureDebut" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('voyageur.public.typeLabel') }}</mat-label>
                  <mat-select [formControl]="editForm.controls.typeTrajet">
                    <mat-option value="PUBLIC">{{
                      language.t('voyageur.create.public')
                    }}</mat-option>
                    <mat-option value="PRIVE">{{
                      language.t('voyageur.create.private')
                    }}</mat-option>
                  </mat-select>
                </mat-form-field>

                <div class="flex items-end gap-2">
                  <button
                    mat-flat-button
                    color="primary"
                    type="button"
                    (click)="saveEdit(trajet)"
                    [disabled]="editForm.invalid || actionTrajetId() === trajet.id"
                  >
                    {{ language.t('common.save') }}
                  </button>
                  <button
                    mat-stroked-button
                    type="button"
                    (click)="cancelEdit()"
                    [disabled]="actionTrajetId() === trajet.id"
                  >
                    {{ language.t('common.back') }}
                  </button>
                </div>
              </mat-card-content>
            }
          </mat-card>
        } @empty {
          @if (!loading()) {
            <mat-card>
              <mat-card-content class="py-6 text-slate-600">
                {{ language.t('voyageur.public.noTrajets') }}
              </mat-card-content>
            </mat-card>
          }
        }
      </div>
    </section>
  `,
})
export class VoyageurPublicTrajetsPage {
  private readonly trajetsApi = inject(TrajetsApiService);
  private readonly agencesApi = inject(AgencesApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly voyageurSession = inject(VoyageurSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly joiningTrajetId = signal<number | null>(null);
  readonly actionTrajetId = signal<number | null>(null);
  readonly editingTrajetId = signal<number | null>(null);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly trajets = signal<TrajetResponse[]>([]);
  readonly agences = signal<AgenceResponse[]>([]);
  readonly selectedAgenceId = signal<number>(0);
  readonly search = signal('');
  readonly editForm = new FormGroup({
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    heureDebut: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    typeTrajet: new FormControl<'PUBLIC' | 'PRIVE'>('PUBLIC', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  readonly filteredTrajets = computed(() => {
    const agenceId = this.selectedAgenceId();
    const search = this.search().trim().toLowerCase();

    return this.trajets()
      .filter((trajet) => {
        const trajetsAgence = !agenceId || trajet.agenceId === agenceId;
        const haystack =
          `${trajet.vehiculeNom} ${trajet.agenceNom} ${trajet.organisateurNom} ${trajet.villeDepart} ${trajet.villeArrivee}`.toLowerCase();
        const trajetsSearch = !search || haystack.includes(search);
        return trajetsAgence && trajetsSearch;
      })
      .sort((a, b) => {
        const scoreA = this.isOrganizer(a) ? 0 : 1;
        const scoreB = this.isOrganizer(b) ? 0 : 1;
        return scoreA - scoreB;
      });
  });

  constructor() {
    this.loadData();
  }

  private filtres: RechercheTrajet = {};
  rechercher(filtres: RechercheTrajet): void {
    this.filtres = filtres;
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');

    forkJoin({
      trajets: this.trajetsApi.getPublic(this.filtres),
      agences: this.agencesApi.getAll(),
    }).subscribe({
      next: ({ trajets, agences }) => {
        this.trajets.set(trajets);
        this.agences.set(agences);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('voyageur.public.loadError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  joinTrajet(trajet: TrajetResponse): void {
    const voyageurId = this.voyageurSession.voyageurId();
    if (!voyageurId) {
      this.errorMessage.set(this.language.t('voyageur.public.noVoyageur'));
      return;
    }

    if (!this.canJoin(trajet)) {
      this.errorMessage.set(this.language.t('voyageur.public.joinBlocked'));
      return;
    }

    this.joiningTrajetId.set(trajet.id);
    this.message.set('');
    this.errorMessage.set('');

    this.reservationsApi
      .create({
        trajetId: trajet.id,
        voyageurId: voyageurId,
        requesterId: voyageurId,
      })
      .subscribe({
        next: () => {
          this.joiningTrajetId.set(null);
          this.message.set(this.language.t('voyageur.public.joinSuccess'));
        },
        error: (error) => {
          this.joiningTrajetId.set(null);
          this.errorMessage.set(
            extractApiErrorMessage(error, this.language.t('voyageur.public.joinError'), (key) =>
              this.language.t(key),
            ),
          );
        },
      });
  }

  isOrganizer(trajet: TrajetResponse): boolean {
    return trajet.organisateurId === this.voyageurSession.voyageurId();
  }

  canJoin(trajet: TrajetResponse): boolean {
    if (this.isOrganizer(trajet)) {
      return false;
    }

    return trajet.statut === 'PLANIFIE' && trajet.nbPassagersActuels < 4;
  }

  canModify(trajet: TrajetResponse): boolean {
    return this.isOrganizer(trajet) && this.startsInMoreThan24Hours(trajet);
  }

  statusBadgeClass(statut: TrajetResponse['statut']): string {
    if (statut === 'PLANIFIE') {
      return 'ds-badge-success';
    }
    if (statut === 'COMPLET') {
      return 'ds-badge-warning';
    }
    return 'ds-badge-danger';
  }

  typeBadgeClass(type: TrajetResponse['typeTrajet']): string {
    return type === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-neutral';
  }

  typeLabel(type: TrajetResponse['typeTrajet']): string {
    return type === 'PRIVE'
      ? this.language.t('voyageur.create.private')
      : this.language.t('voyageur.create.public');
  }

  trajetStatusLabel(statut: TrajetResponse['statut']): string {
    if (statut === 'PLANIFIE') {
      return this.language.t('common.trajetPlanned');
    }
    if (statut === 'COMPLET') {
      return this.language.t('common.trajetFull');
    }
    return this.language.t('common.trajetCanceled');
  }

  startEdit(trajet: TrajetResponse): void {
    if (!this.canModify(trajet)) {
      this.errorMessage.set(this.language.t('voyageur.public.modifyTooLate'));
      return;
    }

    this.editingTrajetId.set(trajet.id);
    this.editForm.patchValue({
      date: trajet.date,
      heureDebut: trajet.heureDebut.slice(0, 5),
      typeTrajet: trajet.typeTrajet,
    });
  }

  cancelEdit(): void {
    this.editingTrajetId.set(null);
  }

  saveEdit(trajet: TrajetResponse): void {
    const voyageurId = this.voyageurSession.voyageurId();
    if (!voyageurId || this.editForm.invalid) {
      return;
    }

    this.actionTrajetId.set(trajet.id);
    this.message.set('');
    this.errorMessage.set('');

    this.trajetsApi
      .update(trajet.id, {
        vehiculeId: trajet.vehiculeId,
        organisateurId: voyageurId,
        date: this.editForm.controls.date.getRawValue(),
        heureDebut: this.editForm.controls.heureDebut.getRawValue(),
        typeTrajet: this.editForm.controls.typeTrajet.getRawValue(),
      })
      .subscribe({
        next: () => {
          this.actionTrajetId.set(null);
          this.editingTrajetId.set(null);
          this.message.set(this.language.t('voyageur.public.updateSuccess'));
          this.loadData();
        },
        error: (error) => {
          this.actionTrajetId.set(null);
          this.errorMessage.set(
            extractApiErrorMessage(error, this.language.t('voyageur.public.updateError'), (key) =>
              this.language.t(key),
            ),
          );
        },
      });
  }

  cancelOwnTrajet(trajet: TrajetResponse): void {
    const voyageurId = this.voyageurSession.voyageurId();
    if (!voyageurId) {
      return;
    }

    if (!confirm(this.language.t('voyageur.public.deleteConfirm', { id: trajet.id }))) {
      return;
    }

    this.actionTrajetId.set(trajet.id);
    this.message.set('');
    this.errorMessage.set('');

    this.trajetsApi.cancel(trajet.id, voyageurId).subscribe({
      next: () => {
        this.actionTrajetId.set(null);
        this.editingTrajetId.set(null);
        this.message.set(this.language.t('voyageur.public.deleteSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.actionTrajetId.set(null);
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('voyageur.public.deleteError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  private startsInMoreThan24Hours(trajet: TrajetResponse): boolean {
    const startDateTime = new Date(`${trajet.date}T${trajet.heureDebut}`);
    return startDateTime.getTime() - Date.now() >= 24 * 60 * 60 * 1000;
  }
}
