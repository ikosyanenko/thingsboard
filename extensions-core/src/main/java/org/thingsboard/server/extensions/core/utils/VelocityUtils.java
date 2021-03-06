/**
 * Copyright © 2016 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.extensions.core.utils;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.tools.generic.DateTool;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.msg.session.FromDeviceMsg;
import org.thingsboard.server.extensions.api.device.DeviceAttributes;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.core.filter.DeviceAttributesFilter;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrew Shvayka
 */
public class VelocityUtils {

    public static Template create(String source, String templateName) throws ParseException {
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        StringReader reader = new StringReader(source);
        SimpleNode node = runtimeServices.parse(reader, templateName);
        Template template = new Template();
        template.setRuntimeServices(runtimeServices);
        template.setData(node);
        template.initDocument();
        return template;
    }

    public static String merge(Template template, VelocityContext context) {
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }

    public static VelocityContext createContext(RuleProcessingMetaData metadata) {
        VelocityContext context = new VelocityContext();
        metadata.getValues().forEach((k, v) -> context.put(k, v));
        return context;
    }

    public static VelocityContext createContext(DeviceAttributes deviceAttributes, FromDeviceMsg payload) {
        VelocityContext context = new VelocityContext();
        context.put("date", new DateTool());
        pushAttributes(context, deviceAttributes.getClientSideAttributes(), DeviceAttributesFilter.CLIENT_SIDE);
        pushAttributes(context, deviceAttributes.getServerSideAttributes(), DeviceAttributesFilter.SERVER_SIDE);
        pushAttributes(context, deviceAttributes.getServerSidePublicAttributes(), DeviceAttributesFilter.SHARED);

        switch (payload.getMsgType()) {
            case POST_TELEMETRY_REQUEST:
                pushTsEntries(context, (TelemetryUploadRequest) payload);
                break;
        }

        return context;
    }

    private static void pushTsEntries(VelocityContext context, TelemetryUploadRequest payload) {
        payload.getData().forEach((k, vList) -> {
            vList.forEach(v -> {
                context.put(v.getKey(), new BasicTsKvEntry(k, v));
            });
        });
    }

    private static void pushAttributes(VelocityContext context, Collection<AttributeKvEntry> deviceAttributes, String prefix) {
        Map<String, String> values = new HashMap<>();
        deviceAttributes.forEach(v -> values.put(v.getKey(), v.getValueAsString()));
        context.put(prefix, values);
    }
}
