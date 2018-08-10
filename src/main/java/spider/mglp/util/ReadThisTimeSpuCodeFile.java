package spider.mglp.util;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>pakage: spider.mglp.util</p>
 *
 * descirption: 获取已经下载/获得的数据的spu，该类下的两个方法往往需要结合使用
 *
 * @author wanghai
 * @version V1.0
 * @since <pre>2018/7/20 下午3:06</pre>
 */
public class ReadThisTimeSpuCodeFile {
    /**
     * 读取指定文件中的spu，该文件往往是获取清理后，或者将一系列csv文件处理为一个json文件时，将这些文件的spu写入了一个单独的文件
     *
     * @param path 该文件的路径
     * @return 该文件spu集合
     */
    public static Set<String> readSpuFile(String path) {
        Set<String> spuSet = null;
        File file = new File(path);
        try {
            InputStreamReader read = new InputStreamReader(new FileInputStream(
                    file), "utf-8");
            if (file.isFile() && file.exists()) {
                spuSet = new HashSet<>();
                BufferedReader br = new BufferedReader(read);
                String txt;
                // 读取文件，将文件内容放入到set中
                while ((txt = br.readLine()) != null) {
                    spuSet.add(txt.split(",")[0]);
                }
                br.close();
            }
            read.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("spuSet size:  " + spuSet.size());
        return spuSet;
    }

    /**
     * 读取指定文件夹中所有文件的spu，这些文件已spu开头命名，比如爬取的尺码csv、试穿csv
     *
     * @param path 该文件夹的路径
     * @return 该文件夹中所有文件的spu集合
     */
    public static HashSet<String> countSpuFileLocal(String path) {
        HashSet<String> spuSetLocal = new HashSet<>();

        // 读取文件spucode过滤

        File files = new File(path);
        File[] fs = files.listFiles();    //遍历path下的文件和目录，放在File数组中
        assert fs != null;
        for (File f : fs) {
            // 若非目录(即文件)  去掉.开头的隐藏文件
            if (!f.isDirectory() && !f.isHidden()) {
                String startName = f.getName();
                String spucode = startName.substring(0, 12);
                spuSetLocal.add(spucode);

            }
        }
        System.out.println("spuSet size:  " + spuSetLocal.size());
        return spuSetLocal;
    }

    public static void main(String[] args) throws IOException {
        HashSet<String> setlocal = ReadThisTimeSpuCodeFile.countSpuFileLocal("/Users/Shared/size_chart");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File("/Users/Shared/size_chart/spu/spidered.txt")));
        for (String s : setlocal) {
            bufferedWriter.write(s);
            bufferedWriter.write("\n");
        }
        bufferedWriter.close();
    }
}
