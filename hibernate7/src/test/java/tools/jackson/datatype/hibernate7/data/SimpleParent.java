package tools.jackson.datatype.hibernate7.data;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

/**
 * Simple entity with one lazy collection, used to test that
 * {@code findLazyValue()} does not open a session for already-initialized
 * collections.
 */
@Entity
public class SimpleParent
{
    @Id
    @GeneratedValue
    public Integer id;

    public String name;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    public List<SimpleChild> children = new ArrayList<>();

    protected SimpleParent() { }

    public SimpleParent(String name) {
        this.name = name;
    }
}
