package com.example.demo.repository;

import com.example.demo.entity.Internship;
import com.example.demo.entity.Skill;
import com.example.demo.enums.InternshipStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Dynamic search for open listings using the plain JPA Criteria API
 * (independent of Spring Data's Specification API, which changed in Spring Data JPA 4).
 */
@Repository
public class InternshipSearchRepository {

    private static final Set<String> SORTABLE = Set.of("deadline", "createdAt", "title", "durationWeeks", "id");

    @PersistenceContext
    private EntityManager em;

    /** majorLike + anySkills are OR-ed together (used by "recommended"); the other filters are AND-ed. */
    public record Filter(String field, String location, Integer maxDurationWeeks, LocalDate deadlineBefore,
                         String skill, String majorLike, List<String> anySkills) {}

    public Page<Internship> searchOpen(Filter f, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Internship> cq = cb.createQuery(Internship.class);
        Root<Internship> root = cq.from(Internship.class);
        cq.select(root).where(predicates(cb, cq, root, f).toArray(new Predicate[0]));
        cq.orderBy(orders(cb, root, pageable.getSort()));
        List<Internship> content = em.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Internship> countRoot = countQuery.from(Internship.class);
        countQuery.select(cb.count(countRoot)).where(predicates(cb, countQuery, countRoot, f).toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> predicates(CriteriaBuilder cb, AbstractQuery<?> q, Root<Internship> root, Filter f) {
        List<Predicate> p = new ArrayList<>();
        p.add(cb.equal(root.get("status"), InternshipStatus.APPROVED));
        p.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("deadline"), LocalDate.now()));

        if (StringUtils.hasText(f.field())) {
            p.add(cb.like(cb.lower(root.<String>get("field")), like(f.field())));
        }
        if (StringUtils.hasText(f.location())) {
            p.add(cb.like(cb.lower(root.<String>get("location")), like(f.location())));
        }
        if (f.maxDurationWeeks() != null) {
            p.add(cb.lessThanOrEqualTo(root.<Integer>get("durationWeeks"), f.maxDurationWeeks()));
        }
        if (f.deadlineBefore() != null) {
            p.add(cb.lessThanOrEqualTo(root.<LocalDate>get("deadline"), f.deadlineBefore()));
        }
        if (StringUtils.hasText(f.skill())) {
            p.add(hasAnySkill(cb, q, root, List.of(f.skill().trim().toLowerCase())));
        }

        boolean hasMajor = StringUtils.hasText(f.majorLike());
        boolean hasSkills = f.anySkills() != null && !f.anySkills().isEmpty();
        if (hasMajor || hasSkills) {
            List<Predicate> ors = new ArrayList<>();
            if (hasMajor) ors.add(cb.like(cb.lower(root.<String>get("field")), like(f.majorLike())));
            if (hasSkills) ors.add(hasAnySkill(cb, q, root, f.anySkills()));
            p.add(cb.or(ors.toArray(new Predicate[0])));
        }
        return p;
    }

    /** EXISTS (select 1 from internship i2 join i2.skills s where i2.id = root.id and lower(s.name) in (...)) */
    private Predicate hasAnySkill(CriteriaBuilder cb, AbstractQuery<?> q, Root<Internship> root, List<String> lowerNames) {
        Subquery<Long> sub = q.subquery(Long.class);
        Root<Internship> r2 = sub.from(Internship.class);
        Join<Internship, Skill> skill = r2.join("skills");
        sub.select(r2.<Long>get("id"))
                .where(cb.equal(r2.get("id"), root.get("id")),
                        cb.lower(skill.<String>get("name")).in(lowerNames));
        return cb.exists(sub);
    }

    private List<jakarta.persistence.criteria.Order> orders(CriteriaBuilder cb, Root<Internship> root, Sort sort) {
        List<jakarta.persistence.criteria.Order> out = new ArrayList<>();
        for (Sort.Order o : sort) {
            if (!SORTABLE.contains(o.getProperty())) continue;   // ignore unknown sort fields
            out.add(o.isAscending() ? cb.asc(root.get(o.getProperty())) : cb.desc(root.get(o.getProperty())));
        }
        if (out.isEmpty()) out.add(cb.asc(root.get("deadline")));
        out.add(cb.asc(root.get("id")));                          // stable paging
        return out;
    }

    private static String like(String s) {
        return "%" + s.trim().toLowerCase() + "%";
    }
}