package com.dotcms.ai.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class EmbeddingsToolInfo extends ServletToolInfo {

    @Override
    public String getKey () {
        return "dotcdn";
    }

    @Override
    public String getScope () {
        return ViewContext.REQUEST;
    }

    @Override
    public String getClassname () {
        return EmbeddingsTool.class.getName();
    }

    @Override
    public Object getInstance ( Object initData ) {

        EmbeddingsTool viewTool = new EmbeddingsTool();
        viewTool.init( initData );

        setScope( ViewContext.REQUEST );

        return viewTool;
    }

}
