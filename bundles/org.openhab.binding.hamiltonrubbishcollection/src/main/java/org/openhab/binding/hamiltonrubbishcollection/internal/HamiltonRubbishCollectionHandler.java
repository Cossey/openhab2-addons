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

import java.time.ZonedDateTime;
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

    private @Nullable ScheduledFuture<?> refreshScheduler;

    public HamiltonRubbishCollectionHandler(Thing thing, HttpClient httpClient) {
        super(thing);

        this.httpClient = httpClient;
    }

    private void startUpdate(int delay) {
        logger.debug("Start refresh scheduler");
        refreshScheduler = scheduler.scheduleWithFixedDelay(this::update, delay, DELAY_UPDATE, TimeUnit.MINUTES);
    }

    private void stopUpdate(boolean networkFail) {
        final ScheduledFuture<?> localRefreshScheduler = refreshScheduler;
        logger.trace("Stopping Update Scheduler");
        if (localRefreshScheduler != null) {
            localRefreshScheduler.cancel(true);
            refreshScheduler = null;
        }
        if (networkFail) {
            logger.debug("Stopped due to Network failure. Wait {} minutes and try again", DELAY_NETWORKERROR);
            startUpdate(DELAY_NETWORKERROR);
        }
    }

    private void update() {
        logger.debug("Refreshing data");
        final API localApi = api;
        if (localApi != null) {
            if (localApi.update()) {
                updateStatus(ThingStatus.ONLINE);

                updateState(CHANNEL_DAY, new StringType(localApi.getDay()));

                if (localApi.getRedBin() != null) {
                    ZonedDateTime redBin = ZonedDateTime.from(localApi.getRedBin());
                    updateState(CHANNEL_BIN_RED, new DateTimeType(redBin));
                }

                if (localApi.getYellowBin() != null) {
                    ZonedDateTime yellowBin = ZonedDateTime.from(localApi.getYellowBin());
                    updateState(CHANNEL_BIN_YELLOW, new DateTimeType(yellowBin));
                }
            } else {
                if (localApi.getErrorDetail() != ThingStatusDetail.NONE) {
                    updateStatus(ThingStatus.OFFLINE, localApi.getErrorDetail(), localApi.getErrorMessage());
                    stopUpdate(false);
                } else {
                    stopUpdate(true);
                }
            }
        } else {
            logger.error("Local API is null, cannot update.");
        }
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing");
        final HamiltonRubbishCollectionConfiguration config = getConfigAs(HamiltonRubbishCollectionConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        api = new API(httpClient, config.address);

        startUpdate(0);
    }

    @Override
    public void dispose() {
        stopUpdate(false);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Not used
    }
}
