package org.openpnp.machine.reference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.openpnp.gui.FeedersPanel;
import org.openpnp.gui.support.FeedPreparationDialog;
import org.openpnp.machine.photon.PhotonFeeder;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;

class ManualFeedConfirmationTest {
    @Test void manualYesForcesOnePreparationAndNoUsesCurrentPocket() throws Exception {
        PhotonFeeder feeder = mock(PhotonFeeder.class);
        Nozzle nozzle = mock(Nozzle.class);
        FeedersPanel.prepareManualPocket(feeder, nozzle, true);
        verify(feeder).preparePlacementRun(true);
        verify(feeder, never()).feed(any());
        reset(feeder);
        FeedersPanel.prepareManualPocket(feeder, nozzle, false);
        verify(feeder).preparePlacementRun(false);
        verify(feeder, never()).feed(any());
    }

    @Test void genericFeederNoSkipsFeedAndCancelDoesNothing() throws Exception {
        Feeder feeder = mock(Feeder.class);
        Nozzle nozzle = mock(Nozzle.class);
        FeedersPanel.prepareManualPocket(feeder, nozzle, false);
        verifyNoInteractions(feeder, nozzle);
        FeedersPanel.pickFeeder(feeder, FeedPreparationDialog.Choice.CANCEL);
        verifyNoInteractions(feeder, nozzle);
        FeedersPanel.prepareManualPocket(feeder, nozzle, true);
        verify(feeder).feed(nozzle);
    }

    @Test void closingDialogIsCancelRatherThanNo() {
        assertEquals(FeedPreparationDialog.Choice.FEED, FeedPreparationDialog.fromButton(0));
        assertEquals(FeedPreparationDialog.Choice.USE_CURRENT, FeedPreparationDialog.fromButton(1));
        assertEquals(FeedPreparationDialog.Choice.CANCEL, FeedPreparationDialog.fromButton(2));
        assertEquals(FeedPreparationDialog.Choice.CANCEL, FeedPreparationDialog.fromButton(-1));
    }
}
