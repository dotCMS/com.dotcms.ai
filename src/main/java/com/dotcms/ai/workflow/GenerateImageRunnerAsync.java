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
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GenerateImageRunnerAsync implements AsyncWorkflowRunner {

    final WorkflowProcessor processor;
    final Map<String, WorkflowActionClassParameter> params;
    final int runDelay;

    @Override
    public WorkflowProcessor getProcessor() {
        return this.processor;
    }

    @Override
    public Map<String, WorkflowActionClassParameter> getParams() {
        return params;
    }

    GenerateImageRunnerAsync(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) {
        this.processor = processor;
        this.params = params;
        this.runDelay=Try.of(()->Integer.parseInt(params.get("runDelay").getValue())).getOrElse(5);
    }




    public void runInternal()  {




        Contentlet workingContentlet = processor.getContentlet();
        final Contentlet finalContentlet = workingContentlet;
        final Host host = Try.of(() -> APILocator.getHostAPI().find(finalContentlet.getHost(), APILocator.systemUser(), true)).getOrElse(APILocator.systemHost());


        final Optional<Field> fieldToWrite = resolveField(params, workingContentlet);
        final boolean overwriteField = Try.of(() -> Boolean.parseBoolean(params.get("overwriteField").getValue())).getOrElse(false);


        final String promptIn = params.get("openAIPrompt").getValue();
        if (UtilMethods.isEmpty(promptIn)) {
            Logger.info(OpenAIContentPromptActionlet.class, "no prompt found, returning");
            return;
        }

        Logger.info(this.getClass(), "Running OpenAI Generate Image Content for : " + workingContentlet.getTitle());

        if (fieldToWrite.isEmpty()) {
            Logger.info(this.getClass(), "no binary field found, returning");
            return;
        }

        Optional<Object> fieldVal = Try.of(() -> APILocator.getContentletAPI().getFieldValue(finalContentlet, fieldToWrite.get())).toJavaOptional();
        if (fieldVal.isPresent() && UtilMethods.isSet(fieldVal.get()) && !overwriteField) {
            Logger.info(OpenAIContentPromptActionlet.class, "field:" + fieldToWrite.get().variable() + "  already set:" + fieldVal.get() + ", returning");
            return;
        }


        try {
            HttpServletRequest requestProxy =
                    new MockSessionRequest(new MockHeaderRequest(new FakeHttpRequest("localhost", null).request(), "referer", "https://local.dotcms.site:8443/fakeRefer").request());
            requestProxy.setAttribute(WebKeys.CMS_USER, processor.getUser());
            requestProxy.getSession().setAttribute(WebKeys.CMS_USER, processor.getUser());
            requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER_ID, processor.getUser().getUserId());


            Context ctx = VelocityUtil.getWebContext(requestProxy, new BaseResponse().response());
            ContentMap contentMap = new ContentMap(workingContentlet, processor.getUser(), PageMode.EDIT_MODE, host, ctx);
            ctx.put("dotContentMap", contentMap);
            ctx.put("contentlet", contentMap);
            ctx.put("content", contentMap);


            String finalPrompt = VelocityUtil.eval(promptIn, ctx);

            ChatGPTImageService service = new ChatGPTImageServiceImpl(ConfigService.INSTANCE.config(host));
            AIImageResponseDTO resp = service.sendChatGPTRequest(finalPrompt, ConfigService.INSTANCE.config(host), false);

            if (!resp.getHttpStatus().equals(String.valueOf(HttpResponseStatus.OK.code()))) {
                Logger.warn(this.getClass(), "API Request error:" + resp.getHttpStatus());
                Logger.warn(this.getClass(), "API Request error:" + resp.getResponse());
                return;
            }

            final TempFileAPI tempApi = APILocator.getTempFileAPI();
            final String url = resp.getResponse();
            DotTempFile tmpFile = tempApi.createTempFileFromUrl(StringUtils.camelCaseLower(workingContentlet.getTitle()) + ".png", requestProxy, new URL(url), 20, Integer.MAX_VALUE);
            boolean isPublished = APILocator.getVersionableAPI().isLive(workingContentlet);


            // if we are async, then get the latest
            if(runDelay>0) {
                workingContentlet = APILocator.getContentletAPI().findContentletByIdentifier(workingContentlet.getIdentifier(), false, workingContentlet.getLanguageId(), processor.getUser(), false);
            }

            workingContentlet.setProperty(fieldToWrite.get().variable(), tmpFile.file);
            processor.setContentlet(workingContentlet);


            new SaveContentActionlet().executeAction(processor, Map.of());
            if (isPublished) {
                new PublishContentActionlet().executeAction(processor, Map.of());
            }


        } catch (Exception e) {
            final SystemMessageBuilder message = new SystemMessageBuilder().setMessage("Error:" + e.getMessage())
                    .setLife(5000).setType(MessageType.SIMPLE_MESSAGE).setSeverity(MessageSeverity.ERROR);

            SystemMessageEventUtil.getInstance().pushMessage(message.create(), List.of(processor.getUser().getUserId()));
            Logger.warn(this.getClass(), "Error:" + e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    Optional<Field> resolveField(Map<String, WorkflowActionClassParameter> params, Contentlet contentlet) {
        final ContentType type = contentlet.getContentType();
        final Optional<Field> fieldToWrite = Try.of(() -> type.fieldMap().get(params.get("fieldToWrite").getValue())).toJavaOptional();
        if (fieldToWrite.isPresent()) {
            return fieldToWrite;
        }

        return type.fields(BinaryField.class).stream().findFirst();

    }


}
