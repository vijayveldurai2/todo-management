package com.vijay.todo_management.service;
import com.vijay.todo_management.entity.Todo;
import com.vijay.todo_management.repository.TodoRepository;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Uses only an explicitly supplied EMPTY disposable MySQL schema. */
@EnabledIfSystemProperty(named="subtask.test.jdbc.url",matches=".+")
class SubtaskMysqlTest {
    static DriverManagerDataSource ds;
    static EntityManagerFactory emf;
    static UUID project=UUID.randomUUID();
    @BeforeAll static void setup() throws Exception {
        ds=new DriverManagerDataSource(System.getProperty("subtask.test.jdbc.url"),"root","");
        try(var con=ds.getConnection(); var s=con.createStatement()) {
            // Pre-V16 fixture with all Todo scalar columns; related entities stay lazy.
            s.execute("CREATE TABLE todos(id char(36) PRIMARY KEY,project_id char(36),display_id varchar(20),"
                    +"title varchar(255),description_json json,description_plain_text longtext,priority varchar(20),"
                    +"status_id char(36),sprint_id char(36),position int,start_date_time datetime,end_date_time datetime,"
                    +"due_date datetime,estimated_time decimal(8,2),remaining_time decimal(8,2),story_points int,"
                    +"created_date datetime(6),modified_date datetime(6)) ENGINE=InnoDB");
            ScriptUtils.executeSqlScript(con,new FileSystemResource("src/main/resources/db/migration/V16__todo_subtasks.sql"));
        }
        var factory=new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds); factory.setPackagesToScan("com.vijay.todo_management.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","none","hibernate.type.preferred_uuid_jdbc_type","CHAR"));
        factory.afterPropertiesSet(); emf=factory.getObject();
    }
    @AfterAll static void close() { if(emf!=null) emf.close(); }
    UUID insert(UUID parent) throws Exception {
        UUID id=UUID.randomUUID();
        try(var con=ds.getConnection(); var s=con.prepareStatement(
                "INSERT INTO todos(id,project_id,parent_todo_id,title,priority,position,created_date,modified_date) VALUES (?,?,?,'Task','MEDIUM',0,NOW(6),NOW(6))")) {
            s.setString(1,id.toString()); s.setString(2,project.toString());
            s.setString(3,parent==null?null:parent.toString()); s.executeUpdate();
        }
        return id;
    }
    @Test void migrationProtectsChildrenAndPromotionPreservesDescendants() throws Exception {
        UUID parent=insert(null),child=insert(parent),grandchild=insert(child);
        try(var con=ds.getConnection(); var s=con.createStatement()) {
            assertThrows(SQLException.class,()->s.executeUpdate("DELETE FROM todos WHERE id='"+parent+"'"));
        }
        var em=emf.createEntityManager();
        try {
            em.getTransaction().begin();
            var repo=new JpaRepositoryFactory(em).getRepository(TodoRepository.class);
            assertEquals(child,repo.findByProject_IdAndParentTodo_IdOrderByCreatedDateAscIdAsc(project,parent).get(0).getId());
            Todo subtask=repo.findForCommentWrite(child,project).orElseThrow();
            repo.promoteToRoot(subtask.getId(),project,java.time.LocalDateTime.now());
            em.createNativeQuery("DELETE FROM todos WHERE id=:id").setParameter("id",parent.toString()).executeUpdate();
            em.getTransaction().commit();
            em.clear();
            assertNull(em.find(Todo.class,child).getParentTodo());
            assertEquals(child,em.find(Todo.class,grandchild).getParentTodo().getId());
        } finally { if(em.getTransaction().isActive())em.getTransaction().rollback(); em.close(); }
    }
    @Test void lockingChildCheckSeesCommitAfterTransactionSnapshot() throws Exception {
        UUID parent=insert(null);
        var em=emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("SELECT COUNT(*) FROM todos",Long.class).getSingleResult(); // Establish snapshot.
            UUID child=insert(parent); // Another connection commits a child after that snapshot.
            var repo=new JpaRepositoryFactory(em).getRepository(TodoRepository.class);
            repo.findForCommentWrite(parent,project).orElseThrow();
            var children=repo.findChildrenForDeletion(parent,PageRequest.of(0,1));
            assertEquals(1,children.size()); assertEquals(child,children.get(0).getId());
            em.getTransaction().rollback();
        } finally { if(em.getTransaction().isActive())em.getTransaction().rollback(); em.close(); }
    }

    @Test void staleOrdinaryEditCannotRestorePromotedParent() throws Exception {
        UUID parent=insert(null), child=insert(parent);
        var editor=emf.createEntityManager(); var promoter=emf.createEntityManager();
        try {
            editor.getTransaction().begin();
            Todo stale=editor.find(Todo.class,child);
            promoter.getTransaction().begin();
            var repo=new JpaRepositoryFactory(promoter).getRepository(TodoRepository.class);
            repo.findForCommentWrite(child,project).orElseThrow();
            repo.promoteToRoot(child,project,java.time.LocalDateTime.now());
            promoter.getTransaction().commit();
            stale.setTitle("Edited concurrently");
            editor.getTransaction().commit();
            editor.clear();
            Todo reloaded=editor.find(Todo.class,child);
            assertNull(reloaded.getParentTodo()); assertEquals("Edited concurrently",reloaded.getTitle());
        } finally {
            if(editor.getTransaction().isActive())editor.getTransaction().rollback();
            if(promoter.getTransaction().isActive())promoter.getTransaction().rollback();
            editor.close(); promoter.close();
        }
    }
}
