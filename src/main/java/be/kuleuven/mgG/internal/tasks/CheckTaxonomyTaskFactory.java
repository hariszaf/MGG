package be.kuleuven.mgG.internal.tasks;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.json.simple.JSONObject;

import be.kuleuven.mgG.internal.model.MGGManager;

public class CheckTaxonomyTaskFactory implements TaskFactory {

    private final MGGManager mggManager;

    public CheckTaxonomyTaskFactory(MGGManager mggManager) {
        this.mggManager = mggManager;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return new TaskIterator(new CheckTaxonomyTask(mggManager));
    }

    // Enabled once a taxonomy file is imported
    @Override
    public boolean isReady() {
        JSONObject taxonomyObject = mggManager.getTaxonomyJsonObject();
        return taxonomyObject != null && taxonomyObject.containsKey("taxonomy");
    }
}
