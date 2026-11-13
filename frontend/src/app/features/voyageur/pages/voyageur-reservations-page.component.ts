import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { PaiementsApiService } from '../../../core/api/paiements-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { ReservationResponse } from '../../../shared/models/reservation.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-voyageur-reservations-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <section class="page-shell">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="title-gradient ds-section-title">
            {{ language.t('voyageur.reservations.title') }}
          </h1>
          <p class="ds-subtitle">{{ language.t('voyageur.reservations.subtitle') }}</p>
        </div>
        <div class="toolbar-actions">
          <a
            mat-flat-button
            color="primary"
            routerLink="/voyageur/trajets/new"
            [queryParams]="{ type: 'PUBLIC' }"
            >{{ language.t('voyageur.reservations.createPublic') }}</a
          >
          <a
            mat-flat-button
            color="accent"
            routerLink="/voyageur/trajets/new"
            [queryParams]="{ type: 'PRIVE' }"
            >{{ language.t('voyageur.reservations.createPrivate') }}</a
          >
          <a mat-stroked-button routerLink="/voyageur/trajets">{{
            language.t('voyageur.reservations.trajets')
          }}</a>
          <a mat-stroked-button routerLink="/voyageur/payments">{{
            language.t('voyageur.payments.title')
          }}</a>
        </div>
      </div>

      <p class="ui-payment-note">
        {{ language.t('ui.pendingNote') }} {{ language.t('transport.paymentDemo') }}
      </p>
      @if (loading()) {
        <mat-spinner
          [attr.aria-label]="language.t('transport.searching')"
          diameter="32"
        ></mat-spinner>
      }

      @if (message()) {
        <p role="status" aria-live="polite" class="status-success">{{ message() }}</p>
      }

      @if (errorMessage()) {
        <p role="alert" class="status-error">{{ errorMessage() }}</p>
      }

      <mat-card class="card-soft panel-gradient">
        <mat-card-header>
          <mat-card-title>{{ language.t('voyageur.reservations.organizedTitle') }}</mat-card-title>
          <mat-card-subtitle>{{
            language.t('voyageur.reservations.organizedSub')
          }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content class="grid gap-4 md:grid-cols-3">
          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('voyageur.reservations.select') }}</mat-label>
            <mat-select [value]="managedTrajetId()" (valueChange)="onManagedTrajetChange($event)">
              <mat-option [value]="null">{{
                language.t('voyageur.reservations.chooseTrajet')
              }}</mat-option>
              @for (trajet of managedTrajets(); track trajet.id) {
                <mat-option [value]="trajet.id">
                  #{{ trajet.id }} – {{ trajet.date }} {{ trajet.heureDebut }} [{{
                    trajetTypeLabel(trajet.typeTrajet)
                  }}]
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          @if (selectedManagedTrajet()?.typeTrajet === 'PRIVE') {
            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('voyageur.reservations.inviteMatricule') }}</mat-label>
              <input
                matInput
                [formControl]="inviteMatricule"
                [placeholder]="language.t('voyageur.reservations.inviteMatricule')"
              />
            </mat-form-field>

            <div class="flex items-end gap-2">
              <button
                mat-flat-button
                color="primary"
                type="button"
                (click)="addPassager()"
                [disabled]="
                  !selectedManagedTrajet() || inviteMatricule.invalid || actionId() !== null
                "
              >
                {{ language.t('voyageur.reservations.addPassager') }}
              </button>
            </div>
          }

          @if (!managedTrajets().length) {
            <div
              class="md:col-span-3 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900"
            >
              {{ language.t('voyageur.reservations.noOrganized') }}
            </div>
          }

          @if (selectedManagedTrajet()) {
            <div class="md:col-span-3 rounded-lg border border-slate-200 bg-white p-4">
              <p class="mb-3 text-sm font-semibold text-slate-800">
                {{ language.t('voyageur.reservations.edit') }} #{{ selectedManagedTrajet()!.id }}
                <span
                  class="ml-2 ds-badge"
                  [class]="typeBadgeClass(selectedManagedTrajet()!.typeTrajet)"
                >
                  {{ trajetTypeLabel(selectedManagedTrajet()!.typeTrajet) }}
                </span>
              </p>
              <form
                [formGroup]="managedTrajetForm"
                class="grid gap-3 md:grid-cols-4"
                (ngSubmit)="updateManagedTrajet()"
              >
                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('voyageur.public.date') }}</mat-label>
                  <input matInput type="date" formControlName="date" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('voyageur.public.time') }}</mat-label>
                  <input matInput type="time" formControlName="heureDebut" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('voyageur.public.typeLabel') }}</mat-label>
                  <mat-select formControlName="typeTrajet">
                    <mat-option value="PRIVE">{{
                      language.t('voyageur.create.private')
                    }}</mat-option>
                    <mat-option value="PUBLIC">{{
                      language.t('voyageur.create.public')
                    }}</mat-option>
                  </mat-select>
                </mat-form-field>

                <div class="flex items-end gap-2">
                  <button
                    mat-flat-button
                    color="primary"
                    type="submit"
                    [disabled]="managedTrajetForm.invalid || actionId() !== null"
                  >
                    {{ language.t('common.save') }}
                  </button>
                  <button
                    mat-stroked-button
                    color="warn"
                    type="button"
                    (click)="requestDeleteManagedTrajet()"
                    [disabled]="actionId() !== null"
                  >
                    {{ language.t('common.delete') }}
                  </button>
                </div>
              </form>

              @if (pendingDeleteTrajetId() === selectedManagedTrajet()!.id) {
                <div
                  class="mt-3 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-900"
                >
                  <p class="font-semibold">
                    {{ language.t('voyageur.reservations.confirmation') }}
                  </p>
                  <p class="mt-1">{{ language.t('voyageur.reservations.emptyBody') }}</p>
                  <div class="mt-3 flex gap-2">
                    <button
                      mat-flat-button
                      color="warn"
                      type="button"
                      (click)="deleteManagedTrajet()"
                      [disabled]="actionId() !== null"
                    >
                      {{ language.t('common.confirm') }}
                    </button>
                    <button
                      mat-stroked-button
                      type="button"
                      (click)="cancelDeleteManagedTrajet()"
                      [disabled]="actionId() !== null"
                    >
                      {{ language.t('common.cancel') }}
                    </button>
                  </div>
                </div>
              }
            </div>
          }

          <div class="md:col-span-3">
            @if (managedReservations().length) {
              <p class="mb-2 text-sm font-semibold text-slate-700">
                {{ language.t('voyageur.reservations.passagers') }}
              </p>
              <div class="grid gap-2 md:grid-cols-2">
                @for (res of managedReservations(); track res.id) {
                  <div
                    class="rounded-lg border border-slate-200 bg-slate-50 p-3 flex items-center justify-between"
                  >
                    <div>
                      <p class="font-medium text-slate-800">{{ res.voyageurNom }}</p>
                      <p class="mt-1 flex flex-wrap gap-1 text-xs">
                        <span class="ds-badge" [class]="reservationBadgeClass(res.statut)"
                          >{{ language.t('voyageur.reservations.reservationLabel') }}:
                          {{ reservationStatusLabel(res.statut) }}</span
                        >
                        <span class="ds-badge" [class]="paymentBadgeClass(res.paiement?.statut)"
                          >{{ language.t('voyageur.reservations.paymentLabel') }}:
                          {{ paymentStatusLabel(res.paiement?.statut) }}</span
                        >
                      </p>
                    </div>
                    @if (selectedManagedTrajet()?.typeTrajet === 'PRIVE') {
                      <button
                        mat-stroked-button
                        color="warn"
                        type="button"
                        (click)="removePassager(res)"
                        [disabled]="
                          actionId() === res.id ||
                          res.statut === 'ANNULEE' ||
                          res.statut === 'EXPIREE' ||
                          res.voyageurId === voyageurId()
                        "
                      >
                        {{ language.t('voyageur.reservations.removePassager') }}
                      </button>
                    }
                  </div>
                }
              </div>
            } @else if (selectedManagedTrajet()) {
              <p class="text-sm text-slate-500 italic">
                {{ language.t('voyageur.reservations.noPassager') }}
              </p>
            }
          </div>
        </mat-card-content>
      </mat-card>

      <div class="grid gap-4 lg:grid-cols-2">
        @for (reservation of reservations(); track reservation.id) {
          <mat-card class="card-soft">
            <mat-card-header>
              <mat-card-title>
                {{ language.t('voyageur.reservations.reservationTitle') }} #{{
                  reservation.id
                }}</mat-card-title
              >
              <mat-card-subtitle
                >{{ reservation.villeDepart }} → {{ reservation.villeArrivee }} ·
                {{ reservation.trajetDateTime }}</mat-card-subtitle
              >
            </mat-card-header>
            <mat-card-content class="ds-data-list">
              <div class="ds-data-row">
                <span class="ds-data-key">{{
                  language.t('voyageur.reservations.passager')
                }}</span><span class="ds-data-value">{{ reservation.voyageurNom }}</span>
              </div>
              <div class="ds-data-row">
                <span class="ds-data-key">{{
                  language.t('voyageur.reservations.trajet')
                }}</span><span class="ds-data-value">#{{ reservation.trajetId }}</span>
              </div>
              <div class="ds-data-row">
                <span class="ds-data-key">{{
                  language.t('voyageur.reservations.reservation')
                }}</span><span class="ds-data-value"
                  ><span class="ds-badge" [class]="reservationBadgeClass(reservation.statut)">{{
                    reservationStatusLabel(reservation.statut)
                  }}</span></span
                >
              </div>
              <div class="ds-data-row">
                <span class="ds-data-key">{{
                  language.t('voyageur.reservations.payment')
                }}</span><span class="ds-data-value"
                  ><span
                    class="ds-badge"
                    [class]="paymentBadgeClass(reservation.paiement?.statut)"
                    >{{ paymentStatusLabel(reservation.paiement?.statut) }}</span
                  ></span
                >
              </div>
              <div class="ds-data-row">
                <span class="ds-data-key">{{
                  language.t('voyageur.reservations.amount')
                }}</span><span class="ds-data-value"
                  >{{ reservation.paiement?.montant ?? 0 }} €</span
                >
              </div>
            </mat-card-content>
            <mat-card-actions>
              <button
                mat-flat-button
                color="primary"
                type="button"
                (click)="pay(reservation)"
                [disabled]="
                  reservation.statut === 'ANNULEE' ||
                  reservation.paiement?.statut !== 'EN_ATTENTE' ||
                  actionId() === reservation.id
                "
              >
                {{ language.t('voyageur.reservations.pay') }}
              </button>
              <button
                mat-stroked-button
                color="warn"
                type="button"
                (click)="cancel(reservation)"
                [disabled]="
                  reservation.statut === 'ANNULEE' ||
                  reservation.statut === 'EXPIREE' ||
                  actionId() === reservation.id
                "
              >
                {{ language.t('voyageur.reservations.cancel') }}
              </button>
            </mat-card-actions>
          </mat-card>
        } @empty {
          @if (!loading()) {
            <mat-card>
              <mat-card-content class="py-6 text-slate-600">
                <p>{{ language.t('voyageur.payments.none') }}</p>
                <p class="mt-2 text-sm text-slate-400">
                  {{ language.t('voyageur.reservations.emptyBody') }}
                </p>
              </mat-card-content>
            </mat-card>
          }
        }
      </div>
    </section>
  `,
})
export class VoyageurReservationsPage {
  private readonly trajetsApi = inject(TrajetsApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly paiementsApi = inject(PaiementsApiService);
  private readonly voyageurSession = inject(VoyageurSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly actionId = signal<number | null>(null);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly reservations = signal<ReservationResponse[]>([]);
  readonly organisedTrajets = signal<TrajetResponse[]>([]);
  // Ne retourne que les trajets PRIVE pour la gestion des passagers
  readonly managedTrajets = computed(() =>
    this.organisedTrajets().filter((m) => m.typeTrajet === 'PRIVE'),
  );
  readonly selectedManagedTrajet = computed(() => {
    const trajetId = this.managedTrajetId();
    if (!trajetId) {
      return null;
    }
    return this.managedTrajets().find((trajet) => trajet.id === trajetId) ?? null;
  });
  readonly managedReservations = signal<ReservationResponse[]>([]);
  readonly managedTrajetId = signal<number | null>(null);
  readonly pendingDeleteTrajetId = signal<number | null>(null);
  readonly managedTrajetForm = new FormGroup({
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    heureDebut: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    typeTrajet: new FormControl<'PUBLIC' | 'PRIVE'>('PRIVE', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });
  readonly inviteMatricule = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/^([VG]\d{4}|[SLF]\d{5})$/)],
  });
  readonly voyageurId = computed(() => this.voyageurSession.voyageurId());

  constructor() {
    this.loadReservations();
    this.loadOrganisedTrajets();
  }

  loadReservations(): void {
    const voyageurId = this.voyageurId();
    if (!voyageurId) {
      this.errorMessage.set(this.language.t('voyageur.reservations.noVoyageur'));
      return;
    }

    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');
    this.reservationsApi.getByVoyageur(voyageurId).subscribe({
      next: (reservations) => {
        this.reservations.set(reservations);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('voyageur.reservations.loadError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  loadOrganisedTrajets(): void {
    const voyageurId = this.voyageurId();
    if (!voyageurId) {
      return;
    }

    this.trajetsApi.getByOrganisateur(voyageurId).subscribe({
      next: (trajets) => {
        this.organisedTrajets.set(trajets.filter((trajet) => trajet.statut !== 'ANNULE'));
        if (this.managedTrajetId() && !this.selectedManagedTrajet()) {
          this.managedTrajetId.set(null);
          this.managedReservations.set([]);
          this.managedTrajetForm.reset({ date: '', heureDebut: '', typeTrajet: 'PRIVE' });
        } else {
          this.syncManagedTrajetForm();
        }
      },
      error: () => {
        // On garde la page utilisable même si ce chargement échoue.
      },
    });
  }

  onManagedTrajetChange(trajetId: number | null): void {
    // On bloque la sélection d'un trajet PUBLIC
    const trajet = this.organisedTrajets().find((m) => m.id === trajetId) ?? null;
    if (trajet && trajet.typeTrajet !== 'PRIVE') {
      this.errorMessage.set(this.language.t('voyageur.reservations.privateOnly'));
      this.managedTrajetId.set(null);
      this.managedReservations.set([]);
      this.pendingDeleteTrajetId.set(null);
      return;
    }

    this.managedTrajetId.set(trajetId);
    this.managedReservations.set([]);
    this.pendingDeleteTrajetId.set(null);

    if (!trajetId) {
      this.managedTrajetForm.reset({ date: '', heureDebut: '', typeTrajet: 'PRIVE' });
      return;
    }

    this.syncManagedTrajetForm();

    this.reservationsApi.getByTrajet(trajetId).subscribe({
      next: (reservations) => this.managedReservations.set(reservations),
      error: (error) => {
        this.errorMessage.set(
          extractApiErrorMessage(
            error,
            this.language.t('voyageur.reservations.passagersLoadError'),
            (key) => this.language.t(key),
          ),
        );
      },
    });
  }

  updateManagedTrajet(): void {
    const selectedTrajet = this.selectedManagedTrajet();
    const requesterId = this.voyageurId();
    if (
      !selectedTrajet ||
      !requesterId ||
      this.managedTrajetForm.invalid ||
      this.actionId() !== null
    ) {
      return;
    }

    this.actionId.set(selectedTrajet.id);
    this.message.set('');
    this.errorMessage.set('');

    this.trajetsApi
      .update(selectedTrajet.id, {
        vehiculeId: selectedTrajet.vehiculeId,
        organisateurId: requesterId,
        date: this.managedTrajetForm.controls.date.getRawValue(),
        heureDebut: this.managedTrajetForm.controls.heureDebut.getRawValue(),
        typeTrajet: this.managedTrajetForm.controls.typeTrajet.getRawValue(),
      })
      .subscribe({
        next: (updatedTrajet) => {
          this.actionId.set(null);
          this.message.set(this.language.t('voyageur.reservations.updateSuccess'));
          this.organisedTrajets.update((trajets) =>
            trajets.map((trajet) => (trajet.id === updatedTrajet.id ? updatedTrajet : trajet)),
          );

          if (updatedTrajet.typeTrajet !== 'PRIVE') {
            this.managedTrajetId.set(null);
            this.managedReservations.set([]);
            this.managedTrajetForm.reset({ date: '', heureDebut: '', typeTrajet: 'PRIVE' });
          } else {
            this.syncManagedTrajetForm();
            this.onManagedTrajetChange(updatedTrajet.id);
          }
        },
        error: (error) => {
          this.actionId.set(null);
          this.errorMessage.set(
            extractApiErrorMessage(
              error,
              this.language.t('voyageur.reservations.updateError'),
              (key) => this.language.t(key),
            ),
          );
        },
      });
  }

  requestDeleteManagedTrajet(): void {
    const selectedTrajet = this.selectedManagedTrajet();
    if (!selectedTrajet) {
      return;
    }

    this.pendingDeleteTrajetId.set(selectedTrajet.id);
    this.message.set('');
    this.errorMessage.set('');
  }

  cancelDeleteManagedTrajet(): void {
    this.pendingDeleteTrajetId.set(null);
  }

  deleteManagedTrajet(): void {
    const selectedTrajet = this.selectedManagedTrajet();
    const requesterId = this.voyageurId();
    if (!selectedTrajet || !requesterId || this.actionId() !== null) {
      return;
    }
    // Sécurité : on ne supprime qu'un trajet PRIVÉ
    if (selectedTrajet.typeTrajet !== 'PRIVE') {
      this.errorMessage.set(this.language.t('voyageur.reservations.privateOnly'));
      return;
    }
    if (this.pendingDeleteTrajetId() !== selectedTrajet.id) {
      return;
    }

    this.actionId.set(selectedTrajet.id);
    this.message.set('');
    this.errorMessage.set('');

    this.trajetsApi.cancel(selectedTrajet.id, requesterId).subscribe({
      next: () => {
        this.actionId.set(null);
        this.pendingDeleteTrajetId.set(null);
        this.message.set(this.language.t('voyageur.reservations.deleteSuccess'));
        this.managedTrajetId.set(null);
        this.managedReservations.set([]);
        this.managedTrajetForm.reset({ date: '', heureDebut: '', typeTrajet: 'PRIVE' });
        // Recharge la liste pour garantir l'appel au mock dans le test
        this.trajetsApi.getByOrganisateur(requesterId).subscribe({
          next: (trajets) =>
            this.organisedTrajets.set(trajets.filter((m) => m.statut !== 'ANNULE')),
          complete: () => this.loadReservations(),
        });
      },
      error: (error) => {
        this.actionId.set(null);
        this.pendingDeleteTrajetId.set(null);
        this.errorMessage.set(
          extractApiErrorMessage(
            error,
            this.language.t('voyageur.reservations.deleteError'),
            (key) => this.language.t(key),
          ),
        );
      },
    });
  }

  private syncManagedTrajetForm(): void {
    const selectedTrajet = this.selectedManagedTrajet();
    if (!selectedTrajet) {
      return;
    }

    this.managedTrajetForm.patchValue(
      {
        date: selectedTrajet.date,
        heureDebut: selectedTrajet.heureDebut.slice(0, 5),
        typeTrajet: selectedTrajet.typeTrajet,
      },
      { emitEvent: false },
    );
  }

  addPassager(): void {
    const trajetId = this.managedTrajetId();
    if (
      !this.voyageurId() ||
      !trajetId ||
      !this.selectedManagedTrajet() ||
      this.inviteMatricule.invalid ||
      this.actionId() !== null
    ) {
      return;
    }

    const matricule = this.inviteMatricule.getRawValue().trim().toUpperCase();
    this.actionId.set(-1);
    this.message.set('');
    this.errorMessage.set('');

    this.reservationsApi
      .createPrivateInvites({
        trajetId,
        inviteeMatricules: [matricule],
      })
      .subscribe({
        next: () => {
          this.actionId.set(null);
          this.inviteMatricule.setValue('');
          this.message.set(this.language.t('voyageur.reservations.passagerAdded'));
          this.onManagedTrajetChange(trajetId);
          this.loadReservations();
        },
        error: (error) => {
          this.actionId.set(null);
          this.errorMessage.set(
            extractApiErrorMessage(
              error,
              this.language.t('voyageur.reservations.passagerAddError'),
              (key) => this.language.t(key),
            ),
          );
        },
      });
  }

  removePassager(reservation: ReservationResponse): void {
    const trajetId = this.managedTrajetId();
    this.actionId.set(reservation.id);
    this.message.set('');
    this.errorMessage.set('');

    this.reservationsApi.cancel(reservation.id).subscribe({
      next: () => {
        this.actionId.set(null);
        this.message.set(this.language.t('voyageur.reservations.passagerRemoved'));
        if (trajetId) {
          this.onManagedTrajetChange(trajetId);
        }
        this.loadReservations();
      },
      error: (error) => {
        this.actionId.set(null);
        this.errorMessage.set(
          extractApiErrorMessage(
            error,
            this.language.t('voyageur.reservations.passagerRemoveError'),
            (key) => this.language.t(key),
          ),
        );
      },
    });
  }

  pay(reservation: ReservationResponse): void {
    const voyageurId = this.voyageurId();
    if (!voyageurId) {
      return;
    }

    this.actionId.set(reservation.id);
    this.message.set('');
    this.errorMessage.set('');

    this.paiementsApi.pay(reservation.id, voyageurId).subscribe({
      next: () => {
        this.actionId.set(null);
        this.message.set(this.language.t('voyageur.reservations.paymentSuccess'));
        this.loadReservations();
      },
      error: (error) => {
        this.actionId.set(null);
        this.errorMessage.set(
          extractApiErrorMessage(
            error,
            this.language.t('voyageur.reservations.paymentError'),
            (key) => this.language.t(key),
          ),
        );
      },
    });
  }

  cancel(reservation: ReservationResponse): void {
    this.actionId.set(reservation.id);
    this.message.set('');
    this.errorMessage.set('');

    this.reservationsApi.cancel(reservation.id).subscribe({
      next: () => {
        this.actionId.set(null);
        this.message.set(this.language.t('voyageur.reservations.reservationCanceled'));
        this.loadReservations();
      },
      error: (error) => {
        this.actionId.set(null);
        this.errorMessage.set(
          extractApiErrorMessage(
            error,
            this.language.t('voyageur.reservations.reservationCancelError'),
            (key) => this.language.t(key),
          ),
        );
      },
    });
  }

  typeBadgeClass(type: TrajetResponse['typeTrajet']): string {
    return type === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-success';
  }

  trajetTypeLabel(type: TrajetResponse['typeTrajet']): string {
    return type === 'PRIVE'
      ? this.language.t('voyageur.create.private')
      : this.language.t('voyageur.create.public');
  }

  reservationBadgeClass(statut: ReservationResponse['statut']): string {
    if (statut === 'CONFIRMEE') {
      return 'ds-badge-success';
    }
    if (statut === 'EN_ATTENTE') {
      return 'ds-badge-warning';
    }
    return 'ds-badge-danger';
  }

  reservationStatusLabel(statut: ReservationResponse['statut']): string {
    if (statut === 'EXPIREE') return this.language.t('ui.expired');
    if (statut === 'CONFIRMEE') {
      return this.language.t('common.reservationConfirmed');
    }
    if (statut === 'EN_ATTENTE') {
      return this.language.t('common.reservationPending');
    }
    return this.language.t('common.reservationCanceled');
  }

  paymentBadgeClass(statut: string | undefined): string {
    if (statut === 'PAYE') {
      return 'ds-badge-success';
    }
    if (statut === 'EN_ATTENTE') {
      return 'ds-badge-warning';
    }
    if (statut === 'REMBOURSE') {
      return 'ds-badge-info';
    }
    if (statut === 'ANNULE') {
      return 'ds-badge-danger';
    }
    return 'ds-badge-neutral';
  }

  paymentStatusLabel(statut: string | undefined): string {
    if (statut === 'PAYE') {
      return this.language.t('common.paymentPaid');
    }
    if (statut === 'EN_ATTENTE') {
      return this.language.t('common.paymentPending');
    }
    if (statut === 'REMBOURSE') {
      return this.language.t('common.paymentRefunded');
    }
    if (statut === 'ANNULE') {
      return this.language.t('common.paymentCanceled');
    }
    return this.language.t('common.none');
  }
}
