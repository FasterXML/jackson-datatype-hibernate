package tools.jackson.datatype.hibernate7.data;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import org.hibernate.envers.Audited;

/**
 * Envers-audited parent entity for testing bidirectional graph serialization
 * with {@code FORCE_LAZY_LOADING} (see [datatype-hibernate#204]).
 */
@Entity
@Audited
public class AuditedParent
{
    @Id
    @GeneratedValue
    public Integer id;

    public String name;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    public List<AuditedChild> children = new ArrayList<>();

    protected AuditedParent() { }

    public AuditedParent(String name) {
        this.name = name;
    }
}
