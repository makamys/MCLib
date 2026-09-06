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
    /** Tasks which are executed once for every mod while that mod is being handled in the given loader state. */
    static Map<LoaderState, Map<String, Triple<ComparableVersion, Object, Runnable>>> modTasks = SharedReference.get("TaskQueue", "modTasks", HashMap.class);

    private static void enqueueTask(Map<LoaderState, Map<String, Triple<ComparableVersion, Object, Runnable>>> taskMap,
            LoaderState state, String taskName, Runnable task, ComparableVersion version) {
        Map<String, Triple<ComparableVersion, Object, Runnable>> stateTasks = taskMap.get(state);
        if(stateTasks == null) {
            taskMap.put(state, stateTasks = new HashMap<>());
        }
        Triple<ComparableVersion, Object, Runnable> versionAndTask = stateTasks.get(taskName);
        if(versionAndTask == null || versionAndTask.getLeft().compareTo(version) < 0) {
            stateTasks.put(taskName, Triple.of(version, MCLib.instance, task));
        }
    }
    
    /** Enqueues a task to get executed the next time {@link state} is reached. If a task with the same name is already registered, we will only overwrite it if our version is greater. */
    public static void enqueueTask(LoaderState state, String taskName, Runnable task, ComparableVersion version) {
        enqueueTask(queuedTasks, state, taskName, task, version);
    }

    public static void enqueueTask(LoaderState state, String taskName, Runnable runnable) {
        enqueueTask(state, taskName, runnable, new ComparableVersion(MCLib.VERSION));
    }

    /**
     * Enqueues a task which is executed for every mod while FML is handling that mod in the given loader state.
     * If a task with the same name is registered by multiple shaded MCLib copies, only the newest one is kept.
     */
    public static void enqueueModTask(LoaderState state, String taskName, Runnable task, ComparableVersion version) {
        enqueueTask(modTasks, state, taskName, task, version);
    }

    public static void enqueueModTask(LoaderState state, String taskName, Runnable runnable) {
        enqueueModTask(state, taskName, runnable, new ComparableVersion(MCLib.VERSION));
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

    static void runModTasks(LoaderState state) {
        Map<String, Triple<ComparableVersion, Object, Runnable>> tasks = TaskQueue.modTasks.get(state);
        if(tasks != null) {
            for(Triple<ComparableVersion, Object, Runnable> task : tasks.values()) {
                task.getRight().run();
            }
        }
    }
}
