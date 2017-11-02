package source;

import com.google.common.base.Strings;
//import common.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class TailFileHelper
{

    private static final Logger logger = LoggerFactory.getLogger(TailFileHelper.class);

    public static String getFileName(String directory, String inode) {
        if (inode == null || !isExist(directory) || !isDirectory(directory)) {
            return null;
        }

        final File[] allFiles = new File(directory).listFiles();
        if (allFiles == null) {
            return null;
        }

        for (File file : allFiles) {
            if (inode.equals(getInode(file.getAbsolutePath()))) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * 获取目录下的所有文件的inode，不包含目录的
     *
     * @param directory
     * @param regular 文件名必须保护的关键字
     * @return inodeList
     */
    public static List<String> getAllInode(String directory, final String regular) {

        if (!isExist(directory) || !isDirectory(directory)) {
            return null;
        }

        final File dir = new File(directory);

        final File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (isDirectory(dir.getAbsolutePath() + "/" + name)) {
                    return false;
                }
                return isMatcherFileName(name, regular);
            }
        });

        if (files == null || files.length == 0) {
            return null;
        }

        List<String> resultList = new ArrayList<String>();
        for (File file : files) {
            final String inode = getInode(file.getAbsolutePath());
            resultList.add(inode);
        }

        return resultList;
    }

    public static boolean isMatcherFileName(String fileName, String regular) {
        if(Strings.isNullOrEmpty(regular)){
            return true;
        }
        Matcher res = Pattern.compile(regular, Pattern.CASE_INSENSITIVE).matcher(fileName);
        return res.find();
    }

    public static boolean containsFileName(String fileName, List<String> filterList) {
        if (filterList == null || filterList.size() == 0) {
            return true;
        }
        for (String filter : filterList) {
            if (fileName.contains(filter)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExist(String filename) {
        return new File(filename).exists();
    }

    public static boolean isSymbolicLink(String path) {
        try {
            return Files.isSymbolicLink(Paths.get(path));
        } catch (Throwable e) {
            //ErrorLogCollector.errorLog(logger, "isSymbolicLink catch Exception " + path, e);
            return false;
        }
    }

    public static boolean isRealPathExist(String path) {
        try {
            final String realPath = getRealPath(path);
            if (!Strings.isNullOrEmpty(realPath) && isExist(realPath)) {
                return true;
            }
            return false;
        } catch (Throwable t) {
            //ErrorLogCollector.errorLog(logger, "isRealPathExist catch Exception " + path, t);
            return false;
        }
    }

    public static String getRealPath(String path) {
        if (!isSymbolicLink(path)) {
            return path;
        }
        try {
            final Path realPath = Paths.get(path).toRealPath();
            return realPath.toAbsolutePath().toString();
        } catch (Throwable t) {
            //ErrorLogCollector.errorLog(logger, "getRealPath catch Exception " + path, t);
            return null;
        }
    }

    public static boolean isDirectory(String filename) {
        return filename != null && new File(filename).isDirectory();
    }

    public static String getInode(String fileName) {
        String inode;
        try {
            Path path = Paths.get(fileName);
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);

            Object fileKey = attr.fileKey();

            // windows 下filekey为空
            if (fileKey != null) {
                String s = fileKey.toString();
                inode = s.substring(s.indexOf("ino=") + 4, s.indexOf(")"));
            } else {
                inode = fileName;
            }

        } catch (Exception e) {
            return null;
        }

        return inode;
    }
}
