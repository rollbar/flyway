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
package org.flywaydb.core.internal.resolver.shell;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.resolver.MigrationExecutor;
import org.flywaydb.core.internal.util.logging.Log;
import org.flywaydb.core.internal.util.logging.LogFactory;
import org.flywaydb.core.internal.util.scanner.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Database migration based on a shell file.
 */
public class ShellMigrationExecutor implements MigrationExecutor {
    private static final Log LOG = LogFactory.getLog(ShellMigrationExecutor.class);

    /**
     * The Resource pointing to the shell script.
     * The complete shell script is not held as a member field here because this would use the total size of all
     * shell migrations files in heap space during db migration, see issue 184.
     */
    private final Resource shellScriptResource;

    /**
     * Creates a new shell script migration based on this shell script.
     *
     * @param shellScriptResource   The resource containing the sql script.
     */
    public ShellMigrationExecutor(Resource shellScriptResource) {
        this.shellScriptResource = shellScriptResource;
    }

    @Override
    public void execute(Connection connection) {
        String scriptLocation = this.shellScriptResource.getLocationOnDisk();
        LOG.info("Executing: " + scriptLocation);
        try {
            List<String> args = new ArrayList<String>();
            args.add(scriptLocation);
            ProcessBuilder builder = new ProcessBuilder(args);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            Scanner in = new Scanner(process.getInputStream());
            while (in.hasNextLine()) {
                System.out.println("| " + in.nextLine());
            }
            int returnCode = process.waitFor();
            if (returnCode != 0) {
                LOG.error("Exit value: " + returnCode);
                throw new FlywayException("Unable to apply migration");
            }
        }
        catch (Exception e) {
            LOG.error(e.toString());
            throw new FlywayException("Unable to apply migration", e);
        }
    }

    @Override
    public boolean executeInTransaction() {
        return true;
    }
}
