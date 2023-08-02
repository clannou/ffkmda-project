package com.tahanebti.ffkmda.batch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.util.Strings;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tahanebti.ffkmda.address.Address;
import com.tahanebti.ffkmda.club.Club;
import com.tahanebti.ffkmda.phone.Phone;
import com.tahanebti.ffkmda.siege.Siege;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RESTClubReader implements ItemReader<Club> {

	private  String apiUrl;
	private RestTemplate restTemplate;

	private int nextClubIndex;
	private List<Club> clubData;


	RESTClubReader(String apiUrl, RestTemplate restTemplate) {
		this.apiUrl = apiUrl;
		this.restTemplate = restTemplate;
		nextClubIndex = 0;
	}

	@Override
	public Club read()
			throws Exception {

		log.info("Reading the information of the next club");

		if (clubDataIsNotInitialized()) {
			clubData = fetchDataFromAPI();
		}

		Club nextClub = null;
		if (nextClubIndex < clubData.size()) {
			nextClub =  clubData.get(nextClubIndex);
			nextClubIndex++;
		}
		else {
			nextClubIndex = 0;
			clubData = null;
		}

		log.info("Found club: {}", nextClub);


		return nextClub;
	}

	private List<Club> fetchDataFromAPI() throws IOException {
		String token = getAccessTokenFromExtranetFfkmda();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.add("Authorization", "Bearer "+ token );

		HttpEntity<String> httpEntity = new HttpEntity<>(headers);

		URL url = new URL(apiUrl);
		HttpURLConnection http = (HttpURLConnection)url.openConnection();
		http.setRequestProperty("Authorization", "Bearer" + token);
		log.info(http.getResponseCode() + " " + http.getResponseMessage());
		if(http.getResponseCode() == 200) {

			ResponseEntity<Object> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.GET, httpEntity, Object.class);

			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(responseEntity.getBody());
			Map jsonMap = mapper.readValue(json, Map.class);

			List list = (List)jsonMap.get("data");

			List<Club> target = new ArrayList<>();
			for(Object item : list) {
				Map<String,Object> ind = (Map<String,Object> ) item;
				Map<String,Object>  listSiege = (Map<String,Object> )ind.get("siege");


				Club club = Club.builder()
						.code(ind.get("code") == null ? null : ind.get("code").toString())
						.etat(ind.get("etat") == null ? null : ind.get("etat").toString())
						.nom(ind.get("nom") == null ? null : ind.get("nom").toString())
						.nom_court(ind.get("nom_court") == null ? null : ind.get("nom_court").toString())
						.type(ind.get("type") == null ? null : ind.get("type").toString())
						.code_region(ind.get("code_region") == null ? null : ind.get("code_region").toString())
						.code_departement(ind.get("code_departement") == null ? null : ind.get("code_departement").toString())
						.siege(Siege.builder()
								.nom_voie(listSiege.get("nom_voie") == null ? null : listSiege.get("nom_voie").toString())
								.num_voie(listSiege.get("num_voie") == null ? null : listSiege.get("num_voie").toString())
								.type_voie(listSiege.get("type_voie") == null ? null : listSiege.get("type_voie").toString())
								.code_postal_fr(listSiege.get("code_postal_fr") == null ? null : listSiege.get("code_postal_fr").toString())
								.code_postal_et(listSiege.get("code_postal_et") == null ? null : listSiege.get("code_postal_et").toString())
								.commune(listSiege.get("commune") == null ? null : listSiege.get("commune").toString())
								.pays_code(listSiege.get("pays_code") == null ? null : listSiege.get("pays_code").toString())
								.adresse_complete(listSiege.get("adresse_complete") == null ? null : listSiege.get("adresse_complete").toString())
								.code_insee_departement(listSiege.get("code_insee_departement") == null ? null : listSiege.get("code_insee_departement").toString())
								.code_insee_region(listSiege.get("code_insee_region") == null ? null : listSiege.get("code_insee_region").toString())
								.latitude(listSiege.get("latitude") == null ? null : listSiege.get("latitude").toString())
								.longitude(listSiege.get("longitude") == null ? null : listSiege.get("longitude").toString())
								.tel(listSiege.get("tel") == null ? null : listSiege.get("tel").toString())
								.tel2(listSiege.get("tel2") == null ? null : listSiege.get("tel2").toString())
								.tel_pro(listSiege.get("tel_pro") == null ? null : listSiege.get("tel_pro").toString())
								.mobile(listSiege.get("mobile") == null ? null : listSiege.get("mobile").toString())
								.mobile2(listSiege.get("mobile2") == null ? null : listSiege.get("mobile2").toString())
								.mobile_pro(listSiege.get("mobile_pro") == null ? null : listSiege.get("mobile_pro").toString())
								.fax(listSiege.get("fax") == null ? null : listSiege.get("fax").toString())
								.fax_pro(listSiege.get("fax_pro") == null ? null : listSiege.get("fax_pro").toString())
								.batiment(listSiege.get("batiment") == null ? null : listSiege.get("batiment").toString())
								.escalier(listSiege.get("escalier") == null ? null : listSiege.get("escalier").toString())
								.mail(listSiege.get("mail") == null ? null : listSiege.get("mail").toString())
								.facebook(listSiege.get("facebook") == null ? null : listSiege.get("facebook").toString())
								.twitter(listSiege.get("twitter") == null ? null : listSiege.get("twitter").toString())
								.web(listSiege.get("web") == null ? null : listSiege.get("web").toString())
								.build())
						.build();

				log.info(" ------------> club : {}",club);

				// STEP 2 - fix code department issues
				try{
					fixCodeDepartmentRelatedIssues(club);
				}catch (Exception exception){
					log.error("Unable to fix code department issue for given club {}",club);
					log.error("Stacktrace...",exception);
				}

				// STEP 3 - retrieve affiliation information
				try{
					enrichWithAffiliationData(club);
				}catch (Exception exception){
					log.error("Unable to add affiliation data for given club {}",club);
					log.error("Stacktrace...",exception);
				}

				target.add(club);
			}

			return target;
		}

		return Collections.emptyList();
	}

	private void enrichWithAffiliationData(Club club) throws JsonProcessingException {
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

		if(jsonMap.containsKey("data")){
			Map<String, Object> dataMap = (Map<String, Object>) jsonMap.get("data");
			for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				if (key.equals("derniere_affiliation") && Objects.nonNull(value)) {

					if (value instanceof Map) {
						Map<String, Object> nestedMap = (Map<String, Object>) value;
						if (nestedMap.containsKey("saison")) {
							String saison = nestedMap.get("saison").toString();
							club.setAffiliation_year(saison);
							String etat = nestedMap.get("etat").toString();
							club.setAffiliation_etat(etat);
							LocalDateTime affiliationLDT;
							LocalDateTime validationLDT;

							if(nestedMap.containsKey("date d'affiliation")){
								for (Map.Entry<String, Object> affiliationDateEntry : nestedMap.entrySet()) {
									String aDKey = affiliationDateEntry.getKey();
									Object aDValue = affiliationDateEntry.getValue();
									if (aDValue instanceof Map) {
										Map<String, Object> aDMap = (Map<String, Object>) aDValue;
										if (aDMap.containsKey("date") && Objects.nonNull(aDMap.get("date"))) {
											String date = aDMap.get("date").toString();
											affiliationLDT = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
											club.setAffiliation_request_date(affiliationLDT);
										}
									}
								}
							}

							if(nestedMap.containsKey("date de validation")){
								for (Map.Entry<String, Object> affiliationDateEntry : nestedMap.entrySet()) {
									String aDKey = affiliationDateEntry.getKey();
									Object aDValue = affiliationDateEntry.getValue();
									if (aDValue instanceof Map) {
										Map<String, Object> aDMap = (Map<String, Object>) aDValue;
										if (aDMap.containsKey("date") && Objects.nonNull(aDMap.get("date"))) {
											String date = aDMap.get("date").toString();
											validationLDT = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
											club.setAffiliation_validation_date(validationLDT);
										}
									}
								}
							}
						}
					}

				}
			}
			log.info("Club information about affiliation updated ! {}",club);
		}
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

	/**
	 * Department code are not equals to INSEE value
	 * Set of rules to fix incorrect department code
	 *
	 * @param club to analyze
	 */
	private void fixCodeDepartmentRelatedIssues(Club club) {
		log.info("Fix code for club : {}",club.getCode());
		if(Strings.isNotEmpty(club.getCode_departement())){
			if(club.getCode_departement().startsWith("0")){
				club.setCode_departement(club.getCode_departement().substring(1));
			}

			if(Objects.nonNull(club.getSiege()) && Strings.isNotEmpty(club.getSiege().getCode_insee_departement())){
				if(Strings.isNotEmpty(club.getSiege().getCode_insee_departement())){
					if(!Objects.equals(club.getSiege().getCode_insee_departement(),club.getCode_departement())){
						log.info("Correct club wrong department : {} > {}",club.getCode_departement(),club.getSiege().getCode_insee_departement());
						club.setCode_departement(club.getSiege().getCode_insee_departement());
					}
				}else{
					log.warn("Code insee is null");
					int codeDep = Integer.parseInt(club.getCode_departement().replaceAll("[^\\d.]", ""));
					if(codeDep > 100){
						club.getSiege().setCode_insee_departement(club.getCode_departement());
					}else{
						club.getSiege().setCode_insee_departement(club.getCode_departement());
					}
				}
			}

			if(Objects.nonNull(club.getSiege()) && Strings.isNotEmpty(club.getSiege().getCode_insee_region()) && (!Objects.equals(club.getSiege().getCode_insee_region(),club.getCode_region()))){
				log.info("Correct club wrong region : {} > {}",club.getCode_region(),club.getSiege().getCode_insee_region());
				club.setCode_region(club.getSiege().getCode_insee_region());
			}

		}
	}

	public String getAccessTokenFromExtranetFfkmda() {
		String accessUrl = "https://extranet.ffkmda.fr/api/v1/login";
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


	private boolean clubDataIsNotInitialized() {
		return this.clubData == null;
	}


}
