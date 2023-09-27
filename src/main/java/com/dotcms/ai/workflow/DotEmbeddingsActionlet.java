package com.dotcms.embeddings.workflow;


import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.embeddings.api.EmbeddingsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DotEmbeddingsActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    private static final String DOT_EMBEDDING_TYPES_FIELDS = "dotEmbeddingTypes";
    private static final String DOT_EMBEDDING_ACTION = "dotEmbeddingAction";

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        final List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter(DOT_EMBEDDING_TYPES_FIELDS, "List of {contentType}.{fieldVar} to use to generate the embeddings.  Each type.field should be on its own line, e.g. blog.title<br>blog.blogContent", "", false));
        params.add(new WorkflowActionletParameter(DOT_EMBEDDING_ACTION, "CREATE|DELETE", "CREATE", false));
        return params;
    }

    @Override
    public String getName() {
        return "Generate OpenAI Embeddings";
    }

    @Override
    public String getHowTo() {
        return "List of {contentType}.{fieldVar} to use to generate the embeddings.  Each type.field should be on its own line, e.g. blog.title<br>blog.blogContent";
    }


    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final ContentType type = processor.getContentlet().getContentType();


        final String updateOrDelete = "DELETE".equalsIgnoreCase(params.get(DOT_EMBEDDING_ACTION).getValue()) ? "DELETE" : "CREATE";
        final Host host = Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false)).getOrNull();


        List<Field> fields = parseTypesAndFields(type, params.get(DOT_EMBEDDING_TYPES_FIELDS).getValue());

        for (Field field : fields) {
            Logger.info(DotEmbeddingsActionlet.class, "found field:" + type.variable() + "." + field.variable());
            EmbeddingsAPI.impl().generateEmbeddingsforContentAndField(contentlet, field);


        }


    }


    private List<Field> parseTypesAndFields(@NotNull ContentType type, final String typeAndFieldParam) {

        if (UtilMethods.isEmpty(typeAndFieldParam)) {
            Optional<Field> firstField = type.fields().stream().filter(f -> (f instanceof TextAreaField || f instanceof WysiwygField || f instanceof StoryBlockField)).findFirst();
            return firstField.isPresent() ? List.of(firstField.get()) : List.of();

        }


        final List<Field> typesAndFields = new ArrayList<>();
        final String[] typeFieldArr = typeAndFieldParam.trim().split("\\s*,\\s*");
        List<Field> fields = Arrays.stream(typeFieldArr)
                .filter(s -> s.toLowerCase().startsWith(type.variable().toLowerCase()))
                .map(s -> type.fieldMap().get(s.split(".")[1]))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        return fields;
    }


}
