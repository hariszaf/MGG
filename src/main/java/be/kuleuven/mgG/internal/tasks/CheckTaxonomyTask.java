package be.kuleuven.mgG.internal.tasks;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;
import be.kuleuven.mgG.internal.view.JSONDisplayPanel;

/**
 * Shows the taxonomies imported with Import Taxonomy (node, taxonomy), as Check Metadata shows the metadata.
 */
public class CheckTaxonomyTask extends AbstractTask {

    private final MGGManager mggManager;

    public CheckTaxonomyTask(MGGManager mggManager) {
        this.mggManager = mggManager;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        JSONObject taxonomyObject = mggManager.getTaxonomyJsonObject();

        if (taxonomyObject != null && taxonomyObject.containsKey("taxonomy")) {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Imported Taxonomy");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(new JSONDisplayPanel(mggManager, taxonomyObject));
                frame.pack();
                frame.setVisible(true);
            });
        } else {
            taskMonitor.setStatusMessage("No taxonomy imported");
        }
    }
}
