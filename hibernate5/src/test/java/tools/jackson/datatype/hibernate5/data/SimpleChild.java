package tools.jackson.datatype.hibernate5.data;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Simple child entity for {@link SimpleParent}.
 */
@Entity
public class SimpleChild
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Integer id;

    public String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    public SimpleParent parent;

    protected SimpleChild() { }

    public SimpleChild(String label, SimpleParent parent) {
        this.label = label;
        this.parent = parent;
    }
}
