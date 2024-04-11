package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    @DisplayName("순수 JPA 기본 테스트")
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member foundMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(foundMember).isEqualTo(member);

        List<Member> members1 = memberJpaRepository.findAll();
        assertThat(members1).hasSize(1);

        List<Member> members2 = memberJpaRepository.findByName("member1");
        assertThat(members2.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("QueryDsl 기본 테스트")
    public void basicQueryDslTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member foundMember = memberJpaRepository.findByIdQueryDsl(member.getId());
        assertThat(foundMember).isEqualTo(member);

        List<Member> members1 = memberJpaRepository.findAllQueryDsl();
        assertThat(members1).hasSize(1);

        List<Member> members2 = memberJpaRepository.findByNameQueryDsl("member1");
        assertThat(members2.get(0).getUsername()).isEqualTo("member1");
    }
}