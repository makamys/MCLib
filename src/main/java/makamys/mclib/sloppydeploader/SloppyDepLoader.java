package makamys.mclib.sloppydeploader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.common.versioning.ComparableVersion;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.launchwrapper.LaunchClassLoader;
import sun.misc.URLClassPath;
import sun.net.util.URLUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * For autodownloading stuff.
 * This is really unoriginal, mostly ripped off CodeChickenCore, where it was mostly ripped off FML, credits to Chicken-Bones and cpw.
 */
public class SloppyDepLoader {
    private static ByteBuffer downloadBuffer = ByteBuffer.allocateDirect(1 << 23);
    private static final String owner = "Sloppy DepLoader";
    private static DepLoadInst inst;

    public interface IDownloadDisplay {
        void resetProgress(int sizeGuess);

        void setPokeThread(Thread currentThread);

        void updateProgress(int fullLength);

        boolean shouldStopIt();

        void updateProgressString(String string, Object... data);

        Object makeDialog();

        void showErrorDialog(String name, String url);
    }

    public static class DummyDownloader implements IDownloadDisplay {
        @Override
        public void resetProgress(int sizeGuess) {
        }

        @Override
        public void setPokeThread(Thread currentThread) {
        }

        @Override
        public void updateProgress(int fullLength) {
        }

        @Override
        public boolean shouldStopIt() {
            return false;
        }

        @Override
        public void updateProgressString(String string, Object... data) {
        }

        @Override
        public Object makeDialog() {
            return null;
        }

        @Override
        public void showErrorDialog(String name, String url) {
        }
    }

    public static class VersionedFile
    {
        public final Pattern pattern;
        public final String filename;
        public final ComparableVersion version;
        public final String name;

        public VersionedFile(String filename, Pattern pattern) {
            this.pattern = pattern;
            this.filename = filename;
            Matcher m = pattern.matcher(filename);
            if(m.matches()) {
                name = m.group(1);
                version = new ComparableVersion(m.group(2));
            }
            else {
                name = null;
                version = null;
            }
        }

        public boolean matches() {
            return name != null;
        }
    }

    public static class Dependency
    {
        public String url;
        public VersionedFile file;

        public String existing;
        /**
         * Flag set to add this dep to the classpath immediately because it is required for a coremod.
         */
        public boolean coreLib;

        public Dependency(String url, VersionedFile file, boolean coreLib) {
            this.url = url;
            this.file = file;
            this.coreLib = coreLib;
        }
    }

    public static class DepLoadInst {
        private File modsDir;
        private File v_modsDir;
        private IDownloadDisplay downloadMonitor;

        private Map<String, Dependency> depMap = new HashMap<String, Dependency>();
        private HashSet<String> depSet = new HashSet<String>();

        public DepLoadInst() {
            String mcVer = (String) FMLInjectionData.data()[4];
            File mcDir = (File) FMLInjectionData.data()[6];

            modsDir = new File(mcDir, "mods");
            v_modsDir = new File(mcDir, "mods/" + mcVer);
            if (!v_modsDir.exists())
                v_modsDir.mkdirs();
            
            FMLCommonHandler.instance().bus().register(this);
        }
        
        // this happens after pre-init, so mods should have registered their dependencies by now
        @SubscribeEvent
        public void onRenderTick(RenderTickEvent event) {
            load();
            FMLCommonHandler.instance().bus().unregister(this);
        }

        private void addClasspath(String name) {
            try {
                ((LaunchClassLoader) SloppyDepLoader.class.getClassLoader()).addURL(new File(v_modsDir, name).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        private void deleteMod(File mod) {
            if (mod.delete())
                return;

            try {
                ClassLoader cl = SloppyDepLoader.class.getClassLoader();
                URL url = mod.toURI().toURL();
                Field f_ucp = URLClassLoader.class.getDeclaredField("ucp");
                Field f_loaders = URLClassPath.class.getDeclaredField("loaders");
                Field f_lmap = URLClassPath.class.getDeclaredField("lmap");
                f_ucp.setAccessible(true);
                f_loaders.setAccessible(true);
                f_lmap.setAccessible(true);

                URLClassPath ucp = (URLClassPath) f_ucp.get(cl);
                Closeable loader = ((Map<String, Closeable>) f_lmap.get(ucp)).remove(URLUtil.urlNoFragString(url));
                if (loader != null) {
                    loader.close();
                    ((List<?>) f_loaders.get(ucp)).remove(loader);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!mod.delete()) {
                mod.deleteOnExit();
                String msg = owner + " was unable to delete file " + mod.getPath() + " the game will try to delete it on exit. If this message appears again, delete it manually.";
                System.err.println(msg);
            }
        }

        private void download(Dependency dep) {
            File libFile = new File(v_modsDir, dep.file.filename);
            try {
                URL libDownload = new URL(dep.url + '/' + dep.file.filename);
                downloadMonitor.updateProgressString("Downloading file %s", libDownload.toString());
                System.out.println(String.format("Downloading file %s\n", libDownload.toString()));
                URLConnection connection = libDownload.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "" + owner + " Downloader");
                int sizeGuess = connection.getContentLength();
                download(connection.getInputStream(), sizeGuess, libFile);
                downloadMonitor.updateProgressString("Download complete");
                System.out.println("Download complete");
            } catch (Exception e) {
                libFile.delete();
                System.err.println("A download error occured downloading " + dep.file.filename + " from " + dep.url + '/' + dep.file.filename + ": " + e.getMessage());
                //downloadMonitor.showErrorDialog(dep.file.filename, dep.url + '/' + dep.file.filename);
                //throw new RuntimeException("A download error occured", e);
            }
        }

        private void download(InputStream is, int sizeGuess, File target) throws Exception {
            if (sizeGuess > downloadBuffer.capacity())
                throw new Exception(String.format("The file %s is too large to be downloaded by " + owner + " - the download is invalid", target.getName()));

            downloadBuffer.clear();

            int bytesRead, fullLength = 0;

            downloadMonitor.resetProgress(sizeGuess);
            try {
                downloadMonitor.setPokeThread(Thread.currentThread());
                byte[] smallBuffer = new byte[1024];
                while ((bytesRead = is.read(smallBuffer)) >= 0) {
                    downloadBuffer.put(smallBuffer, 0, bytesRead);
                    fullLength += bytesRead;
                    if (downloadMonitor.shouldStopIt()) {
                        break;
                    }
                    downloadMonitor.updateProgress(fullLength);
                }
                is.close();
                downloadMonitor.setPokeThread(null);
                downloadBuffer.limit(fullLength);
                downloadBuffer.position(0);
            } catch (InterruptedIOException e) {
                // We were interrupted by the stop button. We're stopping now.. clear interruption flag.
                Thread.interrupted();
                throw new Exception("Stop");
            } catch (IOException e) {
                throw e;
            }

            try {
                /*String cksum = generateChecksum(downloadBuffer);
                if (cksum.equals(validationHash))
                {*/
                if (!target.exists())
                    target.createNewFile();


                downloadBuffer.position(0);
                FileOutputStream fos = new FileOutputStream(target);
                fos.getChannel().write(downloadBuffer);
                fos.close();
                /*}
                else
                {
                    throw new RuntimeException(String.format("The downloaded file %s has an invalid checksum %s (expecting %s). The download did not succeed correctly and the file has been deleted. Please try launching again.", target.getName(), cksum, validationHash));
                }*/
            } catch (Exception e) {
                throw e;
            }
        }

        private String checkExisting(Dependency dep) {
            for (File f : modsDir.listFiles()) {
                VersionedFile vfile = new VersionedFile(f.getName(), dep.file.pattern);
                if (!vfile.matches() || !vfile.name.equals(dep.file.name))
                    continue;

                if (f.renameTo(new File(v_modsDir, f.getName())))
                    continue;

                deleteMod(f);
            }

            for (File f : v_modsDir.listFiles()) {
                VersionedFile vfile = new VersionedFile(f.getName(), dep.file.pattern);
                if (!vfile.matches() || !vfile.name.equals(dep.file.name))
                    continue;

                int cmp = vfile.version.compareTo(dep.file.version);
                if (cmp < 0) {
                    System.out.println("Deleted old version " + f.getName());
                    deleteMod(f);
                    return null;
                }
                if (cmp > 0) {
                    System.err.println("Warning: version of " + dep.file.name + ", " + vfile.version + " is newer than request " + dep.file.version);
                    return f.getName();
                }
                return f.getName();//found dependency
            }
            return null;
        }

        public void load() {
            if (depMap.isEmpty())
                return;

            loadDeps();
            activateDeps();
        }

        private void activateDeps() {
            for (Dependency dep : depMap.values())
                if (dep.coreLib)
                    addClasspath(dep.existing);
        }

        private void loadDeps() {
            downloadMonitor = new DummyDownloader();
            while (!depSet.isEmpty()) {
                Iterator<String> it = depSet.iterator();
                Dependency dep = depMap.get(it.next());
                it.remove();
                load(dep);
            }
        }

        private void load(Dependency dep) {
            dep.existing = checkExisting(dep);
            if (dep.existing == null)//download dep
            {
                download(dep);
                dep.existing = dep.file.filename;
            }
        }

        private List<File> modFiles() {
            List<File> list = new LinkedList<File>();
            list.addAll(Arrays.asList(modsDir.listFiles()));
            list.addAll(Arrays.asList(v_modsDir.listFiles()));
            return list;
        }

        public void addSloppyDep(SloppyDependency dep) throws IOException {
            boolean obfuscated = ((LaunchClassLoader) SloppyDepLoader.class.getClassLoader())
                    .getClassBytes("net.minecraft.world.World") == null;

            String testClass = dep.testClass;
            if (SloppyDepLoader.class.getResource("/" + testClass.replace('.', '/') + ".class") != null)
                return;

            String repo = dep.repo;
            String filename = dep.filename;
            if (!obfuscated && dep.dev.isPresent())
                filename = dep.dev.get();

            boolean coreLib = false;

            Pattern pattern = null;
            try {
                if(dep.pattern.isPresent())
                    pattern = Pattern.compile(dep.pattern.get());
            } catch (PatternSyntaxException e) {
                System.err.println("Invalid filename pattern: "+ dep.pattern.get());
                e.printStackTrace();
            }
            if(pattern == null)
                pattern = Pattern.compile("(\\w+).*?([\\d\\.]+)[-\\w]*\\.[^\\d]+");

            VersionedFile file = new VersionedFile(filename, pattern);
            if (!file.matches())
                throw new RuntimeException("Invalid filename format for dependency: " + filename);

            addDep(new Dependency(repo, file, coreLib));
        }

        private void addDep(Dependency newDep) {
            if (mergeNew(depMap.get(newDep.file.name), newDep)) {
                depMap.put(newDep.file.name, newDep);
                depSet.add(newDep.file.name);
            }
        }

        private boolean mergeNew(Dependency oldDep, Dependency newDep) {
            if (oldDep == null)
                return true;

            Dependency newest = newDep.file.version.compareTo(oldDep.file.version) > 0 ? newDep : oldDep;
            newest.coreLib = newDep.coreLib || oldDep.coreLib;

            return newest == newDep;
        }
    }

    public static void addDependency(SloppyDependency dep) {
        if (inst == null) {
            inst = new DepLoadInst();
        }
        try {
            inst.addSloppyDep(dep);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}