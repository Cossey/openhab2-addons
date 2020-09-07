/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.hamiltonrubbishcollection.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;

/**
 * The {@link API} holds the code to query the API for rubbish dates.
 *
 * @author Stewart Cossey - Initial contribution
 */
@NonNullByDefault
public class API {

    private final Logger logger = LoggerFactory.getLogger(API.class);

    private final HttpClient httpClient;
    private final String address;

    private static final int REQUEST_TIMEOUT = 10;

    public API(HttpClient httpClient, String address) {
        this.httpClient = httpClient;
        this.address = address;
    }

    private @Nullable String day;
    private @Nullable ZonedDateTime redbin;
    private @Nullable ZonedDateTime yellowbin;
    private @Nullable String errMsg = "";
    private ThingStatusDetail errDetail = ThingStatusDetail.NONE;

    public boolean update() {
        try {
            final String url = "https://hccfightthelandfill.azure-api.net/get_Collection_Dates?address_string="
                    + URLEncoder.encode(address, StandardCharsets.UTF_8.toString());

            logger.debug("Fetching data from API at {} with timeout {}", url, REQUEST_TIMEOUT);

            ContentResponse response = httpClient.newRequest(url).timeout(REQUEST_TIMEOUT, TimeUnit.SECONDS).send();

            if (response.getStatus() == 200) {
                String content = response.getContentAsString().trim();
                StringBuffer contentfixed = new StringBuffer(content);
                logger.trace("Got content: {}", contentfixed);
                contentfixed.delete(content.length() - 1, content.length());
                contentfixed.delete(0, 1);

                logger.trace("Parsed content into valid json: {}", contentfixed.toString());

                JsonObject jsonObject = new JsonParser().parse(contentfixed.toString()).getAsJsonObject();

                JsonElement redbinobj = jsonObject.get("RedBin");
                JsonElement yellowbinobj = jsonObject.get("YellowBin");

                if (redbinobj == null || yellowbinobj == null) {
                    logger.debug(
                            "The content did not have the expected RedBin field, assuming invalid premises or address");

                    errDetail = ThingStatusDetail.CONFIGURATION_ERROR;
                    errMsg = "The premises or address is not valid or has rubbish collection available.";
                    return false;
                }
                
                ZonedDateTime localRedBin = ZonedDateTime.parse(redbinobj.getAsString() + "+12:00");
                ZonedDateTime localYellowBin = ZonedDateTime.parse(yellowbinobj.getAsString() + "+12:00");

                if (localRedBin.compareTo(localYellowBin) < 0) {
                    day = localRedBin.getDayOfWeek().toString();
                    logger.trace("Got day of week from RedBin Date");
                } else {
                    day = localYellowBin.getDayOfWeek().toString();
                    logger.trace("Got day of week from YellowBin Date");
                }

                logger.trace("Day {} Red Date {} Yellow Date {}", day, localRedBin, localYellowBin);

                errDetail = ThingStatusDetail.NONE;

                redbin = localRedBin;
                yellowbin = localYellowBin;

                return true;
            } else {
                logger.debug("Data fetch failed, got HTTP Code {}", response.getStatus());
                return false;
            }
        } catch (UnsupportedEncodingException ue) {
            errDetail = ThingStatusDetail.CONFIGURATION_ERROR;
            errMsg = "Error with encoding url";
            return false;
        } catch (TimeoutException to) {
            return false;
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
    }

    public ThingStatusDetail getErrorDetail() {
        return errDetail;
    }

    public @Nullable String getErrorMessage() {
        return errMsg;
    }

    public @Nullable String getDay() {
        return day;
    }

    public @Nullable ZonedDateTime getYellowBin() {
        return yellowbin;
    }

    public @Nullable ZonedDateTime getRedBin() {
        return redbin;
    }
}
