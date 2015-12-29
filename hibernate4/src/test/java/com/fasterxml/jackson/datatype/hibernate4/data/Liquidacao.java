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
    public Long id;

    @Column(name="valor_total")
    public BigDecimal valorTotal;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contrato_id")
    public Contrato contrato;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parcela_id") @JsonIgnoreProperties("contrato")
    public Parcela parcela;
}
