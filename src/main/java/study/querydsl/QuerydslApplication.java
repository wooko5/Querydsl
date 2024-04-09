package study.querydsl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuerydslApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuerydslApplication.class, args);
    }

//	@Bean //JPAQueryFactory를 해당 코드처럼 미리 등록해놓고 사용해도 됨
//	JPAQueryFactory jpaQueryFactory(EntityManager entityManager){
//		return new JPAQueryFactory(entityManager);
//	}
}
