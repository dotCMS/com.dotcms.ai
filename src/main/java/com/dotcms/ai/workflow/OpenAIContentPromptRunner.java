package com.dotcms.ai.workflow;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.api.ContentToStringUtil;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OpenAIContentPromptRunner implements AsyncWorkflowRunner {


    final String identifier;
    final long language;
    final User user;
    final String prompt;
    final boolean overwriteField;
    final String fieldToWrite;
    final long runAt;
    final String model;
    final float temperature;

    OpenAIContentPromptRunner(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) {
        this(
                processor.getContentlet(),
                processor.getUser(),
                params.get(OpenAIParams.OPEN_AI_PROMPT.key).getValue(),
                Boolean.parseBoolean(params.get(OpenAIParams.OVERWRITE_FIELDS.key).getValue()),
                params.get(OpenAIParams.FIELD_TO_WRITE.key).getValue(),
                Try.of(() -> Integer.parseInt(params.get(OpenAIParams.RUN_DELAY.key).getValue())).getOrElse(5),
                params.get(OpenAIParams.MODEL.key).getValue(),
                Try.of(() -> Float.parseFloat(params.get(OpenAIParams.TEMPERATURE).getValue())).getOrElse(ConfigService.INSTANCE.config().getConfigFloat(AppKeys.COMPLETION_TEMPERATURE))
        );
    }

    OpenAIContentPromptRunner(Contentlet contentlet, User user, String prompt, boolean overwriteField, String fieldToWrite, int runDelay, String model, float temperature) {
        if (UtilMethods.isEmpty(() -> contentlet.getIdentifier())) {
            throw new DotRuntimeException("Content must be saved and have an identifier before running AI Content Prompt");
        }
        this.identifier = contentlet.getIdentifier();
        this.language = contentlet.getLanguageId();
        this.prompt = prompt;
        this.overwriteField = overwriteField;
        this.fieldToWrite = fieldToWrite;
        this.user = user;
        this.runAt = System.currentTimeMillis() + runDelay;
        this.model = model;
        this.temperature = temperature;
    }

    @Override
    public long getRunAt() {
        return runAt;
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

        if (UtilMethods.isEmpty(prompt)) {
            Logger.info(OpenAIContentPromptActionlet.class, "no prompt found");
            return;
        }
        final Contentlet workingContentlet = checkoutLatest(identifier, language, user);

        final ContentType type = workingContentlet.getContentType();


        Logger.info(this.getClass(), "Running OpenAI Modify Content for : " + workingContentlet.getTitle());

        final Optional<Field> fieldToWrite = Try.of(() -> type.fieldMap().get(this.fieldToWrite)).toJavaOptional();


        try {

            JSONObject tryJson = openAIRequest(workingContentlet);

            final Contentlet contentToSave = checkoutLatest(identifier, language, user);
            boolean contentNeedsSaving = setJsonProperties(contentToSave, tryJson, overwriteField);

            if (!contentNeedsSaving) {
                Logger.warn(this.getClass(), "Nothing to save for OpenAI response: " + tryJson);
                return;
            }
            saveContentlet(contentToSave, user);


        } catch (Exception e) {
            final SystemMessageBuilder message = new SystemMessageBuilder().setMessage("Error:" + e.getMessage()).setLife(5000).setType(MessageType.SIMPLE_MESSAGE).setSeverity(MessageSeverity.ERROR);

            SystemMessageEventUtil.getInstance().pushMessage(message.create(), List.of(user.getUserId()));
            Logger.warn(this.getClass(), "Error:" + e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private JSONObject openAIRequest(Contentlet workingContentlet) throws Exception {
        Context ctx = getMockContext(workingContentlet, user);

        final String parsedPrompt = VelocityUtil.eval(prompt, ctx);

        JSONObject openAIResponse = CompletionsAPI.impl().raw(buildRequest(parsedPrompt, model, temperature));

        String response = openAIResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

        return parseJsonResponse(response);
    }

    boolean setJsonProperties(Contentlet contentlet, JSONObject json, boolean overwriteField) {
        com.dotcms.ai.util.Logger.info(this.getClass(), "Setting json:\n" + json.toString(2));
        boolean contentNeedsSaving = false;
        for (Map.Entry entry : (Set<Map.Entry>) json.getAsMap().entrySet()) {
            if (overwriteField || UtilMethods.isEmpty(contentlet.getStringProperty(entry.getKey().toString()))) {

                if (ConfigService.INSTANCE.config().getConfigBoolean(AppKeys.DEBUG_LOGGING)) {
                    Logger.info(this.getClass(), "setting field:" + entry.getKey().toString() + " to " + entry.getValue());
                }

                Field field = contentlet.getContentType().fieldMap().get(entry.getKey().toString());

                String value = String.valueOf(entry.getValue());
                if (ContentToStringUtil.impl.get().isHtml(value)) {
                    value = value.replaceAll("\\s+", " ");
                    value = value.replaceAll("\\>\\s+\\<", "><");
                }


                contentlet.setProperty(entry.getKey().toString(), value);
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
        com.dotcms.ai.util.Logger.debug(this.getClass(), "Open AI Request:\n" + json.toString(2));
        return json;
    }

    private JSONObject parseJsonResponse(String response) {

        Logger.debug(this.getClass(), "---- response ----- ");
        Logger.debug(this.getClass(), response);
        Logger.debug(this.getClass(), "---- response ----- ");
        response = response.replaceAll("\\R+", " ");
        response = response.substring(response.indexOf("{"), response.lastIndexOf("}") + 1);

        String finalResponse = response;
        return Try.of(() -> new JSONObject(finalResponse)).onFailure(e -> Logger.warn(this.getClass(), e.getMessage())).getOrElse(new JSONObject());


    }


}
