/**
 * Copyright 2010-2015 Axel Fontaine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.command;

import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.internal.info.MigrationInfoServiceImpl;
import org.flywaydb.core.internal.metadatatable.MetaDataTable;
import org.flywaydb.core.internal.util.Pair;
import org.flywaydb.core.internal.util.StopWatch;
import org.flywaydb.core.internal.util.TimeFormat;
import org.flywaydb.core.internal.util.jdbc.TransactionCallback;
import org.flywaydb.core.internal.util.jdbc.TransactionTemplate;
import org.flywaydb.core.internal.util.logging.Log;
import org.flywaydb.core.internal.util.logging.LogFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handles the validate command.
 *
 * @author Axel Fontaine
 */
public class DbValidate {
    private static final Log LOG = LogFactory.getLog(DbValidate.class);

    /**
     * The target version of the migration.
     */
    private final MigrationVersion target;

    /**
     * The database metadata table.
     */
    private final MetaDataTable metaDataTable;

    /**
     * The migration resolver.
     */
    private final MigrationResolver migrationResolver;

    /**
     * The connection to use.
     */
    private final Connection connectionMetaDataTable;

    /**
     * The connection to use to perform the actual database migrations.
     */
    private final Connection connectionUserObjects;

    /**
     * Allows migrations to be run "out of order".
     * <p>If you already have versions 1 and 3 applied, and now a version 2 is found,
     * it will be applied too instead of being ignored.</p>
     * <p>(default: {@code false})</p>
     */
    private final boolean outOfOrder;

    /**
     * Whether pending or future migrations are allowed.
     */
    private final boolean pendingOrFuture;

    /**
     * This is a list of callbacks that fire before or after the validate task is executed.
     * You can add as many callbacks as you want.  These should be set on the Flyway class
     * by the end user as Flyway will set them automatically for you here.
     */
    private final FlywayCallback[] callbacks;

    /**
     * Creates a new database validator.
     *
     * @param connectionMetaDataTable The connection to use.
     * @param metaDataTable           The database metadata table.
     * @param migrationResolver       The migration resolver.
     * @param target                  The target version of the migration.
     * @param outOfOrder              Allows migrations to be run "out of order".
     * @param pendingOrFuture         Whether pending or future migrations are allowed.
     * @param callbacks               The lifecycle callbacks.
     */
    public DbValidate(Connection connectionMetaDataTable, Connection connectionUserObjects,
                      MetaDataTable metaDataTable, MigrationResolver migrationResolver,
                      MigrationVersion target, boolean outOfOrder, boolean pendingOrFuture, FlywayCallback[] callbacks) {
        this.connectionMetaDataTable = connectionMetaDataTable;
        this.connectionUserObjects = connectionUserObjects;
        this.metaDataTable = metaDataTable;
        this.migrationResolver = migrationResolver;
        this.target = target;
        this.outOfOrder = outOfOrder;
        this.pendingOrFuture = pendingOrFuture;
        this.callbacks = callbacks;
    }

    /**
     * Starts the actual migration.
     *
     * @return The validation error, if any.
     */
    public String validate() {
        for (final FlywayCallback callback : callbacks) {
            new TransactionTemplate(connectionUserObjects).execute(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction() throws SQLException {
                    callback.beforeValidate(connectionUserObjects);
                    return null;
                }
            });
        }

        LOG.debug("Validating migrations ...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Pair<Integer, String> result = new TransactionTemplate(connectionMetaDataTable).execute(new TransactionCallback<Pair<Integer, String>>() {
            public Pair<Integer, String> doInTransaction() {
                MigrationInfoServiceImpl migrationInfoService =
                        new MigrationInfoServiceImpl(migrationResolver, metaDataTable, target, outOfOrder, pendingOrFuture);

                migrationInfoService.refresh();

                int count = migrationInfoService.all().length;
                String validationError = migrationInfoService.validate();
                return Pair.of(count, validationError);
            }
        });

        stopWatch.stop();

        int count = result.getLeft();
        if (count == 1) {
            LOG.info(String.format("Validated 1 migration (execution time %s)",
                    TimeFormat.format(stopWatch.getTotalTimeMillis())));
        } else {
            LOG.info(String.format("Validated %d migrations (execution time %s)",
                    count, TimeFormat.format(stopWatch.getTotalTimeMillis())));
        }

        for (final FlywayCallback callback : callbacks) {
            new TransactionTemplate(connectionUserObjects).execute(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction() throws SQLException {
                    callback.afterValidate(connectionUserObjects);
                    return null;
                }
            });
        }

        return result.getRight();
    }
}
