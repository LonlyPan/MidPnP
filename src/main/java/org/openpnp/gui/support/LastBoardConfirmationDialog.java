package org.openpnp.gui.support;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class LastBoardConfirmationDialog {
    private LastBoardConfirmationDialog() { }

    public static boolean fromButton(int button) throws FeedPreparationDialog.Cancelled {
        if (button == 0) {
            return true;
        }
        if (button == 1) {
            return false;
        }
        throw new FeedPreparationDialog.Cancelled();
    }

    public static boolean show(Component parent) throws Exception {
        final int[] answer = {JOptionPane.CLOSED_OPTION};
        Runnable dialog = () -> answer[0] = JOptionPane.showOptionDialog(parent,
                "是否是最后一块电路板贴装？\n是：各飞达最后一颗物料吸取后不再提前送料。"
                        + "\n否：各飞达最后一颗物料吸取后仍提前送料，准备下一块板。"
                        + "\n任务开始直接吸取，请确保第一颗物料已准备好。",
                "确认最后一块电路板", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new String[] {"是", "否", "取消贴装"}, "取消贴装");
        if (SwingUtilities.isEventDispatchThread()) {
            dialog.run();
        }
        else {
            SwingUtilities.invokeAndWait(dialog);
        }
        return fromButton(answer[0]);
    }
}
