package com.jpa.querydsl;

import com.jpa.querydsl.entity.Member;
import com.jpa.querydsl.entity.QMember;
import com.jpa.querydsl.entity.QTeam;
import com.jpa.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static com.jpa.querydsl.entity.QMember.member;
import static com.jpa.querydsl.entity.QTeam.team;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Slf4j
public class QueryDslBasicTest {

    @PersistenceContext
    EntityManager em;

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


    @Test
    void startJPQL() {
        // mmeber1을 찾아라.
        String mem1 = "member1";

        String qlString =
                "SELECT m " +
                        "FROM Member m " +
                        "WHERE m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", mem1)
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo(mem1);
    }

    @Test
    void startQuerydsl() {
        String mem1 = "member1";

        // 인스턴스 생성방법 두가지
        QMember m = new QMember("m");
        QMember member = QMember.member;

        Member findMember = queryFactory
                .selectFrom(m)
                // 파라미터 바인딩 처리
                .where(m.username.eq(mem1))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo(mem1);
    }

    @Test
    void search() {
        String mem1 = "member1";

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq(mem1)
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo(mem1);

    }

    @Test
    void searchAndParam() {
        String mem1 = "member1";

        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        // and로 조건 설정
                        member.username.eq(mem1), 
                        (member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo(mem1);

    }

    @Test
    void resultFetch() {
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건
        Member findMember1 = queryFactory
                .selectFrom(member)
                .fetchOne();

        //처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();

        //페이징에서 사용
        // 쿼리를 두번 실행(카운트, 컨텐츠)
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    void paging() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                // 1페이지 스킵
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void pagingWithCount() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                // 1페이지 스킵
                .offset(1)
                // 2개씩 페이징
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
        
    }

    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    void group() {
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() {

        QMember sub = new QMember("sub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(sub.age.max())
                                .from(sub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     */
    @Test
    void subQueryGoe() {

        QMember sub = new QMember("sub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(sub.age.avg())
                                .from(sub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 10 초과인 회원 조회
     */
    @Test
    void subQueryIn() {

        QMember sub = new QMember("sub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(sub.age)
                                .from(sub)
                        .where(sub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQuery() {
        QMember sub = new QMember("sub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(sub.age.avg())
                                .from(sub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // JPA JPQL 서브쿼리의 한계점
        // from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.

        // 해결방안
        // 1. 서브쿼리를 join으로 변경(불가능한 상황 존재)
        // 2. 애플리케이션에서 쿼리를 2번 분리해서 실행
        // 3. 네이티브 SQL을 사용한다.
    }

    /**
     * CASE 문
     * 
     * SELECT, WHERE, ORDER BY 에서 사용가능
     */
    @Test
    void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() {
        List<String> fetch = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("미성년자")
                        .when(member.age.between(21, 30)).then("청년")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }


    }

}
