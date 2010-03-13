package hu.pagavcs.mug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mucommander.file.AbstractFile;

public class FindManager {

	private static final FindManager            singleton = new FindManager();

	private HashMap<String, List<AbstractFile>> mapFind;

	private ScheduledExecutorService            oldSearchPurger;

	public FindManager() {
		mapFind = new HashMap<String, List<AbstractFile>>();
		oldSearchPurger = Executors.newSingleThreadScheduledExecutor();
	}

	public static FindManager getInstance() {
		return singleton;
	}

	public List<AbstractFile> getResults(String id) {
		return mapFind.get(id);
	}

	public void removeResults(String id) {
		mapFind.remove(id);
	}

	public void createResults(final String id) {
		mapFind.put(id, new ArrayList<AbstractFile>());
		oldSearchPurger.schedule(new Runnable() {

			public void run() {
				removeResults(id);
			}
		}, 1, TimeUnit.HOURS);
	}

	public void addResult(String id, AbstractFile file) {
		mapFind.get(id).add(file);
	}

}
