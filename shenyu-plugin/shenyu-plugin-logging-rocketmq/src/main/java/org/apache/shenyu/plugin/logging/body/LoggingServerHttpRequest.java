/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.plugin.logging.body;

import org.apache.shenyu.plugin.logging.entity.ShenyuRequestLog;
import org.apache.shenyu.plugin.logging.utils.LogCollectConfigUtils;
import org.apache.shenyu.plugin.logging.utils.LogCollectUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.util.annotation.NonNull;

/**
 * decorate ServerHttpRequest for read body.
 */
public class LoggingServerHttpRequest extends ServerHttpRequestDecorator {
    
    private final ShenyuRequestLog logInfo;

    public LoggingServerHttpRequest(final ServerHttpRequest delegate, final ShenyuRequestLog logInfo) {
        super(delegate);
        this.logInfo = logInfo;
    }

    /**
     * get request body.
     *
     * @return Flux
     */
    @Override
    @NonNull
    public Flux<DataBuffer> getBody() {
        BodyWriter writer = new BodyWriter();
        boolean collectRequestBody = LogCollectConfigUtils.getLogFieldSwitchConfig().isRequestBody();
        return super.getBody().doOnNext(dataBuffer -> {
            if (LogCollectUtils.isNotBinaryType(getHeaders()) && collectRequestBody) {
                writer.write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
            }
        }).doFinally(signal -> {
            int size = writer.size();
            String body = writer.output();
            boolean requestBodyTooLarge = LogCollectConfigUtils.isRequestBodyTooLarge(size);
            if (size == 0 || requestBodyTooLarge) {
                return;
            }
            logInfo.setRequestBody(body);
        });
    }
}
