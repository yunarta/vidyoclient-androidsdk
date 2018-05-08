package com.vidyo.vidyoconnector.expansions;

import com.vidyo.VidyoClient.Connector.Connector;

import org.jetbrains.annotations.NotNull;

public interface ConnectorFactory {

    Connector create(@NotNull Object it);
}
