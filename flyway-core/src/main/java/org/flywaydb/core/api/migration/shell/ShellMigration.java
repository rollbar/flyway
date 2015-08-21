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
package org.flywaydb.core.api.migration.shell;

import java.sql.Connection;

/**
 * Interface to be implemented by Shell Migrations. By default the migration version and description will be extracted
 * from the file name. The checksum of this migration (for validation) will also be null.
 */
public interface ShellMigration {
    /**
     * Executes this migration. The execution will automatically take place within a transaction, when the underlying
     * database supports it.
     *
     * @param connection The connection to use to execute statements.
     * @throws Exception when the migration failed.
     */
    void migrate(Connection connection) throws Exception;
}
