/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.io.entity.StringEntity;

public abstract class HttpClientUtils {

    /**
     * Converts a cURL command to an Apache HttpClient 5 request
     * 
     * @param curlCommand The cURL command string
     * @return HttpUriRequest object
     */
    public static HttpUriRequest curlToHttpRequest(String curlCommand) {
        // Remove 'curl' if present at the start
        curlCommand = curlCommand.trim();
        if (curlCommand.startsWith("curl ")) {
            curlCommand = curlCommand.substring(5).trim();
        }

        String method = "GET"; // default method
        String url = null;
        Map<String, String> headers = new HashMap<>();
        String body = null;

        // Split the command into parts, respecting quotes
        List<String> parts = splitCommand(curlCommand);

        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);

            switch (part) {
                case "-H":
                case "--header":
                    if (i + 1 < parts.size()) {
                        String header = parts.get(++i);
                        String[] headerParts = header.split(":", 2);
                        if (headerParts.length == 2) {
                            headers.put(headerParts[0].trim(), headerParts[1].trim());
                        }
                    }
                    break;

                case "-X":
                case "--request":
                    if (i + 1 < parts.size()) {
                        method = parts.get(++i);
                    }
                    break;

                case "-d":
                case "--data":
                case "--data-raw":
                    if (i + 1 < parts.size()) {
                        body = parts.get(++i);
                    }
                    break;

                default:
                    if (!part.startsWith("-") && url == null) {
                        url = part;
                    }
                    break;
            }
        }

        if (url == null) {
            throw new IllegalArgumentException("No URL found in cURL command");
        }

        // Create appropriate request object based on method
        HttpUriRequestBase request = createRequest(method, url);

        // Add headers
        headers.forEach(request::addHeader);

        // Add body if present
        if (body != null) {
            // Try to detect JSON content if Content-Type is not specified
            if (!headers.containsKey("Content-Type") && isJsonContent(body)) {
                request.setHeader("Content-Type", "application/json");
            }
            request.setEntity(new StringEntity(body));
        }

        return request;
    }

    /**
     * Creates the appropriate HttpRequest object based on the HTTP method
     */
    private static HttpUriRequestBase createRequest(String method, String url) {
        switch (method.toUpperCase()) {
            case "GET":
                return new HttpGet(url);
            case "POST":
                return new HttpPost(url);
            case "PUT":
                return new HttpPut(url);
            case "DELETE":
                return new HttpDelete(url);
            case "PATCH":
                return new HttpPatch(url);
            case "HEAD":
                return new HttpHead(url);
            case "OPTIONS":
                return new HttpOptions(url);
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    /**
     * Simple check to detect if content appears to be JSON
     */
    private static boolean isJsonContent(String content) {
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * Helper method to split cURL command respecting quotes
     */
    private static List<String> splitCommand(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if ((c == '"' || c == '\'') && (i == 0 || command.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                    quoteChar = 0;
                } else {
                    current.append(c);
                }
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }
}
