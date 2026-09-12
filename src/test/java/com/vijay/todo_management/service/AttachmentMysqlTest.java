package com.vijay.todo_management.service;
import com.vijay.todo_management.repository.AttachmentRepository;
import com.vijay.todo_management.storage.LocalAttachmentStorage;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// Explicit opt-in, EMPTY disposable schema only; never uses application datasource settings.
@EnabledIfSystemProperty(named="attachment.test.jdbc.url", matches=".+")
class AttachmentMysqlTest {
    @TempDir Path directory;
    @Test void todoDeletionRetainsCleanupKeyAndCleanerRemovesFileAndMetadata() throws Exception {
        var ds=new DriverManagerDataSource(System.getProperty("attachment.test.jdbc.url"),"root","");
        String user=UUID.randomUUID().toString(),todo=UUID.randomUUID().toString();
        String id=UUID.randomUUID().toString(),key=UUID.randomUUID().toString();
        try(var con=ds.getConnection(); var s=con.createStatement()) {
            s.execute("CREATE TABLE users(id char(36) PRIMARY KEY) ENGINE=InnoDB");
            s.execute("CREATE TABLE todos(id char(36) PRIMARY KEY) ENGINE=InnoDB");
            s.execute("CREATE TABLE workspaces(id char(36) PRIMARY KEY) ENGINE=InnoDB");
            ScriptUtils.executeSqlScript(con,new FileSystemResource("src/main/resources/db/migration/V15__todo_attachments.sql"));
            s.execute("INSERT INTO users VALUES ('"+user+"')");
            s.execute("INSERT INTO todos VALUES ('"+todo+"')");
            s.execute("INSERT INTO todo_attachments(id,todo_id,uploaded_by,storage_key,file_name,size_bytes,created_at)"
                    +" VALUES ('"+id+"','"+todo+"','"+user+"','"+key+"','a.txt',5,NOW(6))");
            s.execute("DELETE FROM todos WHERE id='"+todo+"'");
            try(var r=s.executeQuery("SELECT todo_id,storage_key FROM todo_attachments WHERE id='"+id+"'")) {
                assertTrue(r.next()); assertNull(r.getString(1)); assertEquals(key,r.getString(2));
            }
        }
        var factory=new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds); factory.setPackagesToScan("com.vijay.todo_management.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","none","hibernate.type.preferred_uuid_jdbc_type","CHAR"));
        factory.afterPropertiesSet();
        EntityManagerFactory emf=factory.getObject();
        var em=emf.createEntityManager();
        try {
            var storage=new LocalAttachmentStorage(directory.toString());
            storage.write(key,new ByteArrayInputStream("hello".getBytes()),100);
            em.getTransaction().begin();
            var repository=new JpaRepositoryFactory(em).getRepository(AttachmentRepository.class);
            new AttachmentCleanup(repository,storage).clean();
            em.getTransaction().commit();
            assertFalse(Files.exists(directory.resolve(key)));
            try(var con=ds.getConnection(); var s=con.createStatement();
                    var r=s.executeQuery("SELECT COUNT(*) FROM todo_attachments")) {
                r.next(); assertEquals(0,r.getInt(1));
            }
        } finally {
            if(em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close(); emf.close();
        }
    }
}
