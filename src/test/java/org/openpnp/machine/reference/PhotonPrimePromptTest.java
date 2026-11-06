package org.openpnp.machine.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.PhotonFeeder;
import org.openpnp.model.*;
import org.openpnp.spi.PnpJobProcessor.JobPlacement;
import org.openpnp.spi.PnpJobProcessor.JobPlacement.Status;
import org.openpnp.gui.support.LastBoardConfirmationDialog;
import org.openpnp.gui.support.FeedPreparationDialog;

class PhotonPrimePromptTest {
    static class TestFeeder extends PhotonFeeder {
        final List<Boolean> choices = new ArrayList<>();
        @Override public boolean isEnabled() { return true; }
        @Override public Location getPickLocation() { return new Location(LengthUnit.Millimeters); }
        @Override public void preparePlacementRun(boolean prime) { choices.add(prime); }
    }

    static class Processor extends ReferencePnpJobProcessor {
        int prompts;
        boolean choice;
        boolean cancel;
        @Override protected boolean requestPhotonLastBoard() throws Exception {
            prompts++;
            if (cancel) {
                throw new org.openpnp.gui.support.FeedPreparationDialog.Cancelled();
            }
            return choice;
        }
        void prepare(Job job, Part part) throws Exception {
            initialize(job);
            machine = Configuration.get().getMachine();
            jobPlacements.clear();
            Placement placement = new Placement("R1");
            placement.setPart(part);
            jobPlacements.add(new JobPlacement(new BoardLocation(new Board()), placement));
            preparePhotonJobFeed();
        }
        JobPlacement addPlacement(Part part) {
            Placement placement = new Placement("extra" + jobPlacements.size());
            placement.setPart(part);
            JobPlacement jp = new JobPlacement(new BoardLocation(new Board()), placement);
            jobPlacements.add(jp);
            return jp;
        }
        JobPlacement first() { return jobPlacements.get(0); }
        void retryPreparation() throws Exception { preparePhotonJobFeed(); }
        void armConfirmationStep() throws Exception {
            java.lang.reflect.Field step = ReferencePnpJobProcessor.class.getDeclaredField("currentStep");
            step.setAccessible(true);
            step.set(this, (ReferencePnpJobProcessor.Step) () -> {
                preparePhotonJobFeed();
                return null;
            });
        }
    }

    @Test void cancelStopsWithoutPrimingOrCleanupMotion() throws Exception {
        Configuration.initialize(Files.createTempDirectory("photon-prime-cancel").toFile());
        Configuration.get().load();
        Part part = new Part("cancel-test");
        TestFeeder feeder = new TestFeeder();
        feeder.setPart(part);
        feeder.setFeedAfterPick(true);
        Configuration.get().getMachine().addFeeder(feeder);
        Processor processor = new Processor();
        processor.cancel = true;
        assertThrows(org.openpnp.gui.support.FeedPreparationDialog.Cancelled.class,
                () -> processor.prepare(new Job(), part));
        assertTrue(feeder.choices.isEmpty());
        processor.armConfirmationStep();
        assertFalse(processor.next());
        assertTrue(feeder.choices.isEmpty());
    }

    @Test void everyRunAsksLastBoardButNeverPrimesAtStart() throws Exception {
        Configuration.initialize(Files.createTempDirectory("photon-prime-prompt").toFile());
        Configuration.get().load();
        Part part = new Part("prime-test");
        TestFeeder feeder = new TestFeeder();
        feeder.setPart(part);
        feeder.setFeedAfterPick(true);
        Configuration.get().getMachine().addFeeder(feeder);
        Processor processor = new Processor();
        Job sameJob = new Job();
        processor.choice = true;
        processor.prepare(sameJob, part);
        processor.retryPreparation();
        assertEquals(1, processor.prompts);
        assertEquals(java.util.Arrays.asList(false), feeder.choices);
        processor.choice = false;
        processor.prepare(sameJob, part);
        assertEquals(2, processor.prompts);
        assertEquals(java.util.Arrays.asList(false, false), feeder.choices);
        processor.prepare(new Job(), part);
        assertEquals(3, processor.prompts, "A new job also asks only the last-board question");
        assertEquals(java.util.Arrays.asList(false, false, false), feeder.choices);
        feeder.setFeedAfterPick(false);
        processor.prepare(sameJob, part);
        assertEquals(3, processor.prompts, "Ordinary feed mode does not ask for advance preparation");
    }
    @Test void lastBoardStopsEachPartsFinalAdvanceButOtherBoardsAlwaysAdvance() throws Exception {
        Configuration.initialize(Files.createTempDirectory("photon-last-board").toFile());
        Configuration.get().load();
        Part part = new Part("last-board-test");
        TestFeeder feeder = new TestFeeder();
        feeder.setPart(part);
        feeder.setFeedAfterPick(true);
        Configuration.get().getMachine().addFeeder(feeder);
        Processor processor = new Processor();
        processor.choice = true;
        Job job = new Job();
        processor.prepare(job, part);
        JobPlacement first = processor.first();
        JobPlacement second = processor.addPlacement(part);
        JobPlacement differentPart = processor.addPlacement(new Part("other-part"));
        TestFeeder otherFeeder = new TestFeeder();
        otherFeeder.setPart(differentPart.getPlacement().getPart());
        otherFeeder.setFeedAfterPick(true);
        Configuration.get().getMachine().addFeeder(otherFeeder);
        assertTrue(processor.shouldAdvancePhotonAfterPick(first));
        first.setStatus(Status.Complete);
        assertFalse(processor.shouldAdvancePhotonAfterPick(second));
        assertFalse(processor.shouldAdvancePhotonAfterPick(differentPart));
        processor.retryPreparation();
        assertEquals(1, processor.prompts, "Retry/step must reuse this run's choice");
        assertEquals(java.util.Arrays.asList(false), otherFeeder.choices);
        processor.choice = false;
        processor.prepare(job, part);
        assertTrue(processor.shouldAdvancePhotonAfterPick(processor.first()),
                "Even the final pick advances when another board follows");
        assertEquals(java.util.Arrays.asList(false, false), feeder.choices);
    }

    @Test void dialogYesNoAndCancelHaveDistinctMeanings() throws Exception {
        assertTrue(LastBoardConfirmationDialog.fromButton(0));
        assertFalse(LastBoardConfirmationDialog.fromButton(1));
        assertThrows(FeedPreparationDialog.Cancelled.class, () -> LastBoardConfirmationDialog.fromButton(2));
        assertThrows(FeedPreparationDialog.Cancelled.class, () -> LastBoardConfirmationDialog.fromButton(-1));
    }

}
