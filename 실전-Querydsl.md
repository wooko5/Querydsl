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
     - fetchCount() : count 쿼리로 변경해서 count 수 조회

   - 정렬

     - 

   - 페이징

   - 집합

   - 조인

     - 기본조인
     - ON 절
     - Fetch 조인

   - 서브쿼리

   - Case문

   - 상수, 문자 더하기

4. 중급 문법

5. 실무 활용 - 순수 JPA와 QueryDsl

6. 실무 활용 - 스프링 데이터 JPA와 QueryDsl

7. 스프링 데이터 JPA가 제공하는 QueryDsl 기능
