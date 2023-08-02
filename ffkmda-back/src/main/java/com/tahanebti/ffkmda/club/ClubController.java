package com.tahanebti.ffkmda.club;

import java.time.Year;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tahanebti.ffkmda.base.PageRequestBuilder;
import com.tahanebti.ffkmda.specification.SpecificationsBuilder;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/clubs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ClubController {

	private final ClubService clubService;
	private final ClubMapper clubMapper;
	private final SpecificationsBuilder<Club> spec;
	private final RestTemplate restTemplate;
	private static final String accessUrl = "https://extranet.ffkmda.fr/api/v1/login";
	private final CacheManager cacheManager;



	@GetMapping
	public List<ClubResponse> find(@RequestParam(required = false) String query) {
		return clubService.find(spec.with(query)).stream().map(clubMapper::toResponse).collect(Collectors.toList());
	}


	@GetMapping("/search2")
	public Map<String, Object> search(
			@Parameter(description = "native query search {} -- Example : code=:eq:ffkmda&siege.commune=:cn:bagnolet") @RequestParam(required = false) String query,
			@RequestParam(required = false, defaultValue = "1") Integer _offset,
			@RequestParam(required = false, defaultValue = "10") Integer _limit,
			@RequestParam(required = false, defaultValue = "id") String _sort

	) throws JsonProcessingException {
		Page<Club> pageEntity = clubService.search(spec.with(query), _limit, _offset, _sort);

		Map<String, Object> response = new HashMap<>();
		response.put("payload",
				pageEntity.getContent().stream().map(clubMapper::toResponse).collect(Collectors.toList()));

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(PageRequestBuilder.getPageRequest(_offset, _limit, _sort));
		Map<String, Object> map = mapper.readValue(json, Map.class);

		response.put("pageNumber", pageEntity.getNumber());
		response.put("pageSize", pageEntity.getSize());
		response.put("totalElements", pageEntity.getTotalElements());
		response.put("last", pageEntity.isLast());
		response.put("first", pageEntity.isFirst());
		response.put("numberOfElements", pageEntity.getNumberOfElements());
		response.put("empty", pageEntity.isEmpty());
		return response;
	}


	@GetMapping("/{id}")
	public ClubResponse getClub(@PathVariable Long id) {
		Club Club = clubService.validateAndGetById(id);
		return clubMapper.toResponse(Club);
	}

	@GetMapping("/code/{code}")
	public ClubResponse getClubByCode(
			@Parameter(description = "find club by code {} -- Example : ffkmda") @PathVariable String code) {
		Club Club = clubService.validateAndGetByCode(code);
		return clubMapper.toResponse(Club);
	}

	@GetMapping("/etat/{etat}")
	public List<ClubResponse> getClubByEtat(
			@Parameter(description = "return list of clubs by etat {} --- Example : A") @PathVariable String etat) {
		List<Club> clubs = clubService.validateAndGetByEtat(etat);
		return clubs.stream().map(clubMapper::toResponse).collect(Collectors.toList());
	}

	@GetMapping("/type/{type}")
	public List<ClubResponse> getClubByType(
			@Parameter(description = "return list of clubs by type {} --- Example : CLU, DEP, LIG etc ....")
			@PathVariable String type) {
		List<Club> clubs = clubService.validateAndGetByType(type);
		return clubs.stream().map(clubMapper::toResponse).collect(Collectors.toList());
	}

	@GetMapping("/address")
	@Cacheable(value = "clubsByAddress", keyGenerator = "customKeyGenerator")
	public Map<String, Object> findClubsBySiegeAddressCommune(
			@Parameter(description = "return list of clubs by nom full text {} --- Example : ADRENALINE FIGHT TEAM ....") @RequestParam(required = false) String fulltext,
			@Parameter(description = "return list of clubs by commune {} --- Example : bagnolet ....") @RequestParam(required = false) String commune,
			@Parameter(description = "return list of clubs by zip {} --- Example : 35000 ....") @RequestParam(required = false) String code_postal_fr,
			@Parameter(description = "return list of clubs by street name {} --- Example : Malmaison ....") @RequestParam(required = false) String nom_voie,
			@Parameter(description = "return list of clubs by street type {} --- Example : Rue ....") @RequestParam(required = false) String type_voie,
			@Parameter(description = "return list of clubs by code departement {} --- Example : 091 ....") @RequestParam(required = false) String code_insee_departement,
			@Parameter(description = "return list of clubs by code departement {} --- Example : 91 ....") @RequestParam(required = false) String code_departement,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "nom") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDirection
	) {



		List<Club> clubs = clubService.findClubsBySiegeAddress(fulltext, commune, code_postal_fr, nom_voie, type_voie, code_insee_departement, code_departement, sortBy, sortDirection);
		clubs.removeIf(club -> {
			try {
				return !filterByCurrentYearAffiliation(club);
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			return false;
		});


		Pageable paging = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

		int pageSize = paging.getPageSize();
		long pageOffset = paging.getOffset();
		long total = pageOffset + clubs.size() + (clubs.size() == pageSize ? pageSize : 0);


		final Page<Club> pageEntity = new PageImpl<>(clubs, paging, total);

		Map<String, Object> response = new HashMap<>();
		response.put("data", pageEntity.getContent().stream()
				.map(clubMapper::toResponse).collect(Collectors.toList()));
		response.put("offset", pageEntity.getPageable().getOffset());
		response.put("current", pageEntity.getPageable());
		response.put("next", pageEntity.getPageable().next());
		response.put("previous", pageEntity.getPageable().previousOrFirst());
		response.put("totalElements", pageEntity.getTotalElements());
		response.put("last", pageEntity.isLast());
		response.put("first", pageEntity.isFirst());
		response.put("numberOfElements", pageEntity.getNumberOfElements());
		response.put("empty", pageEntity.isEmpty());

		// Cache the result


		return response;
	}

	private String generateCacheKey(String fulltext, String commune,
									String code_postal_fr, String nom_voie,
									String type_voie, String code_insee_departement, String code_departement, String sortBy, String sortDirection) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append("findClubsBySiegeAddressCommune:");

		if (fulltext != null) {
			keyBuilder.append("fulltext=").append(fulltext).append(":");
		}

		if (commune != null) {
			keyBuilder.append("commune=").append(commune).append(":");
		}

		if (code_postal_fr != null) {
			keyBuilder.append("code_postal_fr=").append(code_postal_fr).append(":");
		}

		if (nom_voie != null) {
			keyBuilder.append("nom_voie=").append(nom_voie).append(":");
		}

		if (type_voie != null) {
			keyBuilder.append("type_voie=").append(type_voie).append(":");
		}

		if (code_insee_departement != null) {
			keyBuilder.append("code_insee_departement=").append(code_insee_departement).append(":");
		}

		if (code_insee_departement != null) {
			keyBuilder.append("code_departement=").append(code_departement).append(":");
		}

		keyBuilder.append("sortBy=").append(sortBy).append(":");
		keyBuilder.append("sortDirection=").append(sortDirection);

		return keyBuilder.toString();
	}


	private boolean filterByCurrentYearAffiliation(Club club) throws JsonProcessingException {

		String token = getAccessTokenFromExtranetFfkmda();

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.add("Authorization", "Bearer "+ token );

		HttpEntity<String> httpEntity = new HttpEntity<>(headers);
		String accessUrl = "https://extranet.ffkmda.fr/api/v1/structure/{code}?include=instances,disciplines,derniere_affiliation";


		ResponseEntity<Object> responseEntity = restTemplate.exchange(accessUrl,
				HttpMethod.GET, httpEntity, Object.class, club.getCode());


		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(responseEntity.getBody());
		Map<String, Object> jsonMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

		Object saison = findSaison(jsonMap);
		if (saison != null) {
			return isCurrentYearAffiliation(saison);
		}


		return false;


	}

	private boolean isCurrentYearAffiliation(Object saison) {
		int affiliationYear = Integer.parseInt(saison.toString());
		int currentYear = Year.now().getValue();
		return affiliationYear == currentYear;
	}

	// Recursive method to find the "saison" within the JSON object
	private Object findSaison(Map<String, Object> jsonMap) {
		for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (key.equals("derniere_affiliation")) {
				// Check if the value is a nested map with "saison" field
				if (value instanceof Map) {
					Map<String, Object> nestedMap = (Map<String, Object>) value;
					if (nestedMap.containsKey("saison")) {
						return nestedMap.get("saison");
					}
				}
				return null; // "saison" not found within "derniere_affiliation"
			}

			if (value instanceof Map) {
				Object saison = findSaison((Map<String, Object>) value);
				if (saison != null) {
					return saison;
				}
			}
		}

		return null; // "saison" not found
	}

	public String getAccessTokenFromExtranetFfkmda() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept", "application/json");

		Map<String, String> params = new HashMap<String, String>();
		params.put("username", "SiteInternet");
		params.put("password", "$elic!Europea#");

		HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);
		String response = this.restTemplate.postForObject(accessUrl, request, String.class);
		try {
			return new ObjectMapper().readTree(response).get("success").get("token").asText();
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error parsing response while requesting token from ffkmda");
		}
	}


	@GetMapping("/search")
	public Map<String, Object> searchClubs(
			@RequestParam(required = false) String city,
			@RequestParam(required = false) String commune,
			@RequestParam(required = false) String code_postal_fr,
			@RequestParam(required = false) String nom_voie,
			@RequestParam(required = false) String type_voie,
			@RequestParam(required = false) String code_insee_departement,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "nom") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDirection
	) {

		List<Club> clubs = clubService.searchClubs(
				city,
				commune,
				code_postal_fr,
				nom_voie,
				type_voie,
				code_insee_departement,
				page,
				size,
				sortBy,
				sortDirection
		);


		Pageable paging = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

		int pageSize = paging.getPageSize();
		long pageOffset = paging.getOffset();
		long total = pageOffset + clubs.size() + (clubs.size() == pageSize ? pageSize : 0);


		final Page<Club> pageEntity = new PageImpl<>(clubs, paging, total);

		Map<String, Object> response = new HashMap<>();
		response.put("data", pageEntity.getContent().stream().map(clubMapper::toResponse).collect(Collectors.toList()));
		response.put("offset", pageEntity.getPageable().getOffset());
		response.put("current", pageEntity.getPageable());
		response.put("next", pageEntity.getPageable().next());
		response.put("previous", pageEntity.getPageable().previousOrFirst());
		response.put("totalElements", pageEntity.getTotalElements());
		response.put("last", pageEntity.isLast());
		response.put("first", pageEntity.isFirst());
		response.put("numberOfElements", pageEntity.getNumberOfElements());
		response.put("empty", pageEntity.isEmpty());
		return response;


	}

	@GetMapping("/phone")
	public List<ClubResponse> findClubsBySiegePhone(
			@Parameter(description = "return list of clubs by tel type {} --- Example : tel....") @RequestParam(required = false) String tel,
			@Parameter(description = "return list of clubs by mobile type {} --- Example : mobile=0640270263 ....") @RequestParam(required = false) String mobile,
			@Parameter(description = "return list of clubs by street fax {} --- Example : fax....") @RequestParam(required = false) String fax

	) {
		List<Club> clubs = clubService.findClubsBySiegePhone(tel, mobile, fax);
		return clubs.stream().map(clubMapper::toResponse).collect(Collectors.toList());
	}


	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ClubResponse addClub(@Valid @RequestBody CreateClubRequest request) {
		Club Club = clubMapper.toCreate(request);

		Club = clubService.save(Club);
		return clubMapper.toResponse(Club);
	}

	@PatchMapping("/{id}")
	public ClubResponse updateClub(@PathVariable Long id, @Valid @RequestBody UpdateClubRequest updateClubRequest) {
		Club Club = clubService.validateAndGetById(id);

		// Optional.ofNullable(Club.getName()).ifPresent(Club::setName);

		return clubMapper.toResponse(Club);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteClub(@PathVariable Long id) {
		Club Club = clubService.validateAndGetById(id);
		clubService.delete(Club);

		return ResponseEntity.ok(true);
	}


}
