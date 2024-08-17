package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EntityManager entityManager;

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

    @Test
    public void searchMemberTeamTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");
        List<MemberTeamDto> result = memberRepository.searchMemberTeam(condition);
        assertThat(result).extracting("username").containsExactly("member4");
    }
}