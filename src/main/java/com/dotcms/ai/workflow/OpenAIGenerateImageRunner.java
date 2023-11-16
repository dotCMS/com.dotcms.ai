package com.dotcms.ai.workflow;

import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.model.AIImageResponseDTO;
import com.dotcms.ai.service.ChatGPTImageService;
import com.dotcms.ai.service.ChatGPTImageServiceImpl;
import com.dotcms.ai.util.Logger;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OpenAIGenerateImageRunner implements AsyncWorkflowRunner {


    final String identifier;
    final long language;
    final User user;
    final String prompt;
    final boolean overwriteField;
    final String fieldToWrite;
    final long runAt;


    OpenAIGenerateImageRunner(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) {
        this(
                processor.getContentlet(),
                processor.getUser(),
                params.get(OpenAIParams.OPEN_AI_PROMPT.key).getValue(),
                Try.of(() -> Boolean.parseBoolean(params.get(OpenAIParams.OVERWRITE_FIELDS.key).getValue())).getOrElse(false),
                params.get(OpenAIParams.FIELD_TO_WRITE.key).getValue(),
                Try.of(() -> Integer.parseInt(params.get(OpenAIParams.RUN_DELAY.key).getValue())).getOrElse(5)

        );
    }

    OpenAIGenerateImageRunner(Contentlet contentlet, User user, String prompt, boolean overwriteField, String fieldToWrite, int runDelay) {
        this.identifier = contentlet.getIdentifier();
        this.language = contentlet.getLanguageId();
        this.prompt = prompt;
        this.overwriteField = overwriteField;
        this.fieldToWrite = fieldToWrite;
        this.user = user;
        this.runAt = System.currentTimeMillis() + runDelay;

    }

    @Override
    public long getRunAt() {
        return this.runAt;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public long getLanguage() {
        return this.language;
    }

    public void runInternal() {


        final Contentlet workingContentlet = checkoutLatest(identifier, language, user);

        final Host host = Try.of(() -> APILocator.getHostAPI().find(workingContentlet.getHost(), APILocator.systemUser(), true)).getOrElse(APILocator.systemHost());


        final Optional<Field> fieldToWrite = resolveField(workingContentlet);


        if (UtilMethods.isEmpty(prompt)) {
            Logger.info(OpenAIContentPromptActionlet.class, "no prompt found, returning");
            return;
        }

        Logger.info(this.getClass(), "Running OpenAI Generate Image Content for : " + workingContentlet.getTitle());

        if (fieldToWrite.isEmpty()) {
            Logger.info(this.getClass(), "no binary field found, returning");
            return;
        }

        Optional<Object> fieldVal = Try.of(() -> APILocator.getContentletAPI().getFieldValue(workingContentlet, fieldToWrite.get())).toJavaOptional();
        if (fieldVal.isPresent() && UtilMethods.isSet(fieldVal.get()) && !overwriteField) {
            Logger.info(OpenAIContentPromptActionlet.class, "field:" + fieldToWrite.get().variable() + "  already set:" + fieldVal.get() + ", returning");
            return;
        }


        try {
            Context ctx = getMockContext(workingContentlet, user);
            String finalPrompt = VelocityUtil.eval(prompt, ctx);

            ChatGPTImageService service = new ChatGPTImageServiceImpl(ConfigService.INSTANCE.config(host));
            AIImageResponseDTO resp = service.sendChatGPTRequest(finalPrompt, ConfigService.INSTANCE.config(host), false);

            if (!resp.getHttpStatus().equals(String.valueOf(HttpResponseStatus.OK.code()))) {
                Logger.warn(this.getClass(), "API Request error:" + resp.getHttpStatus());
                Logger.warn(this.getClass(), "API Request error:" + resp.getResponse());
                return;
            }

            final TempFileAPI tempApi = APILocator.getTempFileAPI();
            final String url = resp.getResponse();
            HttpServletRequest request = (HttpServletRequest) ctx.get("request");
            DotTempFile tmpFile = tempApi.createTempFileFromUrl(StringUtils.camelCaseLower(workingContentlet.getTitle()) + ".png", request, new URL(url), 20, Integer.MAX_VALUE);

            final Contentlet contentToSave = checkoutLatest(identifier, language, user);
            contentToSave.setProperty(fieldToWrite.get().variable(), tmpFile.file);
            saveContentlet(contentToSave, user);


        } catch (Exception e) {
            final SystemMessageBuilder message = new SystemMessageBuilder().setMessage("Error:" + e.getMessage())
                    .setLife(5000).setType(MessageType.SIMPLE_MESSAGE).setSeverity(MessageSeverity.ERROR);

            SystemMessageEventUtil.getInstance().pushMessage(message.create(), List.of(user.getUserId()));
            Logger.warn(this.getClass(), "Error:" + e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    Optional<Field> resolveField(Contentlet contentlet) {
        final ContentType type = contentlet.getContentType();
        final Optional<Field> fieldToWrite = Try.of(() -> type.fieldMap().get(this.fieldToWrite)).toJavaOptional();
        if (fieldToWrite.isPresent()) {
            return fieldToWrite;
        }

        return type.fields(BinaryField.class).stream().findFirst();

    }


}
