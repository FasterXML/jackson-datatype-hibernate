package tools.jackson.datatype.hibernate7.data;

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
}
