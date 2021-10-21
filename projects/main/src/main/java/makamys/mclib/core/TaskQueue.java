package makamys.mclib.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;

import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.versioning.ComparableVersion;
import makamys.mclib.core.sharedstate.SharedReference;

public class TaskQueue {
    
    /** loaderState -> (taskName -> (version, owner, task)) */
    static Map<LoaderState, Map<String, Triple<ComparableVersion, Object, Runnable>>> queuedTasks = SharedReference.get("TaskQueue", "queuedTasks", HashMap.class);
    
    /** Enqueues a task to get executed the next time {@link state} is reached. If a task with the same name is already registered, we will only overwrite it if our version is greater. */ 
    public static void enqueueTask(LoaderState state, String taskName, Runnable task, ComparableVersion version) {
        Map<String, Triple<ComparableVersion, Object, Runnable>> stateTasks = queuedTasks.get(state);
        if(stateTasks == null) {
            queuedTasks.put(state, stateTasks = new HashMap<>());
        }
        Triple<ComparableVersion, Object, Runnable> versionAndTask = stateTasks.get(taskName);
        if(versionAndTask == null || versionAndTask.getLeft().compareTo(version) < 0) {
            stateTasks.put(taskName, Triple.of(version, MCLib.instance, task));
        }
    }
    
    public static void enqueueTask(LoaderState state, String taskName, Runnable runnable) {
        enqueueTask(state, taskName, runnable, new ComparableVersion(MCLib.VERSION));
    }

    static void consume(LoaderState state, Object owner) {
        Map<String, Triple<ComparableVersion, Object, Runnable>> tasks = TaskQueue.queuedTasks.get(state);
        if(tasks != null) {
            tasks.entrySet().removeIf(e -> {
                if(e.getValue().getMiddle() == owner) {
                    e.getValue().getRight().run();
                    return true;
                }
               return false; 
            });
        }
    }
}
