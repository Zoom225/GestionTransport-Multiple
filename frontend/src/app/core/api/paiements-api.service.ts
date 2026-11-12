import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PaiementResponse } from '../../shared/models/reservation.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class PaiementsApiService {
  constructor(private readonly http: HttpClient) {}

  pay(reservationId: number, _voyageurId?: number): Observable<PaiementResponse> {
    return this.http.post<PaiementResponse>(
      apiUrl(`/paiements/reservation/${reservationId}/pay`),
      {}
    );
  }

  getById(id: number): Observable<PaiementResponse> {
    return this.http.get<PaiementResponse>(apiUrl(`/paiements/${id}`));
  }

  getByReservation(reservationId: number): Observable<PaiementResponse> {
    return this.http.get<PaiementResponse>(apiUrl(`/paiements/reservation/${reservationId}`));
  }

  getByVoyageur(voyageurId: number): Observable<PaiementResponse[]> {
    return this.http.get<PaiementResponse[]>(apiUrl(`/paiements/voyageur/${voyageurId}`));
  }
}
