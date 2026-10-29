import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, CategoryOffer, PriceVersion, PricingMethod, ServiceOffer
} from '../models/api.models';

export interface SaveCategoryBody {
  code: string;
  name: string;
  description?: string;
  consultancyType: string;
  displayOrder?: number;
  active?: boolean;
}

export interface SaveServiceBody {
  categoryId: number;
  name: string;
  description?: string;
  pricingMethod: PricingMethod;
  amount?: number | null;
  currency?: string;
  unitLabel?: string;
  conditions?: string;
  published?: boolean;
  active?: boolean;
  note?: string;
}

@Injectable({ providedIn: 'root' })
export class CatalogueApi {
  private readonly base = `${environment.apiBaseUrl}/consultancy-catalogue`;

  constructor(private http: HttpClient) {}

  categories(): Observable<ApiResponse<CategoryOffer[]>> {
    return this.http.get<ApiResponse<CategoryOffer[]>>(`${this.base}/categories`);
  }

  manageCategories(): Observable<ApiResponse<CategoryOffer[]>> {
    return this.http.get<ApiResponse<CategoryOffer[]>>(`${this.base}/categories/manage`);
  }

  saveCategory(body: SaveCategoryBody, id?: number): Observable<ApiResponse<CategoryOffer>> {
    return id
      ? this.http.put<ApiResponse<CategoryOffer>>(`${this.base}/categories/${id}`, body)
      : this.http.post<ApiResponse<CategoryOffer>>(`${this.base}/categories`, body);
  }

  services(categoryId: number): Observable<ApiResponse<ServiceOffer[]>> {
    return this.http.get<ApiResponse<ServiceOffer[]>>(`${this.base}/categories/${categoryId}/services`);
  }

  manageServices(categoryId: number): Observable<ApiResponse<ServiceOffer[]>> {
    return this.http.get<ApiResponse<ServiceOffer[]>>(`${this.base}/categories/${categoryId}/services/manage`);
  }

  service(id: number): Observable<ApiResponse<ServiceOffer>> {
    return this.http.get<ApiResponse<ServiceOffer>>(`${this.base}/services/${id}`);
  }

  saveService(body: SaveServiceBody, id?: number): Observable<ApiResponse<ServiceOffer>> {
    return id
      ? this.http.put<ApiResponse<ServiceOffer>>(`${this.base}/services/${id}`, body)
      : this.http.post<ApiResponse<ServiceOffer>>(`${this.base}/services`, body);
  }

  prices(id: number): Observable<ApiResponse<PriceVersion[]>> {
    return this.http.get<ApiResponse<PriceVersion[]>>(`${this.base}/services/${id}/prices`);
  }
}
