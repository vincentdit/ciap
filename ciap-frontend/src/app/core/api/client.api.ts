import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, ClientRegistrationRequest, ClientResponse, PageResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ClientApi {
  private readonly base = `${environment.apiBaseUrl}/clients`;

  constructor(private http: HttpClient) {}

  /** Public self-registration — creates a Client plus a linked CLIENT login. */
  register(body: ClientRegistrationRequest): Observable<ApiResponse<ClientResponse>> {
    return this.http.post<ApiResponse<ClientResponse>>(`${this.base}/register`, body);
  }

  get(id: number): Observable<ApiResponse<ClientResponse>> {
    return this.http.get<ApiResponse<ClientResponse>>(`${this.base}/${id}`);
  }

  /** The client profile for the currently authenticated CLIENT user. */
  me(): Observable<ApiResponse<ClientResponse>> {
    return this.http.get<ApiResponse<ClientResponse>>(`${this.base}/me`);
  }

  list(page = 0, size = 100): Observable<ApiResponse<PageResponse<ClientResponse>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<ClientResponse>>>(this.base, { params });
  }
}
