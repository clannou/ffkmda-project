package com.tahanebti.ffkmda.club;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.tahanebti.ffkmda.base.LongIdEntity;
import com.tahanebti.ffkmda.siege.Siege;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "clubs")
@ToString
public class Club extends LongIdEntity {

	
	private String nom; 
	private String nom_court;
	private String code;
	private String etat;
	private String type;
	private String logo;
	private String logo_url;
	private String code_region;
	private String getDerniere_affiliation;
	
	@OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch=FetchType.EAGER)
	@JoinColumn(name = "siege_id", referencedColumnName = "id")
	@ToString.Exclude
	private Siege siege;


	

	
	
}
