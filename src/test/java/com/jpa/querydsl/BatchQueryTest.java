package com.jpa.querydsl;

import com.jpa.querydsl.entity.Member;
import com.jpa.querydsl.entity.QMember;
import com.jpa.querydsl.entity.Team;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static com.jpa.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
@Slf4j
public class BatchQueryTest {

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
    @Commit
    void bulkUpdate() {
        // 쿼리 한번으로 대량의 데이터를 수정할 때 사용
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        
        em.flush();
        em.clear();
        // 지연 쓰기와 초기화를 실행한 후 조회해야 한다.

        // 영속성 컨텍스트를 패싱하고 바로 쿼리가 날라가게 된다.
        List<Member> result = queryFactory.selectFrom(member)
                .fetch();

        for (Member entity : result) {
            System.out.println("entity = " + entity);
        }
    }

    @Test
    @Commit
    void bulkAdd() {
        // 쿼리 한번으로 대량의 데이터를 수정할 때 사용
        long count = queryFactory
                .update(member)
                // 빼기는 음수로 파라미터 입력
                .set(member.age, member.age.add(1))
                .execute();

        em.flush();
        em.clear();
    }

    @Test
    @Commit
    void bulkMultiply() {
        // 쿼리 한번으로 대량의 데이터를 수정할 때 사용
        long count = queryFactory
                .update(member)
                // 곱하기
                .set(member.age, member.age.multiply(2))
                .execute();

        em.flush();
        em.clear();
    }

    @Test
    @Commit
    void bulkDelete() {
        // 대량 삭제
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        em.flush();
        em.clear();
    }

    // ------- SQL function
    @Test
    void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
        
        // DB Dialect에 등록이 돼있어야함(H2Dialect)
    }

    @Test
    void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                // .where(member.username.eq(Expressions.stringTemplate(
                //         "function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
