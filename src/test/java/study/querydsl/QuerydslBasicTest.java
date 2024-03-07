package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
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
    @DisplayName("기존 JPQL 테스트")
    public void startJPQL() {
        Member foundMember = entityManager.createQuery(
                        "select m from Member m where m.username = :username", Member.class
                )
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(foundMember.getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("Querydsl V1 테스트")
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
    @DisplayName("Querydsl V2 테스트")
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
    @DisplayName("단건 조회 테스트")
    public void search() {
        Member foundMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();
        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("Between1 테스트")
    public void searchBetween() {
        List<Member> foundMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.between(10, 30))
                .fetch();
        assertThat(foundMember.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Between2 테스트")
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
    @DisplayName("fetchResults 테스트 - deprecated")
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
    @DisplayName("sorting 테스트")
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
    @DisplayName("paging 테스트")
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
    @DisplayName("aggregation 테스트")
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
    @DisplayName("groupBy 테스트")
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
    @DisplayName("일반 join 테스트")
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
    @DisplayName("세타 조인(연관관계가 없는 필드로 조인) 테스트")
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
    @DisplayName("join의 on 테스트")
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
    @DisplayName("연관관계가 없는 엔티티 외부 조인 테스트")
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
    @DisplayName("noFetchJoin 테스트")
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
    @DisplayName("fetchJoin 테스트")
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

    /**
     * 나이가 가장 많은 회원을 조회
     */
    @Test
    @DisplayName("서브쿼리 테스트 - 나이가 가장 많은(eq, max) 회원을 조회")
    public void subQueryEq() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        //subQuery 부분
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원을 조회
     */
    @Test
    @DisplayName("서브쿼리 테스트 - 나이가 평균 이상인(goe) 회원을 조회")
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.goe( //goe는 '>='를 의미
                        //subQuery 부분
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * 나이가 10살보다 많은 회원을 모두(in) 조회
     */
    @Test
    @DisplayName("서브쿼리 테스트 - 나이가 10살보다 많은 회원을 모두(in) 조회")
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.in(
                        //subQuery 부분
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    @DisplayName("select 절에 subquery 테스트")
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = jpaQueryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple == " + tuple);
        }
    }

    @Test
    @DisplayName("단순한 조건 case when 테스트")
    public void basicCase() {
        List<String> result = jpaQueryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("result == " + s);
        }
    }

    @Test
    @DisplayName("복잡한 조건 case when 테스트")
    public void complexCase() {
        List<String> result = jpaQueryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0 ~ 20살")
                        .when(member.age.between(21, 30)).then("21 ~ 30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("result == " + s);
        }
    }

    @Test
    @DisplayName("select 문에 상수를 넣는 테스트")
    public void constant() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple == " + tuple);
        }
    }

    @Test
    @DisplayName("select 문에 원하는 문자열을 합성해서 넣는 테스트")
    public void concat() {
        List<String> result = jpaQueryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) //{username_age}, stringValue()를 안 하면 타입에러가 발생
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s == " + s);
        }
    }

    @Test
    @DisplayName("프로젝션 대상이 하나 테스트")
    public void simpleProjection() {
        List<String> result = jpaQueryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for (String res : result) {
            System.out.println(res);
        }
    }

    @Test
    @DisplayName("프로젝션 대상이 둘 이상 테스트")
    public void tupleProjection() {
        List<Tuple> result = jpaQueryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("username == " + tuple.get(member.username));
            System.out.println("age == " + tuple.get(member.age));
        }
    }

    @Test
    @DisplayName("순수 JPA에서 DTO 조회 테스트")
    public void findDtoByJPQL() {
        List<MemberDto> result = entityManager.createQuery(
                        "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                        MemberDto.class
                )
                .getResultList();

        for (MemberDto dto : result) {
            System.out.println("DTO의 사용자명 == " + dto.getUsername());
            System.out.println("DTO의 나이 == " + dto.getAge());
        }
    }

    @Test
    @DisplayName("프로퍼티 접근 - Setter, DTO 조회 테스트")
    public void findDtoBySetter() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch(); // MemberDto에 @NoArgsConstructor를 선언해야 테스트 성공

        for (MemberDto dto : result) {
            System.out.println("DTO == " + dto);
        }
    }

    @Test
    @DisplayName("필드 직접 접근, DTO 조회 테스트")
    public void findDtoByField() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch(); // Projections.fields: getter, setter가 없어도 되는 방식

        for (MemberDto dto : result) {
            System.out.println("DTO == " + dto);
        }
    }

    @Test
    @DisplayName("생성자 사용, DTO 조회 테스트")
    public void findDtoByConstructor() {
        List<MemberDto> result = jpaQueryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch(); // 생성자의 argument 순서와 select문의 column 순서가 일치해야함

        for (MemberDto dto : result) {
            System.out.println("DTO == " + dto);
        }
    }

    @Test
    @DisplayName("별칭이 다를 때, 필드 직접 접근 DTO 조회 테스트")
    public void findUserDtoByFiled() {
        QMember subMember = new QMember("memberSub");
        List<UserDto> result = jpaQueryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(subMember.age.max())
                                        .from(subMember), "age"
                        )))
                .from(member)
                .fetch(); // member.username는 UserDto.name과 매칭이 안 되기 때문 as를 사용

        for (UserDto dto : result) {
            System.out.println("DTO == " + dto);
        }
    }

    @Test
    @DisplayName("별칭이 다를 때, 생성자 사용 DTO 조회 테스트")
    public void findUserDtoByConstructor() {
        List<UserDto> result = jpaQueryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch(); // 생성자 argument 순서에 맞게 들어오기만 하면 되기 때문에 성공

        for (UserDto dto : result) {
            System.out.println("DTO == " + dto);
        }
    }

    /**
     * argument를 잘못 입력하면 컴파일러로 타입을 체크할 수 있으므로 가장 안전한 방법
     * 다만 DTO에 QueryDSL 어노테이션을 유지해야 하는 점과 DTO까지 Q파일을 생성해야 하는 단점이 있음
     * Projections.constructor은 argument를 잘못 입력해도 컴파일 오류가 발생하지 않고 runtime 오류가 발생
     */
    @Test
    @DisplayName("@QueryProjection의 DTO 조회 테스트")
    public void findDtoByQueryProjection() {
        List<MemberDto> result = jpaQueryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch(); // @QueryProjection이 선언된 생성자 argument 순서에 맞게 들어오기만 하면 성공

        for (MemberDto dto : result) {
            System.out.println("DTO == " + dto);
        }
    }

    @Test
    @DisplayName("BooleanBuilder 동적쿼리 테스트")
    public void dynamicQueryByBooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;
        List<Member> result = searchMemberV1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMemberV1(String username, Integer age) {
        BooleanBuilder builder = new BooleanBuilder();
        if (username != null) {
            builder.and(member.username.eq(username));
        }
        if (age != null) {
            builder.and(member.age.eq(age));
        }
        return jpaQueryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    @DisplayName("where 다중 조건 동적쿼리 테스트")
    public void dynamicQueryByWhereMultiParam() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;
        List<Member> result = searchMemberV2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMemberV2(String username, Integer age) {
        return jpaQueryFactory
                .selectFrom(member)
                .where(usernameEq(username), ageEq(age)) //where문에 usernameEq(usernameCond)이 null이면 해당 조건문은 없었던 걸로 취급하기에 동적쿼리 가능
//                .where(usernameAndAgeEq(username, age)) //위의 식과 동일함
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return username != null ? member.username.eq(username) : null;
    }

    private BooleanExpression ageEq(Integer age) {
        return age != null ? member.age.eq(age) : null;
    }

    private BooleanExpression usernameAndAgeEq(String usernameCond, Integer ageCond) {
        return member.username.eq(usernameCond).and(member.age.eq(ageCond)); //두 조건문을 합쳐서 새로운 조립이 가능(composition)
    }
}
