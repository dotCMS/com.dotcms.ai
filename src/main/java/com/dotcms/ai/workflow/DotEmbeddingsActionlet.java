package com.dotcms.ai.workflow;


import com.dotcms.ai.api.EmbeddingsAPI;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
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
    private static final String DOT_EMBEDDING_INDEX = "default";
    @Override
    public List<WorkflowActionletParameter> getParameters() {

        final List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter(DOT_EMBEDDING_TYPES_FIELDS, "List of {contentType}.{fieldVar} to use to generate the embeddings.  Each type.field should be on its own line, e.g. blog.title<br>blog.blogContent", "", false));
        params.add(new WorkflowActionletParameter(DOT_EMBEDDING_INDEX, "Index Name", "", false));
        params.add(new WorkflowActionletParameter(DOT_EMBEDDING_ACTION, "INSERT|DELETE", "INSERT", false));

        return params;
    }

    @Override
    public String getName() {
        return "OpenAI Embeddings";
    }

    @Override
    public String getHowTo() {
        return "List of {contentType}.{fieldVar} to use to generate the embeddings.  Each type.field should be on its own line, e.g. blog.title<br>blog.blogContent.  If no contentType.fields are specified here, dotCMS will attempt to guess how the content should be indexed based on its content type and/or its fields.";
    }


    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final ContentType type = processor.getContentlet().getContentType();


        final String updateOrDelete = "DELETE".equalsIgnoreCase(params.get(DOT_EMBEDDING_ACTION).getValue()) ? "DELETE" : "INSERT";
        final Host host = Try.of(() -> APILocator.getHostAPI().find(contentlet.getHost(), APILocator.systemUser(), false)).getOrNull();
        final String indexName = UtilMethods.isSet(params.get(DOT_EMBEDDING_INDEX).getValue()) ? params.get(DOT_EMBEDDING_INDEX).getValue(): "default";
        HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if(request == null){
            return;
        }

        List<Field> fields = parseTypesAndFields(type, params.get(DOT_EMBEDDING_TYPES_FIELDS).getValue());

        if (fields.isEmpty()) {
            EmbeddingsAPI.impl().generateEmbeddingsforContent(contentlet, indexName);
        }


        for (Field field : fields) {
            Logger.info(DotEmbeddingsActionlet.class, "found field:" + type.variable() + "." + field.variable());
            EmbeddingsAPI.impl().generateEmbeddingsforContent(contentlet, Optional.of(field), indexName);
        }


    }


    private List<Field> parseTypesAndFields(@NotNull ContentType type, final String typeAndFieldParam) {

        if (UtilMethods.isEmpty(typeAndFieldParam)) {
            return List.of();
        }


        final List<Field> typesAndFields = new ArrayList<>();
        final String[] typeFieldArr = typeAndFieldParam.trim().split("\\s*,\\s*");
        List<Field> fields = Arrays.stream(typeFieldArr).filter(s -> s.toLowerCase().startsWith(type.variable().toLowerCase())).map(s -> type.fieldMap().get(s.split(".")[1])).filter(Objects::nonNull).collect(Collectors.toList());


        return fields;
    }


}
