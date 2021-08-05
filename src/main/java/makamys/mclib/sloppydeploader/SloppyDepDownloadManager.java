package makamys.mclib.sloppydeploader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import makamys.mclib.core.sharedstate.SharedReference;
import static makamys.mclib.sloppydeploader.SloppyDepLoader.NS;

public class SloppyDepDownloadManager {
    
    private static BlockingQueue<Runnable> workQueue = SharedReference.get(NS, "workQueue", LinkedBlockingQueue.class);
    static ThreadPoolExecutor executor = SharedReference.get(NS, "executor", () -> new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, workQueue));
    static List<CompletableFuture<String>> futures = SharedReference.get(NS, "futures", ArrayList.class);
    
    public void enqueueDownload(Supplier task) {
        futures.add(CompletableFuture.supplyAsync(task, executor));
    }
    
    public boolean allDone() {
        return futures.stream().allMatch(f -> f.isDone());
    }
    
    public List<String> getDownloadedList() {
        if(!allDone()) return Arrays.asList();
        
        return futures.stream().map(f -> {
            try {
                return f.get();
            } catch(Exception e) {
                return null;
            }})
        .filter(Objects::nonNull).collect(Collectors.toList());
    }
}
