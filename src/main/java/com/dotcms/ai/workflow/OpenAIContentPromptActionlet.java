package com.dotcms.ai.workflow;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.ai.util.OpenAIThreadPool;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import io.vavr.control.Try;

import java.util.List;
import java.util.Map;

public class OpenAIContentPromptActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        return List.of(
                new WorkflowActionletParameter("fieldToWrite", "The field where you want to write the results.  " +
                        "<br>If your response is being returned as a json object, this field can be left blank" +
                        "<br>and the keys of the json object will be used to update the content fields.", "", false),
                new WorkflowActionletParameter("overwriteField", "Overwrite existing content (true|false)", "true", true),
                new WorkflowActionletParameter("openAIPrompt", "The prompt that will be sent to the AI", "We need an attractive search result in Google. Return a json object that includes the fields \"pageTitle\" for a meta title of less than 55 characters and \"metaDescription\" for the meta description of less than 300 characters using this content:\\n\\n${fieldContent}\\n\\n", true),
                new WorkflowActionletParameter("runDelay", "Update the content asynchronously, after X seconds. O means run in-process", "5", true),
                new WorkflowActionletParameter("model", "The AI model to use, defaults to " + ConfigService.INSTANCE.config().getConfig(AppKeys.COMPLETION_MODEL), ConfigService.INSTANCE.config().getConfig(AppKeys.COMPLETION_MODEL), false),
                new WorkflowActionletParameter("temperature", "The AI temperature for the response.  Between .1 and 2.0.  Defaults to " + ConfigService.INSTANCE.config().getConfig(AppKeys.COMPLETION_TEMPERATURE), ConfigService.INSTANCE.config().getConfig(AppKeys.COMPLETION_TEMPERATURE), false)


        );
    }

    @Override
    public String getName() {
        return "AI Content Prompt";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will send the value of the 'openAIPrompt' field to AI and write the returned results to a field or fields of your choosing.  If the AI returns a JSON object, the key/values of that JSON will be merged with your content's fields.  The prompt can also take velocity (content can be referenced as $dotContentMap)";
    }

    @Override
    public void executePreAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
        if (Try.of(() -> Integer.parseInt(params.get("runDelay").getValue())).getOrElse(5) > 0) {
            OpenAIThreadPool.threadPool().submit(new ContentPromptRunner(processor, params));
        }
    }

    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
        if (Try.of(() -> Integer.parseInt(params.get("runDelay").getValue())).getOrElse(5) == 0) {
            new ContentPromptRunner(processor, params).run();
        }
    }


}
