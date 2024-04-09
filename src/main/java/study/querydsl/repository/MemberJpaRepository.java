package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.member;

@Repository
public class MemberJpaRepository {

    private final EntityManager entityManager;
    private final JPAQueryFactory jpaQueryFactory;

    /**
     * JPAQueryFactory의 동시성 문제는 EntityManager에 의존함
     * EntityManager는 스프링과 함께 사용할 때, 동시성 문제에 상관없이 트랜잭션 단위로 분리되어 실행됨
     * 그래서 동시성 문제가 발생하지 않음
     */
    public MemberJpaRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.jpaQueryFactory = new JPAQueryFactory(entityManager); //this.jpaQueryFactory = jpaQueryFactory; 보다 더 나은 코드
    }

    public void save(Member member) {
        entityManager.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member member = entityManager.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public Member findByIdQueryDsl(Long id) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(member.id.eq(id))
                .fetchOne();
    }

    public List<Member> findAll() {
        return entityManager.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAllQueryDsl() {
        return jpaQueryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByName(String name) {
        return entityManager.createQuery("select m from Member m where username = :username", Member.class)
                .setParameter("username", name)
                .getResultList();
    }

    public List<Member> findByNameQueryDsl(String name) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq(name))
                .fetch();
    }
}
