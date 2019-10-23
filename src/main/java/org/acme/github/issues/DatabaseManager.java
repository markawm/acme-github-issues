package org.acme.github.issues;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

@ApplicationScoped
public class DatabaseManager {
    private Jdbi jdbi;

    @Inject
    @Default
    DataSource dataSource;

    @PostConstruct
    public void init() {
        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new PostgresPlugin());
    }


    @Produces
    @ApplicationScoped
    public Jdbi provider() {
        return jdbi;
    }

}
