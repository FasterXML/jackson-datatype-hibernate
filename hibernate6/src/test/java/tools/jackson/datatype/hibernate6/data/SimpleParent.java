package tools.jackson.datatype.hibernate6.data;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

/**
 * Simple entity with one lazy List collection (bag), used to test
 * {@code convertToJavaCollection()} with {@code REPLACE_PERSISTENT_COLLECTIONS}.
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
