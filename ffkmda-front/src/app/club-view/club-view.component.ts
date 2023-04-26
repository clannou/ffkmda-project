import { isPlatformServer } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { combineLatest, filter, map, Observable, of, ReplaySubject, share, startWith, Subject, switchMap, takeUntil, tap } from 'rxjs';
declare var $: any;
import { groupBy } from 'lodash-es';
import { isEmpty } from 'lodash';
import { fadeAnimation } from '../shared/animations/fade.animation';
import { AutoCompleteResult, ExtranetService } from '../core/service/extranet.service';
import { PageRequest } from '../core/models/page-request.model';
import { DrawerLocation } from '../shared/components/drawer/drawer-location.enum';
import { DrawerSize } from '../shared/components/drawer/drawer-size.enum';
import { DrawerService } from '../shared/components/drawer/drawer.service';
import { IPageable, DEFAULT_SEARCH_PAGE, NULL_SEARCH_PAGE, PageableSearch } from '../core/models/pageable.model';

const typeMap: Record<string, string> = {
  ClubDepartement: 'departement',
  ClubCommune: 'commune',
};

@Component({
  selector: 'app-club-view',
  templateUrl: './club-view.component.html',
  styleUrls: ['./club-view.component.css'],
  animations: [fadeAnimation]
})
export class ClubViewComponent implements OnInit, AfterViewInit, OnDestroy  {
  regions: any;
  clubs: any
  targetCode: any;
  filter: any
  p: any = 1 
  total: any;
  cubPage: any;
  
  private readonly DEFAULT_PAGE: IPageable = DEFAULT_SEARCH_PAGE || NULL_SEARCH_PAGE;

  private _subscriptionSubject: Subject<void>;

  sections$!: Observable<{ type: string; clubs: any[] }[]>;

  filters: any = null;
  readonly filtersSubject: Subject<any> = new ReplaySubject();
  readonly filters$: Observable<any> = this.filtersSubject.asObservable();

  private readonly getClubsTrigger = new Subject<void>();



  public items = new Array(18);
  public isLoading = false;
  public isServer!: boolean;
  commune: any
  sort = 'nom,asc';

  opened = true;

  public suggestions$!: Observable<AutoCompleteResult>;
  public  autoCompleteSubject = new Subject<string>();
  
  clubs$!: Observable<any>;
  
  queryparams: any;
  filterQuery$!: Observable<any>;


  public DrawerLocation = DrawerLocation;
  public DrawerSize = DrawerSize.MEDIUM;
  constructor(
    private _extranetService: ExtranetService,
    private _drawerService: DrawerService,
    @Inject(PLATFORM_ID) private platformId: Object, //eslint-disable-line
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {
    this._subscriptionSubject = new Subject<void>();
    

    this.suggestions$ = this.autoCompleteSubject
    .asObservable()
    .pipe(
      switchMap((searchTerm) =>
        this._extranetService.getAutoComplete(searchTerm).pipe(
          tap(console.log)
        )
      )
    );


  }

  ngOnInit() {
    this.listenToEvents();    
    this.isServer = isPlatformServer(this.platformId);
  }

  
  onSearch(searchTerm: string) {
    this.router.navigate(['/'], { queryParams: { search: searchTerm } });
  }

  onAutoComplete($event: string) {
    this.autoCompleteSubject.next($event);
  }

  onFilter({ key, value }: { key: string; value: string }) {
    this.router.navigate(['/'], {
      queryParams: {
        filters: `${key}=${value}`,
      },
      queryParamsHandling: 'merge',
    });
  }
  
  toggle(): void {
    this.opened = !this.opened;
  }

  public onView(data: any): void {
    console.log("data", data)
    this._drawerService.show(ClubViewComponent, {
      data: data
    });
  }
  
  listenToEvents(){
    this.listenToSearchQuery()
  }
  
  listenToSearchQuery(page?: PageableSearch) {
    this.setLoading(true)
    const clubSearch: PageableSearch = {
      pageable: this.cubPage || this.DEFAULT_PAGE
    };
    this._extranetService
    .searchByCommune("paris", clubSearch)
    .pipe(
    //  tap((resp: any) => this.setLoading(true)),
      switchMap(
        (query?: any) => (this.sections$ = this.getSearchRequest(query, page))
      ),
      takeUntil(this._subscriptionSubject)
    )
    .subscribe(resp => {
      this.clubs = resp
      console.log(resp)
      this.setLoading(false);
      this.cdr.detectChanges();
    })
  }


  private getSearchRequest(query?: any, page?: any): Observable<any> {
    console.log("target page", this.cubPage)
    const clubSearch: PageableSearch = {
      pageable: this.cubPage || this.DEFAULT_PAGE
    };
   
    return this.route.queryParams
    .pipe(switchMap((p: any) => {

      const dep = p.dep;
      const search = p.search;

      // if (Object.keys(p).length === 0) {
      //   console.log('Query parameters are null');
      //   return this._extranetService
      //   .search(clubSearch)
      // }

      if (dep) {
        console.log(dep);
        this.filter = dep;
        return this._extranetService.searchByDep(dep, clubSearch);
      }

      if (search) {
        console.log(typeof search);
        this.filter = search;
        
        const isFullTextSearch = /\s/.test(query);

        console.log(search)

        if(isFullTextSearch && !this.isNumber(search)){
          return this._extranetService.searchByFullText(search, clubSearch);
        } else if (this.isNumber(search)) {
          console.log("is a number", search)
          return this._extranetService.searchByCodePostal(search, clubSearch);
        } else  {
          return this._extranetService.searchByCommune(search, clubSearch) ;
        }       
      }

      return of([]);
    }))
  }

isNumber(n: any) { return !isNaN(parseFloat(n)) && !isNaN(n - 0) }

isString(s: any): boolean {
  return typeof s === "string" || s instanceof String;
}
  
isFullTextSearch(query: any) {
  // Check if the query contains any spaces
  return query.indexOf(' ') !== -1;
}

formatQueryParamString(paramString: any) {
  // Replace special characters and spaces with "-"
  return paramString.replace(/[^\w\s]/gi, '').replace(/\s+/g, '-');
}

  private setLoading(isLoading: boolean): void {
    this.isLoading = isLoading;
    this.cdr.detectChanges();
  }

  
  ngAfterViewInit() {
    var regions = this.regions;

    $(function() {
      $('#vmap').vectorMap({
        map: 'france_fr',
        backgroundColor: '#fff',
        borderColor: '#818181',
        borderOpacity: 0.75,
        borderWidth: 1,
        color: '#f4f3f0',
        enableZoom: true,
        hoverColor: '#c9dfaf',
        hoverOpacity: null,
        normalizeFunction: 'linear',
        scaleColors: ['#b6d6ff', '#005ace'],
        selectedColor: '#c9dfaf',
        selectedRegions: null,
        showTooltip: true,
   // Click by region
    onRegionClick: function(element:any, code:any, region:any) {

      var name = '<strong>' + region + '</strong><br>';
      localStorage.setItem("commune", region)
      console.log(code)
      let dep = code.split("-")[1]
      var urlParts = location.href.split("?");
      location.href = urlParts[0] + "?dep=" + dep;

      //location.reload()
    },
    });
  })
   
  
  }

  public onGoToPage(event: any): void {
      console.log(event)
      this.cubPage = PageRequest.from(event.pageNumber, event.pageSize);
      
      console.log("target page", this.cubPage)

      this.getSearchRequest(this.filter, this.cubPage)
      .pipe(
        takeUntil(this._subscriptionSubject)
      )
      .subscribe(resp => {
        this.clubs = resp
        console.log(resp)
        this.setLoading(false);
        this.cdr.detectChanges();
      })
  }

 
  
  ngOnDestroy(): void {
    this._subscriptionSubject.next();
    this._subscriptionSubject.complete();
    console.log('person component destroyed');
  }



}

