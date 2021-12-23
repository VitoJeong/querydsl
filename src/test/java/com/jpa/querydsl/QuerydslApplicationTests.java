package com.jpa.querydsl;

import com.jpa.querydsl.entity.Hello;
import com.jpa.querydsl.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Commit
class QuerydslApplicationTests {

    @PersistenceContext
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QHello qHello = new QHello("h");
        // em.clear();
        Hello findEntity = queryFactory
                .selectFrom(qHello)
                .where(qHello.id.eq(hello.getId()))
                .fetchOne();

        assertThat(findEntity).isEqualTo(hello);
        assertThat(findEntity.getId()).isEqualTo(hello.getId());
    }

}
