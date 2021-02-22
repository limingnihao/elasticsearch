/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.search.rescore;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryRewriteContext;
import org.elasticsearch.index.query.SearchExecutionContext;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.rescore.ScriptRescorer.ScriptRescoreContext;

import java.io.IOException;

public class ScriptRescorerBuilder extends RescorerBuilder<ScriptRescorerBuilder>{
    public static final String NAME = "script";
    private static final ParseField SCRIPT_FIELD = new ParseField("script");
    private final Script script;

    public ScriptRescorerBuilder(Script script) {
        this.script = script;
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public RescorerBuilder<ScriptRescorerBuilder> rewrite(QueryRewriteContext queryRewriteContext) throws IOException {
        return this;
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        script.writeTo(out);
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.field(SCRIPT_FIELD.getPreferredName(), script);
        builder.endObject();
    }

    @Override
    protected RescoreContext innerBuildContext(int windowSize, SearchExecutionContext context) throws IOException {
        ScriptRescoreContext scriptRescoreContext = new ScriptRescoreContext(windowSize);
        return scriptRescoreContext;
    }


}
