package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager entityManager; //EntityManager는 멀티쓰레드 환경에서도 사용가능

    JPAQueryFactory jpaQueryFactory; //JPAQueryFactory를 필드로 선언해도 멀티쓰레드 환경(동시성 문제)에서도 사용가능

    @BeforeEach
    public void before() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        entityManager.persist(teamA);
        entityManager.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.persist(member3);
        entityManager.persist(member4);
    }

    @Test
    public void startJPQL() {
        Member foundMember = entityManager.createQuery(
                        "select m from Member m where m.username = :username", Member.class
                )
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(foundMember.getAge()).isEqualTo(10);
    }

    @Test
    public void startQuerydsl() {
        jpaQueryFactory = new JPAQueryFactory(entityManager);
        QMember qMember = new QMember("test"); //variable은 alias(별칭)를 의미(크게 중요하진 X)

        Member foundMember = jpaQueryFactory
                .select(qMember)
                .from(qMember)
                .where(qMember.username.eq("member1"))
                .fetchOne();
        assert foundMember != null;
        assertThat(foundMember.getAge()).isEqualTo(10);
    }
}
