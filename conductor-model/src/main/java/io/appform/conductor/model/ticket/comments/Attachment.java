/*
 * Copyright (c) 2021 Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appform.conductor.model.ticket.comments;

import com.google.common.net.MediaType;
import lombok.Value;

import java.net.URL;
import java.util.Date;

/**
 * An uploaded file, for example passport etc
 */
@Value
public class Attachment {

    /**
     * Globally unique identifier for the attachment
     */
    String id;

    /**
     * Creator of this attachment
     */
    String creator;

    /**
     * MIME type for the file
     */
    MediaType type;

    /**
     * URL coordinate for the file
     */
    URL url;

    /**
     * Size of the file in bytes
     */
    long sizeInBytes;

    /**
     * Is the file encrypted?
     */
    boolean encrypted;

    /**
     * First upload date
     */
    Date created;

    /**
     * Latest upload date
     */
    Date updated;
}
