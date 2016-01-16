package com.fasterxml.jackson.datatype.hibernate4.data;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity @Table(name="Contrato", catalog="classicmodels")
public class Contrato {
    @Id
    public Long id;

    @Column(name="numero_contrato")
    public String numeroContrato;

    @OneToMany(mappedBy="contrato") @JsonIgnoreProperties("contrato")
    private List<Parcela> parcelas;

    @OneToMany(mappedBy="contrato") @JsonIgnoreProperties("contrato")
    private List<Liquidacao> liquidacoes;

    public List<Parcela> getParcelas() {
        return parcelas;
    }

    public void setParcelas(List<Parcela> parcelas) {
        this.parcelas = parcelas;
    }

    public List<Liquidacao> getLiquidacoes() {
        return liquidacoes;
    }

    public void setLiquidacoes(List<Liquidacao> liquidacoes) {
        this.liquidacoes = liquidacoes;
    }
}

@Entity @Table(name="Liquidacao", catalog="classicmodels")
class Liquidacao {
    @Id
    public Long id;

    @Column(name="valor_total")
    public BigDecimal valorTotal;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contrato_id")
    public Contrato contrato;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parcela_id") @JsonIgnoreProperties("contrato")
    public Parcela parcela;
}

@Entity @Table(name="Parcela", catalog="classicmodels")
class Parcela {
    @Id
    public Long id;

    @Column(name="numero_parcela")
    public Integer numeroParcela;  

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contrato_id")
    public Contrato contrato;
}
