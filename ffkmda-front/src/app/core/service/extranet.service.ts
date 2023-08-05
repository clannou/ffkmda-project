import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { forkJoin, map, Observable, shareReplay } from 'rxjs';
import { FilterQuery, IPageable, Page, PageableSearch } from '../models/pageable.model';
import { isEmpty } from 'lodash';
import {environment} from "../../../environments/environment";


const httOptions = {
  headers: new HttpHeaders({
    'Content-Type': 'application/json',
  })
}

export interface AutoCompleteResult {
  clubs: AutoCompleteData[];
  sieges: AutoCompleteData[];
}

export interface AutoCompleteData {
  _id: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class ExtranetService {

  constructor(private _http: HttpClient) { }


  search(page?: PageableSearch): Observable<Page<any>> {

    const params: {} = !page ? {} : {
      _offset: page.pageable?.pageNumber,
      _limit: page.pageable?.pageSize,
      sortBy: "nom",
      sortDirection: "asc"
    }
    //prod-club.ffkmda.fr
    return this._http.get<Page<any>>(environment.url+`/api/v1/clubs/search`, { params: params })
  }

  searchByCommune(query?: any, page?: PageableSearch): Observable<Page<any>> {

    const params: {} = !page ? {} : {
      page: page.pageable?.pageNumber,
      size: page.pageable?.pageSize,
      sortBy: "nom",
      sortDirection: "asc"
    }
    //prod-club.ffkmda.fr
    return this._http.get<Page<any>>(environment.url+`/api/v1/clubs/address?fulltext=${query}`, { params: params })
  }

  searchByCodePostal(query?: any, page?: PageableSearch): Observable<Page<any>> {

    const params: {} = !page ? {} : {
      page: page.pageable?.pageNumber || 1,
      size: page.pageable?.pageSize,
      sortBy: "nom",
      sortDirection: "asc"
    }


    return this._http.get<Page<any>>(environment.url+`/api/v1/clubs/address?code_postal_fr=${query}`, { params: params })
  }


  searchByDep(dep: any, page?: PageableSearch): Observable<any> {

    const params: {} = !page ? {} : {
      page: page.pageable?.pageNumber,
      size: page.pageable?.pageSize,
      sortBy: "nom",
      sortDirection: "asc"
    }

    return this._http.get<Page<any>>(environment.url+`/api/v1/clubs/address?code_departement=${dep}`, { params: params })
  }


  searchByFullText(query: any, page?: PageableSearch): Observable<any> {

    const params: {} = !page ? {} : {
      page: page.pageable?.pageNumber,
      size: page.pageable?.pageSize,
      sortBy: "nom",
      sortDirection: "asc"
    }

    return this._http.get<Page<any>>(environment.url+`/api/v1/clubs/address?fulltext=${query}`, { params: params })
  }

  getAutoComplete(query?: any): Observable<any> {
    return forkJoin([
      this.searchByCodePostal(query),
      this.searchByCommune(query),
      this.searchByFullText(query),
    ]).pipe(
      map(([clubs, commune, dep]) => ({
        clubs,
        commune,
        dep
      })
      )
    )
  }




  //http://prod-club.ffkmda.fr:9008/api/v1/clubs/address?code_insee_departement



//don't forget to replace endpoint for production
//http://localhost
  public getOne(code: any): Observable<any> {
    return this._http.get(environment.url+`/api/v1/structures/${code}`);
  }

  getClubsSearch(filters?: any, page?: PageableSearch): Observable<Page<any>> {
    const filterQuery: any = this.constructFiltersQuery(filters);
  
    //let params = new HttpParams();
    let filterSplit = filterQuery.split("=")
  
    const params: {} = !page ? {} : {
      [filterSplit[0]]: filterSplit[1],
      page: page.pageable?.pageNumber,
      size: page.pageable?.pageSize,
      sortBy: 'nom',
      sortDirection: 'asc'
    }
  
    
    return this._http.get<Page<any>>(environment.url+`/api/v1/clubs/search?`, { params: params });
  }

    
  private constructFiltersQuery(filters: any): string {
    let queryParams = '';
   
    if (filters.dep) {
      queryParams += `code_insee_departement=${filters.dep}&`;
    }
    if (filters.search) {
      queryParams += this.isNumber(filters.search) ? 
       `code_postal_fr=${filters.search}&` : `commune=${filters.search}&`
    }
    
    return queryParams.slice(0, -1);
  }
  
  isNumber(n: any) { return !isNaN(parseFloat(n)) && !isNaN(n - 0) }


}