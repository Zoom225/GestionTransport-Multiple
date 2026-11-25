import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { ReservationResponse } from '../../../shared/models/reservation.model';
import { AgenceResponse } from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-admin-trajets-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    ReactiveFormsModule,
  ],
  template: `
    <!-- En-tête trajets -->
    <div class="adm-trajet-header">
      <div class="adm-trajet-header-inner">
        <div class="adm-trajet-title-block">
          <span class="adm-trajet-icon" aria-hidden="true"
            ><svg class="ui-icon"><use href="images/ui-icons.svg#car" /></svg
          ></span>
          <div>
            <h1 class="adm-trajet-title">{{ language.t('admin.trajets.title') }}</h1>
            <p class="adm-trajet-sub">{{ language.t('admin.trajets.subtitle') }}</p>
          </div>
        </div>
        <a routerLink="/admin" class="adm-trajet-back-btn">{{ language.t('common.back') }}</a>
      </div>
    </div>

    <section class="page-shell">
      <a mat-flat-button routerLink="/admin/trajets/new">Créer un trajet avec chauffeur</a>
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

      <div class="grid gap-5 lg:grid-cols-2">
        @for (trajet of filteredTrajets(); track trajet.id) {
          <mat-card class="adm-trajet-card">
            <mat-card-header class="adm-trajet-card-header">
              <div class="adm-trajet-card-title-row">
                <div>
                  <mat-card-title class="adm-trajet-card-title">{{
                    trajet.vehiculeNom
                  }}</mat-card-title>
                  <mat-card-subtitle class="adm-trajet-card-sub">
                    {{ trajet.agenceNom }}</mat-card-subtitle
                  >
                </div>
                <span class="ds-badge" [class]="typeBadgeClass(trajet.typeTrajet)">{{
                  typeLabel(trajet.typeTrajet)
                }}</span>
              </div>
              <div class="adm-trajet-date-row">
                <span> {{ trajet.date }}</span>
                <span> {{ trajet.heureDebut }} – {{ trajet.heureFin }}</span>
              </div>
            </mat-card-header>

            <mat-card-content class="adm-trajet-content">
              <div class="adm-trajet-info-grid">
                <div class="adm-info-item">
                  <span class="adm-info-label">{{ language.t('admin.trajets.organizer') }}</span>
                  <span class="adm-info-value">{{ trajet.organisateurNom }}</span>
                </div>
                <div class="adm-info-item">
                  <span class="adm-info-label">{{ language.t('admin.trajets.status') }}</span>
                  <span class="ds-badge" [class]="statusBadgeClass(trajet.statut)">{{
                    trajetStatusLabel(trajet.statut)
                  }}</span>
                </div>
                <div class="adm-info-item adm-passagers-item">
                  <span class="adm-info-label">{{ language.t('admin.trajets.passagers') }}</span>
                  <div class="adm-passagers-bar">
                    <div
                      class="adm-passagers-fill"
                      [style.width.%]="(trajet.nbPassagersActuels / 4) * 100"
                    >
                      <span>{{ trajet.nbPassagersActuels }}/4</span>
                    </div>
                  </div>
                </div>
              </div>

              @if (selectedTrajetId() === trajet.id) {
                <div class="adm-reservations-panel">
                  <p class="adm-reservations-title">
                    {{ language.t('admin.trajets.reservations') }}
                  </p>
                  @for (reservation of selectedReservations(); track reservation.id) {
                    <div class="adm-reservation-item flex-wrap">
                      <span class="font-medium">{{ reservation.voyageurNom }}</span>
                      @if (reservation.absenceDeclareeLe) {
                        <p>
                          {{ language.t('ui.absenceReported') }}
                          {{ reservation.absenceDeclareeLe | date: 'dd/MM/yyyy HH:mm' }} :
                          {{ reservation.motifAbsence }}
                        </p>
                      } @else if (peutDeclarerAbsence(reservation, trajet)) {
                        <button
                          mat-button
                          type="button"
                          (click)="absenceSelectionnee.set(reservation.id)"
                        >
                          {{ language.t('ui.reportAbsence') }}
                        </button>
                        @if (absenceSelectionnee() === reservation.id) {
                          <form
                            class="grid gap-2 min-w-0 w-full"
                            (submit)="$event.preventDefault(); declarerAbsence(reservation)"
                          >
                            <label [for]="'absence-' + reservation.id">{{
                              language.t('ui.absenceReason')
                            }}</label>
                            <textarea
                              class="w-full min-w-0 border rounded p-2"
                              [id]="'absence-' + reservation.id"
                              [formControl]="motifAbsence"
                              maxlength="500"
                              rows="3"
                            ></textarea>
                            <button
                              mat-flat-button
                              type="submit"
                              [disabled]="motifAbsence.invalid || absenceEnCours()"
                            >
                              {{ language.t('ui.saveReport') }}
                            </button>
                          </form>
                        }
                      }
                      <div class="flex gap-2">
                        <span
                          class="ds-badge"
                          [class]="reservationBadgeClass(reservation.statut)"
                          >{{ reservationStatusLabel(reservation.statut) }}</span
                        >
                        <span
                          class="ds-badge"
                          [class]="paymentBadgeClass(reservation.paiement?.statut)"
                          >{{ paymentStatusLabel(reservation.paiement?.statut) }}</span
                        >
                      </div>
                    </div>
                  } @empty {
                    <p class="text-sm text-slate-500 text-center py-2">
                      {{ language.t('admin.trajets.noReservations') }}
                    </p>
                  }
                </div>
              }
            </mat-card-content>

            <mat-card-actions class="adm-trajet-actions">
              <button
                class="adm-trajet-btn-details"
                type="button"
                (click)="showReservations(trajet)"
              >
                {{
                  selectedTrajetId() === trajet.id
                    ? language.t('admin.trajets.hideDetails')
                    : language.t('admin.trajets.showDetails')
                }}
              </button>
              <button
                class="adm-trajet-btn-convert"
                type="button"
                (click)="convertToPublic(trajet.id)"
                [disabled]="trajet.typeTrajet !== 'PRIVE'"
              >
                {{ language.t('admin.trajets.convert') }}
              </button>
            </mat-card-actions>
          </mat-card>
        } @empty {
          <div class="adm-trajet-empty lg:col-span-2">
            <span></span>
            <p>{{ language.t('admin.trajets.empty') }}</p>
          </div>
        }
      </div>
    </section>

    <style>
      .adm-trajet-header {
        background: var(--ui-navy);
        padding: 1.5rem 2rem;
        box-shadow: var(--ui-shadow);
        color: var(--ui-surface);
      }
      .adm-trajet-header-inner {
        display: flex;
        align-items: center;
        justify-content: space-between;
        max-width: 1200px;
        margin: 0 auto;
        gap: 1rem;
      }
      .adm-trajet-title-block {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .adm-trajet-icon {
        font-size: 2.5rem;
      }
      .adm-trajet-title {
        font-size: 1.6rem;
        font-weight: 800;
        color: var(--ui-surface);
        margin: 0;
      }
      .adm-trajet-sub {
        color: color-mix(in srgb, var(--ui-surface) 75%, transparent);
        font-size: 0.85rem;
        margin: 0;
      }
      .adm-trajet-back-btn {
        background: color-mix(in srgb, var(--ui-surface) 15%, transparent);
        color: var(--ui-surface);
        border: 2px solid color-mix(in srgb, var(--ui-surface) 35%, transparent);
        border-radius: 9999px;
        padding: 0.45rem 1.1rem;
        font-weight: 700;
        font-size: 0.85rem;
        text-decoration: none;
        transition: background 0.15s;
        white-space: nowrap;
      }
      .adm-trajet-back-btn:hover {
        background: color-mix(in srgb, var(--ui-surface) 25%, transparent);
      }

      .adm-trajet-card {
        border-radius: 1.25rem !important;
        border: 1px solid color-mix(in srgb, var(--ui-warning) 15%, transparent) !important;
        overflow: hidden;
        border-top: 4px solid var(--ui-primary);
        transition:
          transform 0.15s,
          box-shadow 0.15s;
      }
      .adm-trajet-card:hover {
        transform: translateY(-3px);
      }

      .adm-trajet-card-header {
        padding: 1rem 1.25rem 0 !important;
      }
      .adm-trajet-card-title-row {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 0.75rem;
        margin-bottom: 0.5rem;
      }
      .adm-trajet-card-title {
        font-size: 1.05rem !important;
        font-weight: 700 !important;
        color: var(--ui-navy) !important;
      }
      .adm-trajet-card-sub {
        color: var(--ui-text-secondary) !important;
      }
      .adm-trajet-date-row {
        display: flex;
        gap: 1rem;
        font-size: 0.82rem;
        color: var(--ui-text-secondary);
        padding-bottom: 0.5rem;
        flex-wrap: wrap;
      }

      .adm-trajet-content {
        padding: 0.75rem 1.25rem !important;
      }
      .adm-trajet-info-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.75rem;
      }
      .adm-info-item {
        display: flex;
        flex-direction: column;
        gap: 0.2rem;
      }
      .adm-passagers-item {
        grid-column: 1 / -1;
      }
      .adm-info-label {
        font-size: 0.72rem;
        font-weight: 600;
        color: var(--ui-text-secondary);
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }
      .adm-info-value {
        font-size: 0.9rem;
        font-weight: 600;
        color: var(--ui-navy);
      }

      .adm-passagers-bar {
        background: var(--ui-surface-subtle);
        border-radius: 9999px;
        height: 1.25rem;
        overflow: hidden;
        border: 1px solid var(--ui-surface-subtle);
        margin-top: 0.2rem;
      }
      .adm-passagers-fill {
        height: 100%;
        background: var(--ui-primary);
        border-radius: 9999px;
        display: flex;
        align-items: center;
        justify-content: center;
        min-width: 2rem;
        font-size: 0.7rem;
        font-weight: 700;
        color: var(--ui-surface);
        transition: width 0.4s ease;
      }

      .adm-reservations-panel {
        margin-top: 0.75rem;
        border-radius: 0.75rem;
        border: 1px solid var(--ui-warning);
        background: var(--ui-surface-subtle);
        padding: 0.75rem;
      }
      .adm-reservations-title {
        font-weight: 700;
        color: var(--ui-danger);
        font-size: 0.9rem;
        margin: 0 0 0.5rem;
      }
      .adm-reservation-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 0.5rem;
        padding: 0.5rem 0.75rem;
        border-radius: 0.5rem;
        background: var(--ui-surface);
        margin-bottom: 0.4rem;
        font-size: 0.85rem;
        border: 1px solid var(--ui-warning);
      }

      .adm-trajet-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        padding: 0.75rem 1.25rem 1rem !important;
        border-top: 1px solid var(--ui-surface-subtle);
      }
      .adm-trajet-btn-details {
        flex: 1;
        min-width: 130px;
        padding: 0.5rem 1rem;
        border-radius: 0.6rem;
        background: var(--ui-surface-subtle);
        color: var(--ui-danger);
        border: 1.5px solid var(--ui-warning);
        font-weight: 700;
        font-size: 0.85rem;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-trajet-btn-details:hover {
        background: var(--ui-warning-soft);
      }
      .adm-trajet-btn-convert {
        flex: 1;
        min-width: 160px;
        padding: 0.5rem 1rem;
        border-radius: 0.6rem;
        background: var(--ui-primary);
        color: var(--ui-surface);
        border: none;
        font-weight: 700;
        font-size: 0.85rem;
        cursor: pointer;
        box-shadow: var(--ui-shadow);
        transition: transform 0.15s;
      }
      .adm-trajet-btn-convert:hover:not(:disabled) {
        transform: translateY(-1px);
      }
      .adm-trajet-btn-convert:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .adm-trajet-empty {
        text-align: center;
        padding: 3rem;
        color: var(--ui-text-secondary);
        font-size: 1.1rem;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
      }
      .adm-trajet-empty span {
        font-size: 3rem;
      }
    </style>
  `,
})
export class AdminTrajetsPage {
  readonly absenceSelectionnee = signal<number | null>(null);
  readonly absenceEnCours = signal(false);
  readonly motifAbsence = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(500)],
  });

  peutDeclarerAbsence(reservation: ReservationResponse, trajet: TrajetResponse): boolean {
    return (
      trajet.statut !== 'ANNULE' &&
      reservation.statut === 'CONFIRMEE' &&
      reservation.paiement?.statut === 'PAYE' &&
      new Date(reservation.trajetDateTime).getTime() <= Date.now()
    );
  }

  declarerAbsence(reservation: ReservationResponse): void {
    if (this.motifAbsence.invalid || this.absenceEnCours()) return;
    this.absenceEnCours.set(true);
    this.errorMessage.set('');
    this.reservationsApi.declarerAbsence(reservation.id, this.motifAbsence.value).subscribe({
      next: (updated) => {
        this.selectedReservations.update((rows) =>
          rows.map((row) => (row.id === updated.id ? updated : row)),
        );
        this.absenceSelectionnee.set(null);
        this.motifAbsence.reset();
        this.absenceEnCours.set(false);
        this.message.set(
          'Déclaration enregistrée. La sanction dépend de la politique explicitement activée.',
        );
      },
      error: (error) => {
        this.absenceEnCours.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(error, "La déclaration n'a pas pu être enregistrée.", (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }
  private readonly trajetsApi = inject(TrajetsApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly agencesApi = inject(AgencesApiService);
  private readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly trajets = signal<TrajetResponse[]>([]);
  readonly agences = signal<AgenceResponse[]>([]);
  readonly selectedTrajetId = signal<number | null>(null);
  readonly selectedReservations = signal<ReservationResponse[]>([]);

  readonly filteredTrajets = computed(() => {
    if (this.adminSession.isGlobalAdmin()) {
      return this.trajets();
    }
    const agenceIds = new Set(this.agences().map((agence) => agence.id));
    return this.trajets().filter(
      (trajet) => trajet.agenceId != null && agenceIds.has(trajet.agenceId),
    );
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      trajets: this.trajetsApi.getAll(),
      agences: this.agencesApi.getAll(),
    }).subscribe({
      next: ({ trajets, agences }) => {
        this.trajets.set(trajets);
        this.agences.set(
          this.adminSession.isGlobalAdmin()
            ? agences
            : agences.filter((agence) => agence.id === this.adminSession.agenceId()),
        );
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.trajets.loadError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  showReservations(trajet: TrajetResponse): void {
    if (this.selectedTrajetId() === trajet.id) {
      this.selectedTrajetId.set(null);
      this.selectedReservations.set([]);
      return;
    }

    this.selectedTrajetId.set(trajet.id);
    this.reservationsApi.getByTrajet(trajet.id).subscribe({
      next: (reservations) => this.selectedReservations.set(reservations),
      error: (error) => {
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.trajets.reservationsError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  convertToPublic(trajetId: number): void {
    this.message.set('');
    this.errorMessage.set('');

    this.trajetsApi.convertToPublic(trajetId).subscribe({
      next: () => {
        this.message.set(this.language.t('admin.trajets.convertSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.trajets.convertError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  typeBadgeClass(type: TrajetResponse['typeTrajet']): string {
    return type === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-neutral';
  }

  typeLabel(type: TrajetResponse['typeTrajet']): string {
    return type === 'PRIVE'
      ? this.language.t('voyageur.create.private')
      : this.language.t('voyageur.create.public');
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

  trajetStatusLabel(statut: TrajetResponse['statut']): string {
    if (statut === 'PLANIFIE') {
      return this.language.t('common.trajetPlanned');
    }
    if (statut === 'COMPLET') {
      return this.language.t('common.trajetFull');
    }
    return this.language.t('common.trajetCanceled');
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
