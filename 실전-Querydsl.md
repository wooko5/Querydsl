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

2. 예제 도메인 모델

3. 기본 문법

4. 중급 문법

5. 실무 활용 - 순수 JPA와 QueryDsl

6. 실무 활용 - 스프링 데이터 JPA와 QueryDsl

7. 스프링 데이터 JPA가 제공하는 QueryDsl 기능
