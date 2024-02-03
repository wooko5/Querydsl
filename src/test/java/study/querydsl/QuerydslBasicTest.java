package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
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
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager entityManager; //EntityManager는 멀티쓰레드 환경에서도 사용가능

    JPAQueryFactory jpaQueryFactory; //JPAQueryFactory를 필드로 선언해도 멀티쓰레드 환경(동시성 문제)에서도 사용가능

    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

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

    @Test
    public void pagingV1() {
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    /**
     * 실무에서는 Tuple을 직접 쓰는 방법보다
     * DTO로 바로 조회하는 방법을 더 많이 쓴다
     */
    @Test
    public void aggregation() {
        List<Tuple> result = jpaQueryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀명과 각 팀의 평균 연령을 구하는 테스트
     */
    @Test
    public void groupBy() {
        List<Tuple> result = jpaQueryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamB.get(team.name)).isEqualTo("teamB");

        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 직원(join)
     */
    @Test
    public void basicJoin() {
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
//                .join(member.team, team) //inner join
                .leftJoin(member.team, team)
//                .rightJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 즉, 모든 회원과 팀의 모든 데이터를 조인해서(catesian)
     * where절의 조건으로 필터링 하는 조인
     */
    @Test
    public void thetaJoin() {
        entityManager.persist(new Member("teamA"));
        entityManager.persist(new Member("teamB"));
        List<Member> result = jpaQueryFactory
                .select(member)
                .from(member, team).where(member.username.eq(team.name))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * inner join이면 where문을 이용하고,
     * 어쩔 수 없이 outer join을 써야하는 경우에만 'on'을 사용하자
     */
    @Test
    public void joinOnFiltering() {
        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA")) //on을 정해주지 않으면 member.team.id(FK) = team.id(PK)를 사용
//                .join(member.team, team) //해당 절과 똑같음
//                .where(team.name.eq("teamA"))
                .fetch();

        result.forEach(System.out::println);
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상을 외부 조인
     */
    @Test
    public void joinOnNoRelation() {
        entityManager.persist(new Member("teamA"));
        entityManager.persist(new Member("teamB"));
        entityManager.persist(new Member("teamC"));

        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team) //member의 teamId와 team의 teamId를 기준으로 join
                .leftJoin(team).on(member.username.eq(team.name)) // 회원 이름과 팀 이름으로만 left-join
//                .join(team).on(member.username.eq(team.name)) // 회원 이름과 팀 이름으로만 inner-join
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void noFetchJoin() {
        entityManager.flush();
        entityManager.clear();

        Member foundMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        assert foundMember != null;
        boolean loaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(foundMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    @Test
    public void fetchJoin() {
        entityManager.flush();
        entityManager.clear();

        Member foundMember = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        assert foundMember != null;
        boolean loaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(foundMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }
}
