package io.appform.conductor.core.utils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.appform.conductor.core.ui.CustomHelpers;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HandlebarsUtils {

    /**
     * For use by Handlebars.java internally.
     */
    private static final Cache<TemplateSource, Template> templateCache = CacheBuilder
            .newBuilder()
            .build();

    private static final Handlebars HANDLEBARS = new Handlebars()
            .with(new GuavaTemplateCache(templateCache))
            .registerHelper("eq", ConditionalHelpers.eq)
            .registerHelper("neq", ConditionalHelpers.neq)
            .registerHelper("not", ConditionalHelpers.not)
            .registerHelper("or", ConditionalHelpers.or)
            .registerHelper("and", ConditionalHelpers.and)
            .registerHelper("gt", ConditionalHelpers.gt)
            .registerHelper("gte", ConditionalHelpers.gte)
            .registerHelper("lt", ConditionalHelpers.lt)
            .registerHelper("lte", ConditionalHelpers.lte)
            .registerHelpers(new CustomHelpers());

    public static Handlebars handlebars() {
        return HANDLEBARS;
    }
}
