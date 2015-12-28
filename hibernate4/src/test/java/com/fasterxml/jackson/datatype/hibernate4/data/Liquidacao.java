package com.fasterxml.jackson.datatype.hibernate4.data;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity @Table(name="Liquidacao", catalog="classicmodels")
public class Liquidacao {

    @Id
    private Long id;

    @Column(name="valor_total")
    private BigDecimal valorTotal;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contrato_id")
    private Contrato contrato;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parcela_id") @JsonIgnoreProperties("contrato")
    private Parcela parcela;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(BigDecimal valorTotal) {
		this.valorTotal = valorTotal;
	}

	public Contrato getContrato() {
		return contrato;
	}

	public void setContrato(Contrato contrato) {
		this.contrato = contrato;
	}

	public Parcela getParcela() {
		return parcela;
	}

	public void setParcela(Parcela parcela) {
		this.parcela = parcela;
	}
    
    
    
}