package com.fasterxml.jackson.datatype.hibernate4.data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Entity
public class JPAEntity {

	@XmlID
	@Id
	// XmlAttribute, XmlValue or XmlElement is required t
	@XmlAttribute
	private String id;
	
	@ManyToOne
	@XmlIDREF
	private JPAEntity owner;

	public JPAEntity getOwner() {
		return owner;
	}

	public void setOwner(JPAEntity owner) {
		this.owner = owner;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
