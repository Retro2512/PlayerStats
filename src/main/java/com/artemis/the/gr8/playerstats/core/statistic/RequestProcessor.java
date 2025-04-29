package com.artemis.the.gr8.playerstats.core.statistic;

import com.artemis.the.gr8.playerstats.api.StatRequest;

public abstract class RequestProcessor {

    public abstract void processPlayerRequest(StatRequest<?> playerStatRequest);

    public abstract void processServerRequest(StatRequest<?> serverStatRequest);

    public abstract void processTopRequest(StatRequest<?> topStatRequest);
}
