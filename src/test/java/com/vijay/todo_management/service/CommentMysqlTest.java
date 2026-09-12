package com.vijay.todo_management.service;

import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.repository.CommentRepository;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in real MySQL checks. URL must point to an EMPTY disposable schema.
 * Run with -Dcomment.test.jdbc.url=jdbc:mysql://127.0.0.1:33328/comments_test
 * Tables are created without dropping anything; never use an application schema.
 */
@EnabledIfSystemProperty(named="comment.test.jdbc.url", matches=".+")
class CommentMysqlTest {
    static EntityManagerFactory emf;
    static DriverManagerDataSource ds;
    static UUID author = UUID.randomUUID();
    @BeforeAll static void setup() throws Exception {
        ds = new DriverManagerDataSource(System.getProperty("comment.test.jdbc.url"), "root", "");
        try (Connection connection = ds.getConnection(); Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE users(id char(36) PRIMARY KEY) ENGINE=InnoDB");
            s.execute("CREATE TABLE todos(id char(36) PRIMARY KEY) ENGINE=InnoDB");
            s.execute("INSERT INTO users VALUES ('" + author + "')");
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("src/main/resources/db/migration/V14__todo_comments.sql"));
        }
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setPackagesToScan("com.vijay.todo_management.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","none",
                "hibernate.type.preferred_uuid_jdbc_type","CHAR"));
        factory.afterPropertiesSet();
        emf = factory.getObject();
    }
    @AfterAll static void close() { if (emf != null) emf.close(); }
    UUID seed(int count) throws Exception {
        UUID todo = UUID.randomUUID();
        try (var con = ds.getConnection(); var s = con.createStatement()) {
            s.execute("INSERT INTO todos VALUES ('" + todo + "')");
            String parent = "NULL";
            for (int i=0;i<count;i++) {
                String id = UUID.randomUUID().toString();
                s.execute("INSERT INTO comments(id,todo_id,author_id,parent_comment_id,content_json,content_plain_text,created_at,updated_at) VALUES ('"
                        + id + "','" + todo + "','" + author + "'," + parent
                        + ",'{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}','Original',NOW(6),NOW(6))");
                parent = "'" + id + "'";
            }
        }
        return todo;
    }
    UUID first(UUID todo) throws Exception {
        try (var con=ds.getConnection(); var s=con.createStatement();
                var r=s.executeQuery("SELECT id FROM comments WHERE todo_id='" + todo + "' AND parent_comment_id IS NULL")) {
            assertTrue(r.next()); return UUID.fromString(r.getString(1));
        }
    }
    @Test void deepThreadCleanupUsesActualRepositoryAndMigration() throws Exception {
        UUID todo = seed(25);
        var em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            // Same lock/cleanup ordering as TodoService; minimal fixture has no Todo columns beyond id.
            em.createNativeQuery("SELECT id FROM todos WHERE id = :id FOR UPDATE", String.class)
                    .setParameter("id",todo.toString()).getSingleResult();
            var repo = new JpaRepositoryFactory(em).getRepository(CommentRepository.class);
            assertEquals(25,repo.detachParentsForTodo(todo));
            em.createNativeQuery("DELETE FROM todos WHERE id = :id").setParameter("id",todo.toString()).executeUpdate();
            em.getTransaction().commit();
            try(var con=ds.getConnection(); var s=con.createStatement();
                    var r=s.executeQuery("SELECT COUNT(*) FROM comments WHERE todo_id='" + todo + "'")) {
                r.next(); assertEquals(0,r.getInt(1));
            }
        } finally { em.close(); }
    }
    @Test void softDeletePreservesDatabaseContentAndReplies() throws Exception {
        UUID todo=seed(3), id=first(todo);
        var em=emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Comment c=em.find(Comment.class,id);
            var original=c.getContentJson();
            c.setDeleted(true); c.setDeletedAt(java.time.LocalDateTime.now());
            c.setDeletedBy(em.getReference(User.class,author));
            em.getTransaction().commit();
            try(var con=ds.getConnection(); var s=con.createStatement();
                    var r=s.executeQuery("SELECT content_json,content_plain_text,is_deleted,deleted_at,deleted_by,version FROM comments WHERE id='" + id + "'")) {
                r.next();
                assertEquals(original,new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.getString(1)));
                assertEquals("Original",r.getString(2)); assertTrue(r.getBoolean(3));
                assertNotNull(r.getTimestamp(4)); assertEquals(author.toString(),r.getString(5)); assertEquals(1,r.getInt(6));
            }
            try(var con=ds.getConnection(); var s=con.createStatement();
                    var r=s.executeQuery("SELECT COUNT(*) FROM comments WHERE todo_id='" + todo + "' AND is_deleted=false")) {
                r.next(); assertEquals(2,r.getInt(1));
            }
        } finally { em.close(); }
    }
    @Test void staleEditCannotUndoCommittedDeletion() throws Exception {
        UUID id=first(seed(1));
        var editor=emf.createEntityManager(); var deleter=emf.createEntityManager();
        try {
            editor.getTransaction().begin();
            Comment stale=editor.find(Comment.class,id);
            deleter.getTransaction().begin();
            Comment current=deleter.find(Comment.class,id); current.setDeleted(true);
            deleter.getTransaction().commit();
            stale.setContentPlainText("Stale edit");
            assertThrows(OptimisticLockException.class,editor::flush);
            editor.getTransaction().rollback();
            deleter.clear();
            assertTrue(deleter.find(Comment.class,id).isDeleted());
            assertEquals("Original",deleter.find(Comment.class,id).getContentPlainText());
        } finally {
            if(editor.getTransaction().isActive()) editor.getTransaction().rollback();
            editor.close(); deleter.close();
        }
    }
}
