package com.jpa.querydsl;

import com.jpa.querydsl.entity.Member;
import com.jpa.querydsl.entity.QMember;
import com.jpa.querydsl.entity.QTeam;
import com.jpa.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.jpa.querydsl.entity.QMember.member;
import static com.jpa.querydsl.entity.QTeam.*;
import static com.jpa.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Slf4j
public class QueryDslJoinTest {

    @PersistenceContext
    EntityManager em;

    @PersistenceUnit
    EntityManagerFactory emf;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        // 초기화
        em.flush();
        em.clear();

        List<Member> members = em.createQuery("SELECT m FROM Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println("member=" + member);
            System.out.println("-> member.team=" + member.getTeam());
        }

    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        QMember member = QMember.member;
        QTeam team = QTeam.team;
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) // inner join
                // .leftJoin(member.team, team)
                // .rightJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                // from 절에 여러 엔티티 선택
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        // 외부조인 불가능

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     *
     * JPQL: SELECT m, t
     *      FROM Member m
     *      LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.*
     *      FROM Member m
     *      LEFT JOIN Team t ON m.TEAM_ID = t.id
     *          and t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                // 팀 이름이 teamA인 팀만 조인되면서 회원은 모두 조회
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        // 내부조인 이면 익숙한 where 절로 해결하고,
        // 정말 외부조인이 필요한 경우에만 이 기능을 사용하자.

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        
        // theta 조인(연관관계가 없는 조인)인데 외부조인이 필요할 경우
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                // 연관관계가 아닌 필드로 조인
                .leftJoin(team).on(member.username.eq(team.name))
                // id로 조인까지 추가
                // .leftJoin(member.team, team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    /**
     * 페치조인 미적용
     */
    @Test
    public void noFetchJoin() throws Exception {
        // 영속성컨텍스트를 날린후 결과 확인
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 영속성컨텍스트에서 초기화된 엔티티인지 확인하는 기능
        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    /**
     * 페치조인 적용
     *
     * 페치조인
     * SQL조인을 활용해서 연관된 엔티티를 SQL 한번에 조회하는 기능
     * 주로 성능 최적화에 사용
     */
    @Test
    public void fetchJoin() throws Exception {
        // 영속성컨텍스트를 날린후 결과 확인
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                // 페치조인적용
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        // 영속성컨텍스트에서 초기화된 엔티티인지 확인하는 기능
        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());

        assertThat(loaded).as("페치 조인 적용").isTrue();
    }
    
}
