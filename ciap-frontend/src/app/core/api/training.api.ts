import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, PageResponse, EventResponse, CreateEventRequest
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class TrainingApi {
  private readonly base = `${environment.apiBaseUrl}/training`;

  constructor(private http: HttpClient) {}

  listPublished(page = 0, size = 50): Observable<ApiResponse<PageResponse<EventResponse>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<EventResponse>>>(this.base, { params });
  }

  listAll(page = 0, size = 50): Observable<ApiResponse<PageResponse<EventResponse>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<EventResponse>>>(`${this.base}/all`, { params });
  }

  create(body: CreateEventRequest): Observable<ApiResponse<EventResponse>> {
    return this.http.post<ApiResponse<EventResponse>>(this.base, body);
  }

  publish(id: number): Observable<ApiResponse<EventResponse>> {
    return this.http.post<ApiResponse<EventResponse>>(`${this.base}/${id}/publish`, {});
  }

  register(id: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/${id}/register`, {});
  }

  cancel(id: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/${id}/cancel`, {});
  }
}
