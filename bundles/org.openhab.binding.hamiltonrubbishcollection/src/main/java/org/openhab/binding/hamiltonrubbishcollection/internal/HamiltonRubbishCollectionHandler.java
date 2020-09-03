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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.hamiltonrubbishcollection.internal.HamiltonRubbishCollectionBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HamiltonRubbishCollectionHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Stewart Cossey - Initial contribution
 */
@NonNullByDefault
public class HamiltonRubbishCollectionHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(HamiltonRubbishCollectionHandler.class);

    private final HttpClient httpClient;
    private @Nullable API api;

    private static final int DELAY_NETWORKERROR = 3;
    private static final int DELAY_UPDATE = 480;

    private @Nullable HamiltonRubbishCollectionConfiguration config;

    private @Nullable ScheduledFuture<?> refreshScheduler;

    public HamiltonRubbishCollectionHandler(Thing thing, HttpClient httpClient) {
        super(thing);

        this.httpClient = httpClient;
    }

    private void startUpdate(int delay) {
        logger.debug("Start refresh scheduler");
        refreshScheduler = scheduler.scheduleWithFixedDelay(() -> {
            update();
        }, delay, DELAY_UPDATE, TimeUnit.MINUTES);
    }

    private void stopUpdate(boolean networkFail) {
        if (refreshScheduler != null) {
            refreshScheduler.cancel(true);
            refreshScheduler = null;
        }
        if (networkFail) {
            logger.debug("Network failure wait {} minutes and try again", DELAY_NETWORKERROR);
            startUpdate(DELAY_NETWORKERROR);
        }
    }

    private void update() {
        logger.debug("Refreshing data");
        if (api.update()) {
            updateStatus(ThingStatus.ONLINE);

            updateState(CHANNEL_DAY, new StringType(api.getDay()));
            updateState(CHANNEL_BIN_RED, new DateTimeType(api.getRedBin()));
            updateState(CHANNEL_BIN_YELLOW, new DateTimeType(api.getYellowBin()));
        } else {
            if (api.getErrorDetail() != ThingStatusDetail.NONE) {
                updateStatus(ThingStatus.OFFLINE, api.getErrorDetail(), api.getErrorMessage());
                stopUpdate(false);
            } else {
                stopUpdate(true);
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing");
        config = getConfigAs(HamiltonRubbishCollectionConfiguration.class);

        api = new API(httpClient, config.address);

        updateStatus(ThingStatus.UNKNOWN);

        startUpdate(0);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Not used
    }
}
