package com.dotcms.ai.workflow;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AsyncContentPromptRunner implements AsyncWorkflowRunner {

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
    AsyncContentPromptRunner(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) {
        this.processor = processor;
        this.params = params;
        this.runDelay=Try.of(()->Integer.parseInt(params.get("runDelay").getValue())).getOrElse(5);
    }





    public void runInternal() {




        Contentlet workingContentlet = processor.getContentlet();
        final ContentType type = workingContentlet.getContentType();
        final Contentlet finalContentlet = workingContentlet;

        Logger.info(this.getClass(), "Running OpenAI Modify Content for : " + workingContentlet.getTitle());
        final boolean overwriteField = Try.of(() -> Boolean.parseBoolean(params.get("overwriteField").getValue())).getOrElse(true);

        final Optional<Field> fieldToWrite = Try.of(() -> type.fieldMap().get(params.get("fieldToWrite").getValue())).toJavaOptional();
        final String promptIn = params.get("openAIPrompt").getValue();
        if (UtilMethods.isEmpty(promptIn)) {
            Logger.info(OpenAIContentPromptActionlet.class, "no prompt found");
            return;
        }


        try {

            JSONObject tryJson = openAIRequest();

            if(runDelay>0) {
                workingContentlet = APILocator.getContentletAPI().findContentletByIdentifier(workingContentlet.getIdentifier(), false, workingContentlet.getLanguageId(), processor.getUser(), false);
            }

            boolean contentNeedsSaving = setJsonProperties(workingContentlet, tryJson, overwriteField);


            if (!contentNeedsSaving) {
                Logger.warn(this.getClass(), "Nothing to save for OpenAI response: " + tryJson);
            }


            processor.setContentlet(workingContentlet);
            if (contentNeedsSaving) {
                boolean isPublished = APILocator.getVersionableAPI().isLive(workingContentlet);
                new SaveContentActionlet().executeAction(processor, Map.of());
                if (isPublished) {
                    new PublishContentActionlet().executeAction(processor, Map.of());
                }
            }
        } catch (Exception e) {
            final SystemMessageBuilder message = new SystemMessageBuilder().setMessage("Error:" + e.getMessage())
                    .setLife(5000).setType(MessageType.SIMPLE_MESSAGE).setSeverity(MessageSeverity.ERROR);

            SystemMessageEventUtil.getInstance().pushMessage(message.create(), List.of(processor.getUser().getUserId()));
            Logger.warn(this.getClass(), "Error:" + e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private JSONObject openAIRequest() throws Exception {

        final String promptIn = params.get("openAIPrompt").getValue();
        Context ctx = new VelocityUtil().getWorkflowContext(processor);
        ctx.put("dotContentMap", ctx.get("contentMap"));
        ctx.put("contentlet", ctx.get("contentMap"));
        final String model = UtilMethods.isEmpty(() -> params.get("model").getValue())
                ? ConfigService.INSTANCE.config().getConfig(AppKeys.COMPLETION_MODEL)
                : params.get("model").getValue();

        final String temperatureIn = UtilMethods.isEmpty(() -> params.get("temperature").getValue())
                ? ConfigService.INSTANCE.config().getConfig(AppKeys.COMPLETION_MODEL)
                : params.get("temperature").getValue();

        final float temperature = Float.parseFloat(temperatureIn);

        final String prompt = VelocityUtil.eval(promptIn, ctx);

        JSONObject openAIResponse = CompletionsAPI.impl().raw(buildRequest(prompt, model, temperature));

        String response = openAIResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

        return parseJson(response);


    }

    boolean setJsonProperties(Contentlet contentlet, JSONObject json, boolean overwriteField) {
        boolean contentNeedsSaving = false;
        for (Map.Entry entry : (Set<Map.Entry>) json.getAsMap().entrySet()) {
            if (overwriteField || UtilMethods.isEmpty(contentlet.getStringProperty(entry.getKey().toString()))) {
                contentlet.setProperty(entry.getKey().toString(), entry.getValue());
                contentNeedsSaving = true;
            }
        }
        return contentNeedsSaving;
    }

    private JSONObject buildRequest(String prompt, String model, float temperature) {
        JSONArray messages = new JSONArray();
        messages.add(Map.of("role", "user", "content", prompt));
        JSONObject json = new JSONObject();
        json.put("messages", messages);
        json.put("model", model);
        json.put("temperature", temperature);
        json.put("stream", false);

        return json;
    }

    JSONObject parseJson(String response) {

        Logger.debug(this.getClass(), "---- response ----- ");
        Logger.debug(this.getClass(), response);
        Logger.debug(this.getClass(), "---- response ----- ");
        response = response.replaceAll("\\R+", " ");
        response = response.substring(response.indexOf("{"), response.lastIndexOf("}")+1);

        String finalResponse = response;
        return Try.of(() -> new JSONObject(finalResponse)).onFailure(e -> Logger.warn(this.getClass(), e.getMessage())).getOrElse(new JSONObject());


    }


}
