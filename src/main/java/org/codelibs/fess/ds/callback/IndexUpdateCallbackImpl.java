/*
 * Copyright 2012-2019 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.ds.callback;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.stream.StreamUtil;
import org.codelibs.fess.es.client.FessEsClient;
import org.codelibs.fess.exception.DataStoreException;
import org.codelibs.fess.helper.CrawlingInfoHelper;
import org.codelibs.fess.helper.IndexingHelper;
import org.codelibs.fess.helper.SearchLogHelper;
import org.codelibs.fess.helper.SystemHelper;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.util.DocList;
import org.codelibs.fess.util.DocumentUtil;

public class IndexUpdateCallbackImpl implements IndexUpdateCallback {
    private static final Logger logger = LogManager.getLogger(IndexUpdateCallbackImpl.class);

    protected AtomicLong documentSize = new AtomicLong(0);

    protected volatile long executeTime = 0;

    protected final DocList docList = new DocList();

    protected long maxDocumentRequestSize;

    @PostConstruct
    public void init() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initialize {}", this.getClass().getSimpleName());
        }
        maxDocumentRequestSize = ComponentUtil.getFessConfig().getIndexerDataMaxDocumentRequestSizeAsInteger().longValue();
    }

    /* (non-Javadoc)
     * @see org.codelibs.fess.ds.callback.IndexUpdateCallback#store(java.util.Map)
     */
    @Override
    public void store(final Map<String, String> paramMap, final Map<String, Object> dataMap) {
        final long startTime = System.currentTimeMillis();
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final FessEsClient fessEsClient = ComponentUtil.getFessEsClient();

        if (logger.isDebugEnabled()) {
            logger.debug("Adding {}", dataMap);
        }

        //   required check
        final Object urlObj = dataMap.get(fessConfig.getIndexFieldUrl());
        if (urlObj == null) {
            throw new DataStoreException("url is null. dataMap=" + dataMap);
        }

        final IndexingHelper indexingHelper = ComponentUtil.getIndexingHelper();
        final CrawlingInfoHelper crawlingInfoHelper = ComponentUtil.getCrawlingInfoHelper();
        dataMap.put(fessConfig.getIndexFieldId(), crawlingInfoHelper.generateId(dataMap));

        final String url = dataMap.get(fessConfig.getIndexFieldUrl()).toString();

        if (fessConfig.getIndexerClickCountEnabledAsBoolean()) {
            addClickCountField(dataMap, url, fessConfig.getIndexFieldClickCount());
        }

        if (fessConfig.getIndexerFavoriteCountEnabledAsBoolean()) {
            addFavoriteCountField(dataMap, url, fessConfig.getIndexFieldFavoriteCount());
        }

        final Set<String> matchedLabelSet = ComponentUtil.getLabelTypeHelper().getMatchedLabelValueSet(url);
        if (!matchedLabelSet.isEmpty()) {
            final Set<String> newLabelSet = new HashSet<>();
            final String[] oldLabels = DocumentUtil.getValue(dataMap, fessConfig.getIndexFieldLabel(), String[].class);
            StreamUtil.stream(oldLabels).of(stream -> stream.forEach(newLabelSet::add));
            matchedLabelSet.stream().forEach(newLabelSet::add);
            dataMap.put(fessConfig.getIndexFieldLabel(), newLabelSet.toArray(new String[newLabelSet.size()]));
        }

        if (!dataMap.containsKey(fessConfig.getIndexFieldDocId())) {
            final SystemHelper systemHelper = ComponentUtil.getSystemHelper();
            dataMap.put(fessConfig.getIndexFieldDocId(), systemHelper.generateDocId(dataMap));
        }

        ComponentUtil.getLanguageHelper().updateDocument(dataMap);

        synchronized (docList) {
            docList.add(dataMap);
            if (logger.isDebugEnabled()) {
                logger.debug("Added the document. The number of a document cache is {}.", docList.size());
            }

            final Long contentLength = DocumentUtil.getValue(dataMap, fessConfig.getIndexFieldContentLength(), Long.class);
            if (contentLength != null) {
                docList.addContentSize(contentLength.longValue());
                if (docList.getContentSize() >= maxDocumentRequestSize) {
                    indexingHelper.sendDocuments(fessEsClient, docList);
                }
            } else if (docList.size() >= fessConfig.getIndexerDataMaxDocumentCacheSizeAsInteger().intValue()) {
                indexingHelper.sendDocuments(fessEsClient, docList);
            }
            executeTime += System.currentTimeMillis() - startTime;
        }

        documentSize.getAndIncrement();

        if (logger.isDebugEnabled()) {
            logger.debug("The number of an added document is {}.", documentSize.get());
        }

    }

    @Override
    public void commit() {
        synchronized (docList) {
            if (!docList.isEmpty()) {
                final IndexingHelper indexingHelper = ComponentUtil.getIndexingHelper();
                final FessEsClient fessEsClient = ComponentUtil.getFessEsClient();
                indexingHelper.sendDocuments(fessEsClient, docList);
            }
        }
    }

    protected void addClickCountField(final Map<String, Object> doc, final String url, final String clickCountField) {
        final SearchLogHelper searchLogHelper = ComponentUtil.getSearchLogHelper();
        final int count = searchLogHelper.getClickCount(url);
        doc.put(clickCountField, count);
        if (logger.isDebugEnabled()) {
            logger.debug("Click Count: {}, url: {}", count, url);
        }
    }

    protected void addFavoriteCountField(final Map<String, Object> doc, final String url, final String favoriteCountField) {
        final SearchLogHelper searchLogHelper = ComponentUtil.getSearchLogHelper();
        final long count = searchLogHelper.getFavoriteCount(url);
        doc.put(favoriteCountField, count);
        if (logger.isDebugEnabled()) {
            logger.debug("Favorite Count: {}, url: {}", count, url);
        }
    }

    @Override
    public long getDocumentSize() {
        return documentSize.get();
    }

    @Override
    public long getExecuteTime() {
        return executeTime;
    }

}
