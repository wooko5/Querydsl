package study.querydsl.repository;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("순수 JPA 기본 테스트")
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member foundMember = memberRepository.findById(member.getId()).get();
        assertThat(foundMember).isEqualTo(member);

        List<Member> members1 = memberRepository.findAll();
        assertThat(members1).hasSize(1);

        List<Member> members2 = memberRepository.findByUsername("member1");
        assertThat(members2.get(0).getUsername()).isEqualTo("member1");
    }
}