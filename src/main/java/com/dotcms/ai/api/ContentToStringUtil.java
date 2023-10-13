package com.dotcms.ai.api;


import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.viewtools.MarkdownTool;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap;
import com.dotcms.repackage.org.jsoup.Jsoup;
import com.dotcms.tika.TikaProxyService;
import com.dotcms.tika.TikaServiceBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.felix.framework.OSGISystem;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContentToStringUtil {

    public static final Lazy<ContentToStringUtil> impl = Lazy.of(ContentToStringUtil::new);
    private static final String[] MARKDOWN_STRING_PATTERNS = {
            "(^|[\\n])\\s*1\\.\\s.*\\s+1\\.\\s",                    // markdown list with 1. \n 1.
            "(^|[\\n])\\s*-\\s.*\\s+-\\s",                          // markdown unordered list -
            "(^|[\\n])\\s*\\*\\s.*\\s+\\*\\s",                      // markdown unordered list *
            "\\s(__|\\*\\*)(?!\\s)(.(?!\\1))+(?!\\s(?=\\1))",       // markdown __bold__
            "\\[[^]]+\\]\\(https?:\\/\\/\\S+\\)",                   // markdown link [this is a link](http://linking)
            "\\n####\\s.*$",                                        // markdown h4
            "\\n###\\s.*$",                                         // markdown h3
            "\\n##\\s.*$",                                          // markdown h2
            "\\n#\\s.*$",                                           // markdown h1
            "\\n```"                                                // markdown code block


    };
    private static final Lazy<List<Pattern>> MARKDOWN_PATTERNS = Lazy.of(() -> Arrays.stream(MARKDOWN_STRING_PATTERNS).map(Pattern::compile).collect(Collectors.toList()));
    private static final Pattern HTML_PATTERN = Pattern.compile(".*\\<[^>]+>.*");
    static MarkdownTool markdown = new MarkdownTool();



    private final Lazy<TikaProxyService> tikaService = Lazy.of(() -> {
        try {
            return OSGISystem.getInstance().getService(TikaServiceBuilder.class, "com.dotcms.tika").createTikaService();
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    });

    private ContentToStringUtil() {

    }

    /**
     * This creates a tmp file with an .html extension so that tika can parse it as "html".  If tika fails, we fall
     * back to JSoup.  If that fails, we fall back to regex
     *
     * @param html
     * @return
     */
    private Optional<String> parseHTML(@NotNull String html) {

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(null, ".html");
            Files.write(tempFile, html.getBytes(StandardCharsets.UTF_8));
            return parseText(tikaService.get().parseToString(tempFile.toFile()));
        } catch (Exception e) {
            Logger.warnAndDebug(ContentToStringUtil.class, "Tika failed parsing, trying JSoup:" + e.getMessage(), e);
            try {
                return Optional.ofNullable(Jsoup.parse(html).text());
            } catch (Exception e1) {
                Logger.warnAndDebug(ContentToStringUtil.class, "JSoup failed parsing, trying regex:" + e1.getMessage(), e);
                return Try.of(() -> html.replaceAll("<[^>]*>", "")).toJavaOptional();
            }


        } finally {
            try {
                tempFile.toFile().delete();//NOSONAR
            } catch (Exception ex) {
                //NOSONAR
            }
        }
    }

    private Optional<String> parseText(@NotNull String val) {
        val = UtilMethods.isSet(val)
                ? val.replaceAll("\\s+", " ")
                : null;

        if(UtilMethods.isEmpty(val) || val.length()<1024){
            return Optional.empty();
        }
        return Optional.of(val);


    }

    private Optional<String> parseBlockEditor(@NotNull String val) {

        final StoryBlockMap storyBlockMap = new StoryBlockMap(val);
        return parseHTML(storyBlockMap.toHtml());


    }

    private Optional<String> parseMarkdown(@NotNull String val) {

        try {
            MarkdownTool tool = new MarkdownTool();
            return parseHTML(tool.parse(val));
        } catch (Throwable e) {
            throw new DotRuntimeException(e);
        }

    }

    /**
     * This method will index the first long_text field that has been marked as indexed
     *
     * @param contentlet
     * @return
     */


    public Optional<Field> guessWhatFieldToIndex(@NotNull Contentlet contentlet) {
        if (contentlet.isFileAsset()) {
            File fileAsset = Try.of(() -> contentlet.getBinary("fileAsset")).getOrNull();
            if (!indexMe(fileAsset)) {
                return Optional.empty();
            }
            return Optional.ofNullable(contentlet.getContentType().fieldMap().get("fileAsset"));
        }
        if (contentlet.isDotAsset()) {
            File asset = Try.of(() -> contentlet.getBinary("asset")).getOrNull();
            if (!indexMe(asset)) {
                return Optional.empty();
            }
            return Optional.ofNullable(contentlet.getContentType().fieldMap().get("asset"));
        }

        final String ignoreUrlMapFields = (contentlet.getContentType().urlMapPattern() != null) ? contentlet.getContentType().urlMapPattern() : "";

        Optional<Field> foundField= contentlet.getContentType()
                .fields()
                .stream().filter(f -> !ignoreUrlMapFields.contains("{" + f.variable() + "}"))
                .filter(f -> f instanceof StoryBlockField || f instanceof WysiwygField
                ).findFirst();

        if(foundField.isPresent()){
            return foundField;
        }

        return contentlet.getContentType()
                .fields()
                .stream().filter(f -> !ignoreUrlMapFields.contains("{" + f.variable() + "}"))
                .filter(f ->
                        f instanceof TextAreaField  || f.dataType().equals(DataTypes.LONG_TEXT)
                ).findFirst();
    }

    private boolean indexMe(File file) {

        final Set<String> indexFileExtensions = Set.of(ConfigService.INSTANCE.config().getConfigArray(AppKeys.EMBEDDINGS_FILE_EXTENSIONS_TO_EMBED));
        final int minimumTextLength = ConfigService.INSTANCE.config().getConfigInteger(AppKeys.EMBEDDINGS_MINIMUM_FILE_SIZE_TO_INDEX);



        return file != null && indexFileExtensions.contains(UtilMethods.getFileExtension(file.toString()));
    }


    public String parseFields(@NotNull Contentlet con, @NotNull List<Field> fields) {
        StringBuilder builder = new StringBuilder();
        for (Field field : fields) {
            builder.append(parseField(con, Optional.of(field)));
            builder.append(" \n");
        }
        return builder.toString();
    }

    private Optional<String> parseFile(@NotNull File file) {
        if (!indexMe(file)) {
            return Optional.empty();
        }

        return Try.of(() -> tikaService.get().parseToString(file)).toJavaOptional();

    }

    public Optional<String> parseField(@NotNull Contentlet contentlet, @NotNull Optional<Field> fieldOpt) {
        if(UtilMethods.isEmpty(()->contentlet.getIdentifier())) {
            return Optional.empty();
        }
        ContentType type = contentlet.getContentType();


        if (fieldOpt.isEmpty() && contentlet.isHTMLPage() == Boolean.TRUE) {
            return parsePage(contentlet);
        }
        if (fieldOpt.isEmpty()) {
            return Optional.empty();
        }
        Field field = fieldOpt.get();
        if (field instanceof BinaryField) {
            return parseFile(Try.of(() -> contentlet.getBinary(field.variable())).getOrNull());
        }
        String value = contentlet.getStringProperty(field.variable());

        if (field instanceof StoryBlockField && StringUtils.isJson(value)) {
            Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is a StoryBlockField field");
            return parseBlockEditor(value);
        }
        if (isMarkdown(value)) {
            Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is a markdown field");
            return parseMarkdown(value);
        }
        if (isHtml(value)) {
            Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is an HTML field");
            return parseHTML(value);
        }
        Logger.info(this.getClass(), type.variable() + "." + field.variable() + " is a text field");
        return parseText(value);

    }

    private Optional<String> parsePage(Contentlet pageContentlet) {
        if(UtilMethods.isEmpty(()->pageContentlet.getIdentifier())) {
            return Optional.empty();
        }
        try {
            if (Boolean.FALSE.equals(pageContentlet.isHTMLPage())) {
                return Optional.empty();
            }
            return Optional.ofNullable(APILocator.getHTMLPageAssetAPI().getHTML(APILocator.getHTMLPageAssetAPI().fromContentlet(pageContentlet), "dot-user-agent"));
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "parsePage:" + pageContentlet + " failed:" + e.getMessage(), e);
            return Optional.empty();
        }
    }

    private boolean isMarkdown(@NotNull String value) {
        if(UtilMethods.isEmpty(value)) {
            return false;
        }

        String converted = Try.of(() -> markdown.parse(value.substring(0, Math.min(value.length(), 10000)))).getOrNull();
        if (converted == null || value.equals(converted)) {
            return false;
        }
        // its markdown if we get two or more matching patterns
        return MARKDOWN_PATTERNS.get().stream().filter(p -> p.matcher(value).find()).count() > 1;
    }

    // it is HTML if parsing it returns a different value than it
    private boolean isHtml(@NotNull String value) {
        if(UtilMethods.isEmpty(value)) {
            return false;
        }
        if (HTML_PATTERN.matcher(value).matches()) {
            return true;
        }

        try {
            String textOfHtmlString = Jsoup.parse(value).text();
            return !textOfHtmlString.equals(value);
        } catch (Exception e) {
            Logger.warnAndDebug(EmbeddingsAPIImpl.class, e);
        }
        return false;
    }


}
