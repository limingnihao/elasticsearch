/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.search.rescore;

import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toSet;

public class ScriptRescorer implements Rescorer {

    public static final Rescorer INSTANCE = new ScriptRescorer();

    @Override
    public TopDocs rescore(TopDocs topDocs, IndexSearcher searcher, RescoreContext rescoreContext) throws IOException {

        assert rescoreContext != null;
        if (topDocs == null || topDocs.scoreDocs.length == 0) {
            return topDocs;
        }

        final ScriptRescorer.ScriptRescoreContext rescore = (ScriptRescorer.ScriptRescoreContext) rescoreContext;

        org.apache.lucene.search.Rescorer rescorer = new org.apache.lucene.search.QueryRescorer(rescore.query()) {

            @Override
            protected float combine(float firstPassScore, boolean secondPassMatches, float secondPassScore) {
                return secondPassScore;
            }
        };

        // First take top slice of incoming docs, to be rescored:
        TopDocs topNFirstPass = topN(topDocs, rescoreContext.getWindowSize());

        // Save doc IDs for which rescoring was applied to be used in score explanation
        Set<Integer> topNDocIDs = Collections.unmodifiableSet(
                Arrays.stream(topNFirstPass.scoreDocs).map(scoreDoc -> scoreDoc.doc).collect(toSet()));
        rescoreContext.setRescoredDocs(topNDocIDs);

        // Rescore them:
        TopDocs rescored = rescorer.rescore(searcher, topNFirstPass, rescoreContext.getWindowSize());

        // Splice back to non-topN hits and resort all of them:
        return combine(topDocs, rescored, (QueryRescorer.QueryRescoreContext) rescoreContext);
    }

    @Override
    public Explanation explain(int topLevelDocId, IndexSearcher searcher, RescoreContext rescoreContext, Explanation sourceExplanation) throws IOException {
        return null;
    }

    private static final Comparator<ScoreDoc> SCORE_DOC_COMPARATOR = new Comparator<ScoreDoc>() {
        @Override
        public int compare(ScoreDoc o1, ScoreDoc o2) {
            int cmp = Float.compare(o2.score, o1.score);
            return cmp == 0 ? Integer.compare(o1.doc, o2.doc) : cmp;
        }
    };

    /**
     * Returns a new {@link TopDocs} with the topN from the incoming one, or the same TopDocs if the number of hits is already &lt;=
     * topN.
     */
    private TopDocs topN(TopDocs in, int topN) {
        if (in.scoreDocs.length < topN) {
            return in;
        }

        ScoreDoc[] subset = new ScoreDoc[topN];
        System.arraycopy(in.scoreDocs, 0, subset, 0, topN);

        return new TopDocs(in.totalHits, subset);
    }

    /**
     * Modifies incoming TopDocs (in) by replacing the top hits with resorted's hits, and then resorting all hits.
     */
    private TopDocs combine(TopDocs in, TopDocs resorted, QueryRescorer.QueryRescoreContext ctx) {

        System.arraycopy(resorted.scoreDocs, 0, in.scoreDocs, 0, resorted.scoreDocs.length);
        if (in.scoreDocs.length > resorted.scoreDocs.length) {
            // These hits were not rescored (beyond the rescore window), so we treat them the same as a hit that did get rescored but did
            // not match the 2nd pass query:
            for (int i = resorted.scoreDocs.length; i < in.scoreDocs.length; i++) {
                // TODO: shouldn't this be up to the ScoreMode?  I.e., we should just invoke ScoreMode.combine, passing 0.0f for the
                // secondary score?
                in.scoreDocs[i].score *= ctx.queryWeight();
            }

            // TODO: this is wrong, i.e. we are comparing apples and oranges at this point.  It would be better if we always rescored all
            // incoming first pass hits, instead of allowing recoring of just the top subset:
            Arrays.sort(in.scoreDocs, SCORE_DOC_COMPARATOR);
        }
        return in;
    }

    public static class ScriptRescoreContext extends RescoreContext {
        private Query query;

        public ScriptRescoreContext(int windowSize) {
            super(windowSize, ScriptRescorer.INSTANCE);
        }

        public void setQuery(Query query) {
            this.query = query;
        }

        @Override
        public List<Query> getQueries() {
            return Collections.singletonList(query);
        }

        public Query query() {
            return query;
        }

    }
}
