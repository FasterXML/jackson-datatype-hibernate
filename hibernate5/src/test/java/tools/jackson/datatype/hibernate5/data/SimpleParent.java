package tools.jackson.datatype.hibernate5.data;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

/**
 * Simple entity with one lazy List collection (bag).
 */
@Entity
public class SimpleParent
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Integer id;

    public String name;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    public List<SimpleChild> children = new ArrayList<SimpleChild>();

    protected SimpleParent() { }

    public SimpleParent(String name) {
        this.name = name;
    }
}
