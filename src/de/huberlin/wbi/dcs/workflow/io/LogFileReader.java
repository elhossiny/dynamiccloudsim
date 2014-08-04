package de.huberlin.wbi.dcs.workflow.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.File;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;

import de.huberlin.wbi.dcs.workflow.Task;
import de.huberlin.wbi.dcs.workflow.Workflow;

public abstract class LogFileReader {

	protected static int cloudletId = 0;

	protected Map<String, File> fileNameToFile;
	protected Map<String, Long> fileNameToProducingTaskId;
	protected Map<String, List<Long>> fileNameToConsumingTaskIds;
	protected Map<Long, Task> taskIdToTask;

	protected UtilizationModel utilizationModel;

	public LogFileReader() {
		fileNameToFile = new HashMap<String, File>();
		fileNameToProducingTaskId = new HashMap<String, Long>();
		fileNameToConsumingTaskIds = new HashMap<String, List<Long>>();
		taskIdToTask = new HashMap<Long, Task>();
		utilizationModel = new UtilizationModelFull();
	}

	public Workflow parseLogFile(int userId, String filePath,
			boolean fileNames, boolean kernelTime, String outputFileRegex) {
		if (outputFileRegex == null) {
			outputFileRegex = ".*";
		}
		Workflow workflow = new Workflow();
		fillDataStructures(userId, filePath, fileNames, kernelTime, workflow);
		populateNodes(workflow);
		populateEdges(userId, outputFileRegex, workflow);
		return workflow;
	}

	protected abstract void fillDataStructures(int userId, String filePath,
			boolean fileNames, boolean kernelTime, Workflow workflow);

	protected void populateNodes(Workflow workflow) {
		for (Task task : taskIdToTask.values()) {
			workflow.addTask(task);
		}
	}

	protected void populateEdges(int userId, String outputFileRegex,
			Workflow workflow) {
		for (String fileName : fileNameToFile.keySet()) {
			org.cloudbus.cloudsim.File file = fileNameToFile.get(fileName);

			List<Task> tasksRequiringThisFile = new ArrayList<Task>();
			for (long taskId : fileNameToConsumingTaskIds.get(fileName)) {
				tasksRequiringThisFile.add(taskIdToTask.get(taskId));
			}

			Task taskGeneratingThisFile = taskIdToTask
					.get(fileNameToProducingTaskId.get(fileName));

			if (taskGeneratingThisFile == null) {
				taskGeneratingThisFile = new Task("download", fileName,
						workflow, userId, cloudletId++, 0, file.getSize(),
						file.getSize(), 1, 0, file.getSize(), utilizationModel,
						utilizationModel, utilizationModel);
			}
			if (tasksRequiringThisFile.size() == 0
					&& fileName.matches(outputFileRegex)) {
				Task taskRequiringThisFile = new Task("upload", fileName,
						workflow, userId, cloudletId++, 0, file.getSize(),
						file.getSize(), 1, file.getSize(), 0, utilizationModel,
						utilizationModel, utilizationModel);
				tasksRequiringThisFile.add(taskRequiringThisFile);
			}
			workflow.addFile(file, taskGeneratingThisFile,
					tasksRequiringThisFile);
		}

		
	}

}
