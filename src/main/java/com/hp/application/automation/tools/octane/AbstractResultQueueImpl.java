/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.application.automation.tools.octane;

import com.squareup.tape.FileObjectQueue;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Created by benmeior on 11/21/2016.
 */

public abstract class AbstractResultQueueImpl implements ResultQueue {

    private static final int RETRY_COUNT = 3;

    private FileObjectQueue<QueueItem> queue;

    private QueueItem currentItem;

    protected void init(File queueFile) throws IOException {
        queue = new FileObjectQueue<QueueItem>(queueFile, new JsonConverter());
    }

    @Override
    public synchronized QueueItem peekFirst() {
        if (currentItem == null) {
            currentItem = queue.peek();
        }
        return currentItem;
    }

    @Override
    public synchronized boolean failed() {
        if (currentItem != null) {
            boolean retry;
            if (++currentItem.failCount <= RETRY_COUNT) {
                queue.add(currentItem);
                retry = true;
            } else {
                retry = false;
            }

            remove();

            return retry;
        } else {
            throw new IllegalStateException("no outstanding item");
        }
    }

    @Override
    public synchronized void remove() {
        if (currentItem != null) {
            queue.remove();
            currentItem = null;
        } else {
            throw new IllegalStateException("no outstanding item");
        }
    }

    @Override
    public synchronized void add(String projectName, int buildNumber) {
        queue.add(new QueueItem(projectName, buildNumber));
    }

    @Override
    public synchronized void add(String projectName, int buildNumber, String workspace) {
        queue.add(new QueueItem(projectName, buildNumber, workspace));
    }

    private static class JsonConverter implements FileObjectQueue.Converter<QueueItem> {

        @Override
        public QueueItem from(byte[] bytes) throws IOException {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(IOUtils.toString(new ByteArrayInputStream(bytes)));
            return objectFromJson(json);
        }

        @Override
        public void toStream(QueueItem item, OutputStream bytes) throws IOException {
            JSONObject json = jsonFromObject(item);
            OutputStreamWriter writer = new OutputStreamWriter(bytes);
            writer.append(json.toString());
            writer.close();
        }

        private static QueueItem objectFromJson(JSONObject json) {
            return json.containsKey("workspace") ?
                    new QueueItem(
                            json.getString("project"),
                            json.getInt("build"),
                            json.getInt("count"),
                            json.getString("workspace")) :
                    new QueueItem(
                            json.getString("project"),
                            json.getInt("build"),
                            json.getInt("count"));
        }

        private static JSONObject jsonFromObject(QueueItem item) {
            JSONObject json = new JSONObject();
            json.put("project", item.projectName);
            json.put("build", item.buildNumber);
            json.put("count", item.failCount);
            json.put("workspace", item.workspace);
            return json;
        }
    }
}
