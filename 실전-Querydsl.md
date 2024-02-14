## 실전! QueryDsl



1. 프로젝트 환경설정

   - 프로젝트 생성

     - ```yaml
       Project name: Querydsl
       Project: Gradle - Groovy Project
       사용 기능: Spring Web, jpa, h2, lombok
       SpringBootVersion: 3.2.2
       groupId: study
       artifactId: querydsl
       ```

   - build.gradle

     - ```groovy
       plugins {
       	id 'java'
       	id 'org.springframework.boot' version '3.2.2'
       	id 'io.spring.dependency-management' version '1.1.4'
       }
       
       group = 'study'
       version = '0.0.1-SNAPSHOT'
       
       java {
       	sourceCompatibility = '17'
       }
       
       configurations {
       	compileOnly {
       		extendsFrom annotationProcessor
       	}
       }
       
       repositories {
       	mavenCentral()
       }
       
       dependencies {
       	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
       	implementation 'org.springframework.boot:spring-boot-starter-web'
       	implementation 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.9.0'
       	compileOnly 'org.projectlombok:lombok'
       	runtimeOnly 'com.h2database:h2'
       	annotationProcessor 'org.projectlombok:lombok'
       	testImplementation 'org.springframework.boot:spring-boot-starter-test'
       
       	//test 롬복 사용
       	testCompileOnly 'org.projectlombok:lombok'
       	testAnnotationProcessor 'org.projectlombok:lombok'
       
       	//QueryDsl 추가
       	implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
       	annotationProcessor "com.querydsl:querydsl-apt:${dependencyManagement.importedProperties['querydsl.version']}:jakarta"
       	annotationProcessor "jakarta.annotation:jakarta.annotation-api"
       	annotationProcessor "jakarta.persistence:jakarta.persistence-api"
       }
       
       tasks.named('test') {
       	useJUnitPlatform()
       }
       
       // clean시, 해당 경로의 파일 삭제
       clean {
       	delete file('src/main/generated')
       }
       
       // querydsl QClass 파일 생성 위치를 지정
       tasks.withType(JavaCompile) {
       	options.getGeneratedSourceOutputDirectory().set(file('src/main/generated'))
       }
       
       ```

   - QueryDsl 설정과 검증

     - TIP
       - /src/main/generated에 있는 Q파일들은 git에 올리지 않아야함
       - 스프링을 다른 프레임워크로 바꿀 생각이라면 EntityManager를 주입받을 때, @Autowired보다 @PersistenceContext을 추천
         - @PersistenceContext는 스프링에 종속적이지 않은 java의 어노테이션

   - 라이브러리 살펴보기

     - gradle 의존관계 보기

       - ```groovy
         ./gradlew dependencies --configuration compileClasspath
         ```

     - Querydsl 라이브러리 살펴보기

       - querydsl-apt: Querydsl : 관련 코드 생성 기능 제공
       - querydsl-jpa: querydsl : 라이브러리

   - H2 DB 설치

     - 경로
       - `C:\Program Files (x86)\H2\bin`
     - 관리자 권한으로 git-bash 열고 `chmod 755 h2.sh`
       - ![image-20240124013713101](https://github.com/wooko5/Querydsl/assets/58154633/fcc5651e-3cf9-404a-8cf2-e26fdf46be9e)
     - H2  접속 방법
       - 최초 접속 : `jdbc:h2:~/querydsl `
       - 이후에는 `jdbc:h2:tcp://localhost/~/querydsl`
       - ![image-20240124014653119](https://github.com/wooko5/Querydsl/assets/58154633/4513f675-cf98-4383-82f4-a951bf523fb9)
     
   - 스프링부트 설정 -JPA, DB

     - application.yml

       - ```yaml
         spring:
           datasource:
             url: jdbc:h2:tcp://localhost/~/querydsl
             username: sa
             password:
             driver-class-name: org.h2.Driver
           jpa:
             hibernate:
               ddl-auto: create
             properties:
               hibernate:
         #        show_sql: true # System.out 에 하이버네이트 실행 SQL을 남긴다, 밑의 옵션과 같이 사용하면 중복이기에 주석처리
                 format_sql: true # logger를 통해 하이버네이트 실행 SQL을 남긴다
         
         logging.level:
           org.hibernate.SQL: debug
         #  org.hibernate.type: trace
         ```

2. 예제 도메인 모델

3. 기본 문법

   - JPQL VS Querydsl

     - JPQL 테스트 코드

       - ```java
         @Test
         public void startJPQL() {
             Member foundMember = entityManager.createQuery(
                             "select m from Member m where m.username = :username", Member.class
                     )
                     .setParameter("username", "member1")
                     .getSingleResult();
             assertThat(foundMember.getAge()).isEqualTo(10);
         }
         ```

     - Querydsl 테스트 코드

       - ```java
         import static study.querydsl.entity.QMember.member;
         
         @Test
         public void startQuerydsl() {
             JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
             QMember qMember = new QMember("test"); //variable은 alias(별칭)를 의미(크게 중요하진 X)
         
             Member foundMember = queryFactory
                     .select(qMember) //QMember.member를 static으로 선언
                     .from(qMember)
                     .where(qMember.username.eq("member1"))
                     .fetchOne();
             assert foundMember != null;
             assertThat(foundMember.getAge()).isEqualTo(10);
         }
         ```

   - 기본 Q-Type 활용

     - 코드 축약

       - ```java
         @Test
         public void startQuerydslV1() {
             jpaQueryFactory = new JPAQueryFactory(entityManager);
             QMember qMember = new QMember("test"); //variable은 alias(별칭)를 의미(같은 테이블을 조인해서 alias룰 다르게 설정할 때 필요!!!
         
             Member foundMember = jpaQueryFactory
                     .select(qMember) //QMember.member를 static으로 선언
                     .from(qMember)
                     .where(qMember.username.eq("member1"))
                     .fetchOne();
             assert foundMember != null;
             assertThat(foundMember.getAge()).isEqualTo(10);
         }
         ```

   - 검색 조건 쿼리

     - 코드

       - ```java
         @Test
         public void searchAndParam() {
             jpaQueryFactory = new JPAQueryFactory(entityManager);
         
             Member foundMember = jpaQueryFactory
                     .selectFrom(member)
                     .where(
                             member.username.eq("member1"), //쉼표만 작성해도 and와 같은 코드가 됨
                             member.age.between(10, 30)
                     )
                     .fetchOne();
             assertThat(foundMember.getUsername()).isEqualTo("member1");
         }
         ```

   - 결과조회

     - fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환

     - fetchOne() : 단 건 조회
       - 결과가 없으면 : null
       - 결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
       
     - fetchFirst() : limit(1).fetchOne()

     - fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
       - 페이징 쿼리가 복잡해질 때, 데이터를 가져오는 쿼리랑 토탈 개수 쿼리가 성능 최적화 때문에 다를 수 있음
       - **페이징 쿼리가 복잡하거나 성능이 중요한 쿼리에서는 fetchResults()를 사용하면 X**
       - **deprecated! ==> IntelliJ에서는 fetch()를 대신 사용하길 권유**
       
     - fetchCount() : count 쿼리로 변경해서 count 수 조회

     - 코드

       - ```java
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
         ```

         

   - 정렬

     - 코드

       - ```java
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
         ```

   - 페이징

     - 코드

       - ```java
         @Test
         public void paging1() {
             List<Member> result = queryFactory
                 .selectFrom(member)
                 .orderBy(member.username.desc())
                 .offset(1) //0부터 시작(zero index)
                 .limit(2) //최대 2건 조회
                 .fetch();
             assertThat(result.size()).isEqualTo(2);
         }
         ```

   - 집계

     - aggregation 코드

       - ```java
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
         ```

     - groupBy 코드

       - ```java
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
         ```

   - 조인

     - 기본조인

       - ```java
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
         ```

     - ON 절

       - ```java
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
         ```

       - TIP

         - `inner join이면 where문을 이용하고, outer join을 써야하는 경우에만 'on'을 사용`

       - 주의

         - 일반조인
           - `leftJoin(member.team, team)`
         - on조인
           - `leftJoin(team).on(member.username.eq(team.name))`

     - Fetch 조인 ***

       - 개념

         - `즉시 로딩으로 Member, Team SQL 쿼리 조인으로 한번에 조회`

       - 사용법

         - join(), leftJoin() 등 조인 기능 뒤에 fetchJoin() 이라고 추가 (매우 간단)

       - 코드

         - ```java
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
           ```

   - 서브쿼리

     - 코드

       - ```java
         import static com.querydsl.jpa.JPAExpressions.select;
         
         /**
         * 나이가 평균 이상인 회원을 조회
         */
         @Test
         @DisplayName("서브쿼리 테스트 - 나이가 평균 이상인 회원을 조회")
         public void subQueryV2() {
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
         ```

     - TIP

       - gt : >
       - goe : >=
       - lt : <
       - loe <=

     - JPA JPQL 서브쿼리의 한계

       - ```
         JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 
         당연히 Querydsl도 지원하지 않는다. 
         하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. 
         Querydsl도 하이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한다.
         ```

     - 3가지 해결방법

       - 서브쿼리를 join으로 변경한다.
         - 가능한 상황도 있고, 불가능한 상황도 있다.
       - 애플리케이션에서 쿼리를 2~3번 분리해서 실행한다.
       - nativeSQL을 사용한다.

     - TIP

       - 개발하다보면 화면이나 특정 로직에서 원하는 데이터 형태를 맞추기 위해서 어쩔 수 없이 from절에 서브쿼리를 넣는 경우가 생김
       - 그러나 DB는 데이터만 필터링/그룹핑만 해서 가져오고 로직이나 화면 맞춤용 데이터는 해당 레이어에서 수정하도록 하는 것을 권장
         - DB는 최대한 데이터를 가져오는 용도로만 사용하길 추천

   - Case문

     - TIP
       - case when문을 쓸 수 있지만 DB에 과부하를 막기 위해 DB가 아닌 어플리케이션단에서 데이터를 전환/수정하는 것을 추천

   - 상수, 문자 더하기

4. 중급 문법

5. 실무 활용 - 순수 JPA와 QueryDsl

6. 실무 활용 - 스프링 데이터 JPA와 QueryDsl

7. 스프링 데이터 JPA가 제공하는 QueryDsl 기능
