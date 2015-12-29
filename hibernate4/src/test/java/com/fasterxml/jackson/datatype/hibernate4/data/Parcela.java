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
    public Long id;

    @Column(name="numero_parcela")
    public Integer numeroParcela;  

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contrato_id")
    public Contrato contrato;
}
