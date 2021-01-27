/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.hccrubbishcollection.internal;

import static org.openhab.binding.hccrubbishcollection.internal.HCCRubbishCollectionBindingConstants.*;

import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HCCRubbishCollectionHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Stewart Cossey - Initial contribution
 */
@NonNullByDefault
public class HCCRubbishCollectionHandler extends BaseThingHandler {
    private static final int DELAY_NETWORKERROR = 3;
    private static final int DELAY_UPDATE = 480;

    private final Logger logger = LoggerFactory.getLogger(HCCRubbishCollectionHandler.class);

    private final HttpClient httpClient;
    private @Nullable API api;

    private @Nullable ScheduledFuture<?> refreshScheduler;

    public HCCRubbishCollectionHandler(Thing thing, HttpClient httpClient) {
        super(thing);

        this.httpClient = httpClient;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            updateNow();
        }
    }

    private void updateNow() {
        logger.debug("Updating data immediately");
        stopUpdate(false);
        startUpdate(0);
    }

    @Override
    public void initialize() {
        final HCCRubbishCollectionConfiguration config = getConfigAs(HCCRubbishCollectionConfiguration.class);

        updateStatus(ThingStatus.UNKNOWN);

        api = new API(httpClient, config.address);
        startUpdate(0);
    }

    private void updateData() {
        logger.debug("Fetching new data");
        final API localApi = api;
        if (localApi != null) {
            if (localApi.update()) {
                updateStatus(ThingStatus.ONLINE);

                Integer localDay = localApi.getDay();
                if (localDay != null) {
                    updateState(CHANNEL_DAY, new DecimalType(localDay));
                }

                ZonedDateTime localGeneralDate = localApi.getGeneralDate();
                if (localGeneralDate != null) {
                    updateState(CHANNEL_BIN_GENERAL, new DateTimeType(localGeneralDate));
                }

                ZonedDateTime localRecyclingDate = localApi.getRecyclingDate();
                if (localRecyclingDate != null) {
                    updateState(CHANNEL_BIN_RECYCLING, new DateTimeType(localRecyclingDate));
                }
            } else {
                if (localApi.getErrorDetail() != ThingStatusDetail.COMMUNICATION_ERROR) {
                    updateStatus(ThingStatus.OFFLINE, localApi.getErrorDetail(), localApi.getErrorDetailMessage());
                    stopUpdate(false);
                } else {
                    stopUpdate(true);
                }
            }
        } else {
            logger.error("API object is null, cannot update");
        }
    }

    private void startUpdate(int delay) {
        logger.debug("Start refresh scheduler, delay {}", delay);

        refreshScheduler = scheduler.scheduleWithFixedDelay(this::updateData, delay, DELAY_UPDATE, TimeUnit.MINUTES);
    }

    private void stopUpdate(boolean networkError) {
        final ScheduledFuture<?> localRefreshScheduler = refreshScheduler;
        logger.debug("Stopping updater scheduler, networkError = {}", networkError);
        if (localRefreshScheduler != null) {
            localRefreshScheduler.cancel(true);
            refreshScheduler = null;
        }
        if (networkError) {
            logger.debug("Waiting {} minutes to try again", DELAY_NETWORKERROR);
            startUpdate(DELAY_NETWORKERROR);
        }
    }

    @Override
    public void dispose() {
        stopUpdate(false);

        super.dispose();
    }
}
