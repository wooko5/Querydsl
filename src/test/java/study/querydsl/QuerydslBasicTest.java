package study.querydsl;

import com.querydsl.core.QueryResults;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager entityManager; //EntityManager는 멀티쓰레드 환경에서도 사용가능

    JPAQueryFactory jpaQueryFactory; //JPAQueryFactory를 필드로 선언해도 멀티쓰레드 환경(동시성 문제)에서도 사용가능

    @BeforeEach
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(entityManager);
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
    public void startQuerydslV1() {
        QMember qMember = new QMember("test"); //variable은 alias(별칭)를 의미(같은 테이블을 조인해서 alias룰 다르게 설정할 때 필요!!!

        Member foundMember = jpaQueryFactory
                .select(qMember) //QMember.member를 static으로 선언
                .from(qMember)
                .where(qMember.username.eq("member1"))
                .fetchOne();
        assert foundMember != null;
        assertThat(foundMember.getAge()).isEqualTo(10);
    }

    @Test
    public void startQuerydslV2() {
        Member foundMember = jpaQueryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assert foundMember != null;
        assertThat(foundMember.getAge()).isEqualTo(10);
    }

    @Test
    public void search() {
        Member foundMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchBetween() {
        List<Member> foundMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.between(10, 30))
                .fetch();
        assertThat(foundMember.size()).isEqualTo(3);
    }

    @Test
    public void searchAndParam() {
        Member foundMember = jpaQueryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"), //쉼표만 작성해도 and와 같은 코드가 됨
                        member.age.between(10, 30)
                )
                .fetchOne();
        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {
//        List<Member> fetch = jpaQueryFactory
//                .selectFrom(member)
//                .fetch();

//        Member fetchOne = jpaQueryFactory
//                .selectFrom(member)
//                .fetchOne();

//        Member fetchFirst = jpaQueryFactory
//                .selectFrom(member)
//                .fetchFirst();

        QueryResults<Member> results = jpaQueryFactory
                .selectFrom(member)
                .fetchResults(); //deprecated: groupby, having 절 둥 복잡한 페이징 SQL 문에서 예외가 발생함

        results.getTotal();
        List<Member> contents = results.getResults();

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        entityManager.persist(new Member(null, 100));
        entityManager.persist(new Member("member5", 100));
        entityManager.persist(new Member("member6", 100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(
                        member.age.desc(),
                        member.username.asc().nullsLast()
                )
                .fetch();

        assertThat(result.size()).isEqualTo(3);
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2); //nullsLast() 옵션 때문에 사용자이름이 null이면 마지막

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }
}
