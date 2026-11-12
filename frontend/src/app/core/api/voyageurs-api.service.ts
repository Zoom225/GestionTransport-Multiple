import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { VoyageurRequest, VoyageurResponse } from '../../shared/models/voyageur.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class VoyageursApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<VoyageurResponse[]> {
    return this.http.get<VoyageurResponse[]>(apiUrl('/voyageurs'));
  }

  getById(id: number): Observable<VoyageurResponse> {
    return this.http.get<VoyageurResponse>(apiUrl(`/voyageurs/${id}`));
  }

  getCurrent(): Observable<VoyageurResponse> {
    return this.http.get<VoyageurResponse>(apiUrl('/voyageurs/me'));
  }

  getByMatricule(matricule: string): Observable<VoyageurResponse> {
    return this.http.get<VoyageurResponse>(apiUrl(`/voyageurs/matricule/${matricule}`));
  }

  hasRestriction(id: number): Observable<boolean> {
    return this.http.get<boolean>(apiUrl(`/voyageurs/${id}/restriction`));
  }

  hasBalance(id: number): Observable<boolean> {
    return this.http.get<boolean>(apiUrl(`/voyageurs/${id}/balance`));
  }

  create(payload: VoyageurRequest): Observable<VoyageurResponse> {
    return this.http.post<VoyageurResponse>(apiUrl('/voyageurs'), payload);
  }

  update(id: number, payload: VoyageurRequest): Observable<VoyageurResponse> {
    return this.http.put<VoyageurResponse>(apiUrl(`/voyageurs/${id}`), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/voyageurs/${id}`));
  }
}

