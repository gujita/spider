package spider.mglp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spider.mglp.enums.UrlEnum;
import spider.mglp.util.SqlUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * descirption:
 *
 * @author wanghai
 * @version V1.0
 * @since <pre>2018/8/17 下午2:53</pre>
 */
public class GetAllLabel {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetAllLabel.class);

    /**
     * 获取数据库中所有Spu的评价标签，将json数据持久化，解析处理交给 ()函数
     *
     * @param localDate
     */
    public void getLabelOriginal(String localDate) throws IOException, InterruptedException {
        String parentPath = UrlEnum.ALL_LABEL_FILE_JSON.getDesc() + localDate;
        // 先建立日期目录
        File file = new File(parentPath);
        // 如果文件夹不存在则创建
        if (!file.exists() && !file.isDirectory()) {
            if (!file.mkdir()) {
                LOGGER.error("创建目录  {}  失败", localDate);
            }
        }

        // 查询数据库，获取所有的spu_code和taobao_link
        HashMap<String, String> spuIDMap = SqlUtils.getSpuCodeAndTbLink();
        System.out.println(spuIDMap.size());
        int count = 0;
        for (Map.Entry<String, String> entry : spuIDMap.entrySet()) {
            int begin = entry.getValue().lastIndexOf("id=") + 3;
            String itemId = entry.getValue().substring(begin, begin + 12);
            String spuCode = entry.getKey();
            String url = "https://rate.taobao.com/detailCommon.htm?auctionNumId=" + itemId;
            // 睡眠随机秒，再接着爬数据（0.8秒到1.8秒的区间）
            Random rand = new Random();
            Thread.sleep(rand.nextInt(1000) + 800);
            originalLabelToFile(url, spuCode, parentPath + "/");
            System.out.println(count++);
        }
    }

    public void originalLabelToFile(String url, String spu, String parentPath) throws IOException {
        String jsonText;
        StringBuilder sb = null;
        try (InputStream is = new URL(url).openStream(); BufferedReader brd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            sb = new StringBuilder();
            String cp;
            while ((cp = brd.readLine()) != null) {
                sb.append(cp);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }

        if (sb != null) {
            BufferedWriter buffWriter = new BufferedWriter(new FileWriter(new File(parentPath + spu + "_label.json")));
            jsonText = sb.toString();
            // 裁剪前后的括号
            jsonText = jsonText.substring(1, jsonText.length() - 1);
            buffWriter.write(jsonText);
            buffWriter.close();
        }
    }

    public void parse(String localDate) throws IOException {
        String fileName = UrlEnum.ADJUST_LABEL_FILE_JSON.getDesc() + localDate + "_label.txt";
        FileWriter fileWriter = new FileWriter(new File(fileName));
        String parentPath = UrlEnum.ALL_LABEL_FILE_JSON.getDesc() + localDate;
        File files = new File(parentPath);
        File[] fs = files.listFiles();    //遍历path下的文件和目录，放在File数组中
        int impress = 0;
        assert fs != null;
        ObjectMapper objMapper = new ObjectMapper();
        for (File f : fs) {
            if (!f.isDirectory() && !f.isHidden()) {
                String startName = f.getName();
                String spucode = startName.substring(0, 12);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                String jsonText = bufferedReader.readLine();
                if (jsonText.substring(97, 99).equals("[{")) {
                    impress++;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("{\"spu\":\"").append(spucode).append("\",");
                    Map jsonMap = objMapper.readValue(jsonText, Map.class);
                    Map<String, Object> dataMap = (Map<String, Object>) jsonMap.get("data");
                    List<Map<String, Object>> impressList = (List<Map<String, Object>>) dataMap.get("impress");
                    for (Map<String, Object> aFList : impressList) {
                        stringBuilder.append("\"").append(aFList.get("title")).append("\":").append(aFList.get("count")).append(",");
                    }
                    fileWriter.write(stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1) + "}");
                    fileWriter.write("\n");
                }
            }
        }
        fileWriter.flush();
        fileWriter.close();
        jsonToCsv(fileName);
        System.out.println("有效数： " + impress);
    }

    public void jsonToCsv(String jsonPath) throws IOException {
        BufferedReader brd = new BufferedReader(new InputStreamReader(new FileInputStream(jsonPath), Charset.forName("UTF-8")));
        String cp;
        FileWriter fileWriter = new FileWriter(new File(jsonPath.substring(0, jsonPath.length() - 3) + "csv"));
        while ((cp = brd.readLine()) != null) {
            // {"spu":"103418115495","衣服很舒服":27,"穿着效果好":14,"版型好看":9,"衣服不错":8,"布料好":5,"挺透气的":3,"颜色挺正":3}
            cp = cp.replaceAll("\"", "");
            cp = cp.substring(1, cp.length() - 1);
            String[] fileds = cp.split(",");
            String spu = fileds[0].substring(4, 16);
            for (int i = 1; i < fileds.length; i++) {
                fileWriter.write(spu + "," + fileds[i].split(":")[0] + "," + fileds[i].split(":")[1]);
                fileWriter.write("\n");
            }
        }
        fileWriter.flush();
        fileWriter.close();
        brd.close();
    }


    public static void main(String[] args) {
        GetAllLabel getAllLabel = new GetAllLabel();
        try {
            getAllLabel.parse("2018-08-17");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // {"watershed":100,"sellerRefundCount":0,"isRefundUser":true,"showPicRadio":true,"data":{"impress":[],"count":{"normal":0,"totalFull":10,"total":10,"bad":0,"tryReport":0,"goodFull":10,"additional":0,"pic":0,"good":10,"hascontent":0,"correspond":0},"attribute":[],"newSearch":false,"correspond":"0.0","spuRatting":[]},"skuFull":false,"isShowDefaultSort":true,"askAroundDisabled":true,"skuSelected":false}
    }
}