/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.search.rescore;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryRewriteContext;
import org.elasticsearch.index.query.SearchExecutionContext;
import org.elasticsearch.script.FilterScript;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchService;
import org.elasticsearch.search.rescore.ScriptRescorer.ScriptRescoreContext;

import java.io.IOException;
import java.util.Objects;

public class ScriptRescorerBuilder extends RescorerBuilder<ScriptRescorerBuilder>{
    public static final String NAME = "script";
    private static final ParseField SCRIPT_FIELD = new ParseField("script");
    private final Script script;

    public ScriptRescorerBuilder(Script script) {
        this.script = script;
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
        if (context.allowExpensiveQueries() == false) {
            throw new ElasticsearchException("[script] queries cannot be executed when '" +
                SearchService.ALLOW_EXPENSIVE_QUERIES.getKey() + "' is set to false.");
        }
        FilterScript.Factory factory = context.compile(script, FilterScript.CONTEXT);
        FilterScript.LeafFactory filterScript = factory.newFactory(script.getParams(), context.lookup());
        ScriptQuery query = new ScriptQuery(script, filterScript);
        ScriptRescoreContext scriptRescoreContext = new ScriptRescoreContext(windowSize);
        scriptRescoreContext.setQuery(query);
        return scriptRescoreContext;
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public RescorerBuilder<ScriptRescorerBuilder> rewrite(QueryRewriteContext queryRewriteContext) throws IOException {
        return this;
    }

    static class ScriptQuery extends Query {

        final Script script;
        final FilterScript.LeafFactory filterScript;

        ScriptQuery(Script script, FilterScript.LeafFactory filterScript) {
            this.script = script;
            this.filterScript = filterScript;
        }

        @Override
        public String toString(String field) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("ScriptQuery(");
            buffer.append(script);
            buffer.append(")");
            return buffer.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (sameClassAs(obj) == false) {
                return false;
            }
            ScriptQuery other = (ScriptQuery) obj;
            return Objects.equals(script, other.script);
        }

        @Override
        public int hashCode() {
            int h = classHash();
            h = 31 * h + script.hashCode();
            return h;
        }

        @Override
        public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
            return new ConstantScoreWeight(this, boost) {

                @Override
                public Scorer scorer(LeafReaderContext context) throws IOException {
                    DocIdSetIterator approximation = DocIdSetIterator.all(context.reader().maxDoc());
                    final FilterScript leafScript = filterScript.newInstance(context);
                    TwoPhaseIterator twoPhase = new TwoPhaseIterator(approximation) {

                        @Override
                        public boolean matches() throws IOException {
                            leafScript.setDocument(approximation.docID());
                            return leafScript.execute();
                        }

                        @Override
                        public float matchCost() {
                            // TODO: how can we compute this?
                            return 1000f;
                        }
                    };
                    return new ConstantScoreScorer(this, score(), scoreMode, twoPhase);
                }

                @Override
                public boolean isCacheable(LeafReaderContext ctx) {
                    // TODO: Change this to true when we can assume that scripts are pure functions
                    // ie. the return value is always the same given the same conditions and may not
                    // depend on the current timestamp, other documents, etc.
                    return false;
                }
            };
        }
    }

}
