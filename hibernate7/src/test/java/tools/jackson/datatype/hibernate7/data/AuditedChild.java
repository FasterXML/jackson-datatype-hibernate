package tools.jackson.datatype.hibernate7.data;

import java.util.Objects;

import jakarta.persistence.*;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * Envers-audited child entity with a {@code @ManyToOne} back-reference to
 * {@link AuditedParent} using {@code targetAuditMode = NOT_AUDITED}.
 * <p>
 * On Hibernate ORM 7.3+ this causes Envers to materialize current
 * session-attached entities (live {@code PersistentBag}) instead of historic
 * audit-table data, which interacts with {@code FORCE_LAZY_LOADING} to
 * produce unbounded recursion (see [datatype-hibernate#204]).
 * <p>
 * Note: {@code equals}/{@code hashCode} are based on {@code label} (business
 * key) to exercise the cycle detector's identity-based tracking — two
 * distinct instances with the same label must both be serialized.
 */
@Entity
@Audited
public class AuditedChild
{
    @Id
    @GeneratedValue
    public Integer id;

    public String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    public AuditedParent parent;

    protected AuditedChild() { }

    public AuditedChild(String label, AuditedParent parent) {
        this.label = label;
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditedChild other)) return false;
        return Objects.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(label);
    }
}
