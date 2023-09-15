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

package io.appform.conductor.server.ui;

import com.github.jknack.handlebars.Template;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.appform.conductor.server.utils.HandlebarsUtils;
import io.appform.conductor.server.utils.dev.IgnoreGenerated;
import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A {@link ViewRenderer} which renders Handlebars ({@code .hbs}) templates.
 */
@Slf4j
@IgnoreGenerated
public class HandlebarsViewRenderer implements ViewRenderer {

    /**
     * Handlebars.java does not cache reads of Template content from resources.
     */
    static final LoadingCache<String, Template> compilationCache = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<>() {
                @Override
                public Template load(String srcUrl) throws Exception {
                    return HandlebarsUtils.handlebars().compile(srcUrl.replaceAll(".hbs$", ""));
                }
            });


    public HandlebarsViewRenderer() {
        log.info("Handlebars view renderer created");
    }

    @Override
    public boolean isRenderable(View view) {
        return view.getTemplateName().endsWith(".hbs");
    }

    @Override
    public void render(View view, Locale locale, OutputStream output) throws IOException {
        try (Writer writer = new OutputStreamWriter(output, view.getCharset().orElse(StandardCharsets.UTF_8))) {
            compilationCache.get(view.getTemplateName()).apply(view, writer);
        } catch (FileNotFoundException | ExecutionException e) {
            throw new FileNotFoundException("Template " + view.getTemplateName() + " not found.");
        } catch (Throwable t) {
            log.error("Error generating view: " + t.getMessage(), t);
        }
    }

    @Override
    public void configure(Map<String, String> options) {
        //do nothing
    }

    @Override
    public String getConfigurationKey() {
        return "handlebars";
    }
}
