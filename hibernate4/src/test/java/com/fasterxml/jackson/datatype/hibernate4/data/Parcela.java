package com.fasterxml.jackson.datatype.hibernate4.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity @Table(name="Parcela", catalog="classicmodels")
public class Parcela {

    @Id
    private Long id;

    @Column(name="numero_parcela")
    private Integer numeroParcela;  

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contrato_id")
    private Contrato contrato;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getNumeroParcela() {
		return numeroParcela;
	}

	public void setNumeroParcela(Integer numeroParcela) {
		this.numeroParcela = numeroParcela;
	}

	public Contrato getContrato() {
		return contrato;
	}

	public void setContrato(Contrato contrato) {
		this.contrato = contrato;
	}
    
    
    
    
}
