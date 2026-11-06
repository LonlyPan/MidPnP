package org.openpnp.gui.support;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.openpnp.spi.JobProcessor.JobProcessorException;

public final class FeedPreparationDialog {
    public enum Choice { FEED, USE_CURRENT, CANCEL }

    public static class Cancelled extends JobProcessorException {
        public Cancelled() { super(null, "已取消贴装。"); }
    }

    private FeedPreparationDialog() { }

    public static Choice fromButton(int button) {
        if (button == 0) {
            return Choice.FEED;
        }
        if (button == 1) {
            return Choice.USE_CURRENT;
        }
        return Choice.CANCEL;
    }

    public static Choice show(Component parent, String title, String scope, String cancelLabel) throws Exception {
        final int[] answer = {JOptionPane.CLOSED_OPTION};
        Runnable dialog = () -> answer[0] = JOptionPane.showOptionDialog(parent,
                "是否预先进料一次，再吸取？\n是：" + scope + "先送料一次。\n否：直接吸取当前已准备的物料。",
                title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new String[] {"是", "否", cancelLabel}, cancelLabel);
        if (SwingUtilities.isEventDispatchThread()) {
            dialog.run();
        }
        else {
            SwingUtilities.invokeAndWait(dialog);
        }
        return fromButton(answer[0]);
    }
}
