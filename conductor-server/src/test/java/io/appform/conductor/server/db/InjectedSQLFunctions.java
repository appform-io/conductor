/*
 * Copyright (c) 2023 Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.server.db;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.SessionFactory;

import java.util.Arrays;
import java.util.Date;

/**
 *
 */
@Slf4j
public class InjectedSQLFunctions {
    @SneakyThrows
    public static void register(SessionFactory sessionFactory) {
        try (final var session = sessionFactory.openSession()) {
            val t = session.beginTransaction();
            try {
                Arrays.stream(InjectedSQLFunctions.class.getMethods())
                        .filter(method -> method.isAnnotationPresent(MissingSQLFunction.class))
                        .forEach(method -> {
                            val fqmn = InjectedSQLFunctions.class.getCanonicalName() + "." + method.getName();
                            val lfn = method.getAnnotation(MissingSQLFunction.class).value();
                            Preconditions.checkArgument(!Strings.isNullOrEmpty(lfn),
                                                        "H2 function name can be empty");
                            log.info("Registering method {} as {}", fqmn, lfn);
                            session.createSQLQuery(String.format("DROP ALIAS IF EXISTS %s", lfn)).executeUpdate();
                            session.createSQLQuery(String.format("CREATE ALIAS %s FOR '%s'", lfn, fqmn)).executeUpdate();
                        });
            }
            finally {
                t.commit();
            }
        }
    }

    @SuppressWarnings("unused")
    @MissingSQLFunction("UNIX_TIMESTAMP")
    public static long unixTimestamp(final Date input) {
        return input == null ? 0L : input.getTime() / 1000L;
    }
}
