package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslIntermediateTest {

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

    @Test
    @DisplayName("수정 벌크 연산 테스트")
//    @Commit
    public void bulkUpdate() {
        long count = jpaQueryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute(); //해당 코드는 영속성 컨텍스트의 상태(1차 캐시)를 신경쓰지 않고 DB에 직접 쿼리를 날림(영속성 컨텍스트 != DB)

        //영속성 컨텍스트와 DB의 정보가 다를 때, 영속성 컨텍스트가 우선권을 가짐
        entityManager.flush(); //DB와 영속성 컨텍스트 차이를 없애기 위해 flush()로 현재 영속성 컨텍스트 상태를 DB와 동기화
        entityManager.clear(); //영속성 컨텍스트 상태를 초기화

        assertThat(count).isEqualTo(2);

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        result.forEach(System.out::println); //DB와 영속성 컨텍스트 차이가 없는지 확인
    }

    @Test
    @DisplayName("모든 회원의 나이에 1살 더하기 bulk 테스트")
    public void bulkAdd() {
        long count = jpaQueryFactory
                .update(member)
                .set(member.age, member.age.add(1)) //만약 한 살 빼고 싶으면 add(-1) 하면 됨
//                .set(member.age, member.age.multiply(2)) //나이 곱하기 2
                .execute();

        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("18살 이상의 모든 회원을 삭제 bulk 테스트")
    public void bulkDelete() {
        long count = jpaQueryFactory
                .delete(member)
                .where(member.age.goe(18))
                .execute();

        assertThat(count).isEqualTo(3);
    }
}
