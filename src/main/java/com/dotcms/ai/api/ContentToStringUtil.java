package com.dotcms.ai.api;

import com.dotcms.ai.workflow.DotEmbeddingsActionlet;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.rendering.velocity.viewtools.MarkdownTool;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockMap;
import com.dotcms.repackage.org.jsoup.Jsoup;
import com.dotcms.tika.TikaProxyService;
import com.dotcms.tika.TikaServiceBuilder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.felix.framework.OSGISystem;

import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContentToStringUtil {

    static MarkdownTool markdown = new MarkdownTool();


    public static final Lazy<ContentToStringUtil> instance = Lazy.of(ContentToStringUtil::new);

    private ContentToStringUtil() {

    }


    private final Lazy<TikaProxyService> tikaService = Lazy.of(() -> {
        try {
            return OSGISystem.getInstance().getService(TikaServiceBuilder.class, "com.dotcms.tika").createTikaService();
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    });

    private String parseHTML(@NotNull String val) {

        try (InputStream htmlInputStream = new ByteArrayInputStream(val.getBytes(StandardCharsets.UTF_8))) {
            return tikaService.get().parseToString(htmlInputStream);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

    }


    private String parseText(@NotNull String val) {

        return val;

    }

    private String parseBlockEditor(@NotNull String val) {

        final StoryBlockMap storyBlockMap = new StoryBlockMap(val);
        return parseHTML(storyBlockMap.toHtml());


    }

    private String parseMarkdown(@NotNull String val) {

        try {
            MarkdownTool tool = new MarkdownTool();
            return parseHTML(tool.parse(val));
        } catch (Throwable e) {
            throw new DotRuntimeException(e);
        }

    }

    /**
     * This method will index the first long_text field that has been marked as indexed
     * @param contentlet
     * @return
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public String guessWhatToIndex(@NotNull Contentlet contentlet) throws IOException, DotDataException, DotSecurityException {
        try {
            if (contentlet.isFileAsset()) {
                return parseField(contentlet.getContentType().fieldMap().get("fileAsset"), contentlet.getBinary("fileAsset"));
            }

            if (contentlet.isDotAsset()) {
                return parseField(contentlet.getContentType().fieldMap().get("asset"), contentlet.getBinary("asset"));
            }

            if (contentlet.isHTMLPage()) {
                return parseHTML(APILocator.getHTMLPageAssetAPI().getHTML(APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet), "dot-user-agent"));
            }

            Optional<Field> firstTextField = contentlet.getContentType().fields().stream().filter(Field::indexed).filter(f->f.dataType().equals(DataTypes.LONG_TEXT)).findFirst();

            if(firstTextField.isEmpty()){
                firstTextField = contentlet.getContentType().fields().stream().filter(f->f.dataType().equals(DataTypes.LONG_TEXT)).findFirst();
            }

            if(firstTextField.isEmpty()){
                firstTextField = contentlet.getContentType().fields().stream().filter(Field::indexed).filter(f->f.dataType().equals(DataTypes.TEXT)).findFirst();
            }

            if(firstTextField.isEmpty()){
                throw new DotDataException("Unable to find a field to build embeddings off of");
            }
            return parseField(contentlet, firstTextField.get() );
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }







    public String parseFields(@NotNull Contentlet con, @NotNull List<Field> fields) {
        StringBuilder builder = new StringBuilder();
        for (Field field : fields) {
            String fieldVal = con.getStringProperty(field.variable());
            builder.append(parseField(field, fieldVal));
            builder.append(" \n");
        }
        return builder.toString();
    }

    public String parseField(@NotNull Contentlet contentlet, @NotNull Field field) {
        try {
            if (field instanceof BinaryField) {
                return parseField(field, contentlet.getBinary(field.variable()));
            }
            if (contentlet.isHTMLPage()) {
                return parseHTML(APILocator.getHTMLPageAssetAPI().getHTML(APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet), "dot-user-agent"));
            }

            if (field.dataType().equals(DataTypes.LONG_TEXT) || field.dataType().equals(DataTypes.TEXT)) {
                return parseField(field, contentlet.getStringProperty(field.variable()));
            }

            throw new DotRuntimeException("Unable to build String from field " + contentlet.getContentType().variable() + "." + field);

        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }


    public String parseField(@NotNull Field field, @NotNull String value) {


        if (field instanceof StoryBlockField && StringUtils.isJson(value)) {
            Logger.info(DotEmbeddingsActionlet.class, "field:" + field.variable() + " is a Blockeditor field");
            return parseBlockEditor(value);
        }
        if (isMarkdown(value)) {
            Logger.info(DotEmbeddingsActionlet.class, "field:" + field.variable() + " is a markdown field");
            return parseMarkdown(value);
        }
        if (isHtml(value)) {
            Logger.info(DotEmbeddingsActionlet.class, "field:" + field.variable() + " is an HTML field");
            return parseHTML(value);
        }
        Logger.info(DotEmbeddingsActionlet.class, "field:" + field.variable() + " is an text field");
        return parseText(value);

    }

    public String parseField(@NotNull Field field, @NotNull File file) {
        try {
            return tikaService.get().parseToString(file);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public String parsePage(Contentlet pageContentlet) {
        try {
            //HTML
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
        return null;
    }


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


    private boolean isMarkdown(@NotNull String value) {


        String converted = Try.of(() -> markdown.parse(value.substring(0, Math.min(value.length(), 10000)))).getOrNull();
        if (converted == null || value.equals(converted)) {
            return false;
        }
        // its markdown if we get two or more matching patterns
        return MARKDOWN_PATTERNS.get().stream().filter(p -> p.matcher(value).find()).count() > 1;
    }


    private static final Pattern HTML_PATTERN = Pattern.compile(".*\\<[^>]+>.*");

    // it is HTML if parsing it returns a different value than it
    private boolean isHtml(@NotNull String value) {
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
