package com.dotcms.ai.workflow;

import com.dotcms.ai.api.CompletionsAPI;
import com.dotcms.ai.api.ContentToStringUtil;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.OpenAIModel;
import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OpenAIModifyContentActionlet extends WorkFlowActionlet {

    public final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);
    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        return List.of(
                new WorkflowActionletParameter("fieldToRead", "The field with the content to use in your prompt", "", true),
                new WorkflowActionletParameter("fieldToWrite", "The field where you want to write the results.  " +
                        "<br>If your response is being returned as a json object, this field can be left blank" +
                        "<br>and the keys of the json object will be used to update the content fields.", "", false),
                new WorkflowActionletParameter("overwriteField", "Overwrite existing content (true|false)", "true", true),
                new WorkflowActionletParameter("openAIPrompt", "The prompt that will be sent to OpenAI", "We need an attractive search result in Google. Return a json object that includes the fields \"pageTitle\" for a meta title of less than 55 characters and \"metaDescription\" for the meta description of less than 300 characters using this content:\\n\\n${fieldContent}\\n\\n", true),
                new WorkflowActionletParameter("runDelay", "Update the content asynchronously, after X seconds. O means run in-process", "5", true)
        );
    }

    @Override
    public String getName() {
        return "OpenAI Modify Content";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will send the value of the 'openAIPrompt' field to OpenAI and write the returned results to a field or fields of your choosing.  If OpenAI returns a JSON object, the key/values of that JSON will be merged with your content's fields.  The prompt can also take velocity (content can be referenced as $dotContentMap)";
    }

    @Override
    public void executePreAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final int runDelay = Try.of(() -> Integer.parseInt(params.get("runDelay").getValue())).getOrElse(5);

        if (runDelay>0) {
            scheduledExecutorService.schedule(new UpdateContent(processor, params), runDelay, TimeUnit.SECONDS);
        } else {
            new UpdateContent(processor, params).run();
        }
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
        // Not used

    }

    private JSONObject buildRequest(String prompt) {
        AppConfig config = ConfigService.INSTANCE.config();

        int maxTokenSize = OpenAIModel.resolveModel(config.getConfig(AppKeys.COMPLETION_MODEL)).maxTokens;

        float defaultTemperature = config.getConfigFloat(AppKeys.COMPLETION_TEMPERATURE);

        // aggregate matching results into text
        StringBuilder supportingContent = new StringBuilder();


        JSONArray messages = new JSONArray();
        messages.add(Map.of("role", "user", "content", prompt));
        JSONObject json = new JSONObject();
        json.put("messages", messages);
        json.put("model", config.getConfig(AppKeys.COMPLETION_MODEL));
        json.put("temperature", defaultTemperature);
        json.put("stream", false);

        return json;
    }

    class UpdateContent implements Runnable {
        final WorkflowProcessor processor;
        final Map<String, WorkflowActionClassParameter> params;


        UpdateContent(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) {
            this.processor = processor;
            this.params = params;

        }

        @Override
        public void run() {
            Contentlet contentlet = processor.getContentlet();
            ContentType type = contentlet.getContentType();


            final int runDelay = Try.of(() -> Integer.parseInt(params.get("runDelay").getValue())).getOrElse(5);
            final boolean overwriteField = Try.of(() -> Boolean.parseBoolean(params.get("overwriteField").getValue())).getOrElse(true);
            final Optional<Field> fieldToRead = Try.of(() -> type.fieldMap().get(params.get("fieldToRead").getValue())).toJavaOptional();
            final Optional<Field> fieldToWrite = Try.of(() -> type.fieldMap().get(params.get("fieldToWrite").getValue())).toJavaOptional();
            final String promptIn = params.get("openAIPrompt").getValue();


            final Host host = Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), true)).getOrElse(APILocator.systemHost());


            if (UtilMethods.isEmpty(promptIn)) {
                Logger.info(OpenAIModifyContentActionlet.class, "no prompt found");
                return;
            }

            Optional<String> content = ContentToStringUtil.impl.get().parseField(contentlet, fieldToRead);
            if (content.isEmpty() || UtilMethods.isEmpty(content.get())) {
                Logger.info(OpenAIModifyContentActionlet.class, "no content found for field:" + type.variable() + "." + fieldToWrite.get().variable());
                return;
            }

            try {
                HttpServletRequest requestProxy = new FakeHttpRequest("localhost", null).request();
                HttpServletResponse responseProxy = new BaseResponse().response();

                Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
                ctx.put("fieldContent", content.get());
                ctx.put("dotContentMap", new ContentMap(contentlet, processor.getUser(), PageMode.EDIT_MODE, host, ctx));

                String finalPrompt = VelocityUtil.eval(promptIn, ctx);


                JSONObject openAIResponse = CompletionsAPI.impl().raw(buildRequest(finalPrompt));


                String response = openAIResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");


                if (StringUtils.isJson(response)) {
                    JSONObject responseJson = new JSONObject(response);

                    responseJson.getAsMap().entrySet().forEach(e -> {
                        Map.Entry entry = (Map.Entry) e;

                        if (overwriteField || UtilMethods.isEmpty(contentlet.getStringProperty(entry.getKey().toString()))) {
                            contentlet.setProperty(entry.getKey().toString(), entry.getValue());
                        }
                    });
                }

                if (fieldToWrite.isPresent() && fieldToWrite.get() instanceof TagField) {
                    String[] splitter = response.trim().split("[\n,]");
                    for (String x : splitter) {
                        try {
                            APILocator.getTagAPI().addContentletTagInode(x, contentlet.getInode(), contentlet.getHost(), fieldToWrite.get().variable());
                        } catch (Exception e) {
                            Logger.warnAndDebug(this.getClass(), "Unable to write tag :" + x + " to contentlet with inode " + contentlet.getInode() + " :" + e.getMessage(), e);
                        }
                    }
                } else if(fieldToWrite.isPresent()) {
                    if (overwriteField || UtilMethods.isEmpty(contentlet.getStringProperty(fieldToWrite.get().variable()))) {
                        contentlet.setProperty(fieldToWrite.get().variable(), response);
                    }
                }
                processor.setContentlet(contentlet);

                if (runDelay>0) {
                    boolean isPublished = APILocator.getVersionableAPI().isLive(contentlet);
                    new SaveContentActionlet().executeAction(processor, Map.of());
                    if(isPublished){
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

    }


}
