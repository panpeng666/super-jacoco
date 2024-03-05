package com.xiaoju.basetech.util;

import com.xiaoju.basetech.entity.CoverageReportEntity;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.xiaoju.basetech.util.Constants.REPORT_PATH;

/**
 * @author: panpeng
 * @Title: MergeUnitReportHtml
 * @ProjectName: super-jacoco_2023
 * @Description:
 * @date: 2024/2/22 19:45
 */
@Slf4j
public class MergeUnitReportHtml {
    private static final Long CMD_TIMEOUT = 600000L;

    // 定义一个静态方法mergeHtml，它接受一个包含多个HTML文件路径的ArrayList和一个目标输出文件路径作为参数
    public static Integer[] mergeHtml(ArrayList<String> fileList, String destFile) {
        // 初始化一个大小为3的结果数组，用于存放处理结果状态码及两个覆盖率指标
        Integer[] result=new Integer[3];
        result[0]=0;
        result[1]=-1;
        result[2]=-1;
        // 创建一个JaCoCo覆盖率汇总报告的HTML模板字符串
        String htmlSchema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\">\n" +
                "<head>\n" +
                "    <meta http-equiv=\"Content-Type\" content=\"text/html;charset=UTF-8\" />\n" +
                "    <link rel=\"stylesheet\" href=\"jacoco-resources/report.css\" type=\"text/css\" />\n" +
                "    <link rel=\"shortcut icon\" href=\"jacoco-resources/report.gif\" type=\"image/gif\" />\n" +
                "    <title>manualDiffCoverageReport</title>\n" +
                "    <script type=\"text/javascript\" src=\"jacoco-resources/sort.js\"></script>\n" +
                "</head>\n" +
                "<body onload=\"initialSort(['breadcrumb', 'coveragetable'])\">\n" +
                "    <div class=\"breadcrumb\" id=\"breadcrumb\"><span class=\"info\"><a href=\"jacoco-sessions.html\" class=\"el_session\">Sessions</a></span><span class=\"el_report\">manualDiffCoverageReport</span></div>\n" +
                "    <h1>manualDiffCoverageReport</h1>\n" +
                "    <table class=\"coverage\" cellspacing=\"0\" id=\"coveragetable\">\n" +
                "        <thead>\n" +
                "            <tr>\n" +
                "                <td class=\"sortable\" id=\"a\" onclick=\"toggleSort(this)\">Element</td>\n" +
                "                <td class=\"down sortable bar\" id=\"b\" onclick=\"toggleSort(this)\">Missed Instructions</td>\n" +
                "                <td class=\"sortable ctr2\" id=\"c\" onclick=\"toggleSort(this)\">Cov.</td>\n" +
                "                <td class=\"sortable bar\" id=\"d\" onclick=\"toggleSort(this)\">Missed Branches</td>\n" +
                "                <td class=\"sortable ctr2\" id=\"e\" onclick=\"toggleSort(this)\">Cov.</td>\n" +
                "                <td class=\"sortable ctr1\" id=\"f\" onclick=\"toggleSort(this)\">Missed</td>\n" +
                "                <td class=\"sortable ctr2\" id=\"g\" onclick=\"toggleSort(this)\">Cxty</td>\n" +
                "                <td class=\"sortable ctr1\" id=\"h\" onclick=\"toggleSort(this)\">Missed</td>\n" +
                "                <td class=\"sortable ctr2\" id=\"i\" onclick=\"toggleSort(this)\">Lines</td>\n" +
                "                <td class=\"sortable ctr1\" id=\"j\" onclick=\"toggleSort(this)\">Missed</td>\n" +
                "                <td class=\"sortable ctr2\" id=\"k\" onclick=\"toggleSort(this)\">Methods</td>\n" +
                "                <td class=\"sortable ctr1\" id=\"l\" onclick=\"toggleSort(this)\">Missed</td>\n" +
                "                <td class=\"sortable ctr2\" id=\"m\" onclick=\"toggleSort(this)\">Classes</td>\n" +
                "            </tr>\n" +
                "        </thead>\n" +
                "        <tbody>\n" +
                "        </tbody>\n" +
                "<tfoot></tfoot>"+
                "    </table>\n" +
                "    <div class=\"footer\"><span class=\"right\">Created with <a href=\"http://www.jacoco.org/jacoco\">JaCoCo</a> 1.0.1.201909190214</span></div>\n" +
                "</body>\n" +
                "</html>";
        try {
            // 使用Jsoup库解析HTML模板字符串创建一个Document对象
            Document docSchema = Jsoup.parse(htmlSchema);
            // 初始化一个长度为15的Integer数组，用于存储合并后各项统计数据
            // 初始化所有统计计数器为0
            Integer[] array = new Integer[15];
            array[0] = 0;
            array[1] = 0;
            array[2] = 0;
            array[3] = 0;
            array[4] = 0;
            array[5] = 0;
            array[6] = 0;
            array[7] = 0;
            array[8] = 0;
            array[9] = 0;
            array[10] = 0;
            array[11] = 0;
            array[12] = 0;
            array[13] = 0;
            array[14] = 0;
            // 获取并操作模板中的tbody元素
            Element tbodySchema = docSchema.getElementsByTag("table").first();
            // 遍历输入的HTML文件列表
            for (String fileName : fileList) {
                // 根据文件名构建实际文件对象，并提取模块名称
                File file=new File(fileName);
                String module=new File(file.getParent()).getName();
                // 解析单个覆盖率报告文件内容到Document对象
                Document docc = Jsoup.parse(new File(fileName), "UTF-8", "");
                // 将相对链接转换为基于模块的绝对链接
                Document doc=Jsoup.parse(docc.toString().replace("<a href=\"","<a href=\""+module+"/"));
                // 如果当前文档没有tbody标签，则跳过此文件
                if(doc.getElementsByTag("tbody").first()==null){
                    continue;
                }
                // 将单个报告的tbody内容合并到汇总报告中
                Elements trs = doc.getElementsByTag("tbody").first().getElementsByTag("tr");
                for (Element ele : trs) {
                    tbodySchema.getElementsByTag("tbody").first().append(ele.html());
                }
                // 提取并累加各个统计指标
                String[] a = doc.getElementsByTag("tfoot").first().child(0).text().split(" ");
                array[1] = array[1] + Integer.parseInt(a[1].replace(",", ""));
                array[2] = array[2] + Integer.parseInt(a[3].replace(",", ""));
                //array[3] = array[3] + Integer.parseInt(a[4].replace("%", ""));
                array[4] = array[4] + Integer.parseInt(a[5].replace(",", ""));
                array[5] = array[5] + Integer.parseInt(a[7].replace(",", ""));
                //array[6] = array[6] + Integer.parseInt(a[8].replace("%", ""));
                array[7] = array[7] + Integer.parseInt(a[9].replaceAll(",",""));
                array[8] = array[8] + Integer.parseInt(a[10].replace(",", ""));
                array[9] = array[9] + Integer.parseInt(a[11].replace(",", ""));
                array[10] = array[10] + Integer.parseInt(a[12].replace(",", ""));
                array[11] = array[11] + Integer.parseInt(a[13].replace(",", ""));
                array[12] = array[12] + Integer.parseInt(a[14].replace(",", ""));
                array[13] = array[13] + Integer.parseInt(a[15].replace(",", ""));
                array[14] = array[14] + Integer.parseInt(a[16].replace(",", ""));
            }
            // 对于某些可能为0的统计项进行修正，避免除以零错误
            if(array[2]==0){
                array[1]=1;
                array[2]=1;
            }
            // 对于某些可能为0的统计项进行修正，避免除以零错误
            if(array[5]==0){
                array[4]=1;
                array[5]=1;
            }
            // 对于某些可能为0的统计项进行修正，避免除以零错误
            if(array[10]==0){
                array[9]=1;
                array[10]=1;
            }
            // 构建汇总报告的总计行，并添加到tfoot部分
            String tfoot = "         <tr>\n" +
                    "                <td>Total</td>\n" +
                    "                <td class=\"bar\">" + array[1] + " of " + array[2] + "</td>\n" +
                    "                <td class=\"ctr2\">" + (array[2]-array[1])*100/array[2] + "%</td>\n" +
                    "                <td class=\"bar\">" + array[4] + " of " + array[5] + "</td>\n" +
                    "                <td class=\"ctr2\">" + (array[5]-array[4])*100/array[5] + "%</td>\n" +
                    "                <td class=\"ctr1\">" + array[7] + "</td>\n" +
                    "                <td class=\"ctr2\">" + array[8] + "</td>\n" +
                    "                <td class=\"ctr1\">" + array[9] + "</td>\n" +
                    "                <td class=\"ctr2\">" + array[10] + "</td>\n" +
                    "                <td class=\"ctr1\">" + array[11] + "</td>\n" +
                    "                <td class=\"ctr2\">" + array[12] + "</td>\n" +
                    "                <td class=\"ctr1\">" + array[13] + "</td>\n" +
                    "                <td class=\"ctr2\">" + array[14] + "</td>\n" +
                    "            </tr>\n";
            tbodySchema.getElementsByTag("tfoot").first().append(tfoot);
            // 将合并后的汇总报告写入目标文件
            FileWriter writer = new FileWriter(destFile);
            writer.write(docSchema.toString());
            writer.flush();
            // 更新结果数组：设置状态码为成功（1），并将两个覆盖率指标放入结果数组
            result[0]=1;
            result[1]=(array[5]-array[4])*100/array[5];// 计算分支覆盖率
            result[2]=(array[10]-array[9])*100/array[10];// 计算行覆盖率
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 获取Spring Boot工程中每个子模块的"target/site"目录绝对路径。
     * @param rootPath Spring Boot工程的根目录地址
     * @return 一个字符串数组，包含所有子模块"target/site"目录的绝对路径
     */
    public static List<String> getSubModuleSitePaths(String rootPath) {
        List<String> sitePaths = new ArrayList<>();
        // 检查给定路径是否为有效的目录
        File projectRoot = new File(rootPath);
        if (!projectRoot.exists() || !projectRoot.isDirectory()) {
            throw new IllegalArgumentException("输入的工程目录不存在/找不到，请检查");
        }
        // 遍历工程根目录下的所有子目录，寻找包含pom.xml文件的目录（假设为子模块）
        File[] submodules = projectRoot.listFiles((dir, name) -> new File(dir, name).isDirectory() && hasPomXml(new File(dir, name)));

        // 对于每一个找到的子模块，检查是否存在并是目录的"target/site"
        for (File submodule : submodules) {
            File siteDir = new File(submodule, "target/site");
            if (siteDir.exists() && siteDir.isDirectory()) {
                // 如果存在，则将该"target/site"目录的绝对路径添加到结果列表中
                sitePaths.add(siteDir.getAbsolutePath());
            }
        }
        if (sitePaths.size()==0){
            log.info("该路径下没有对应单元测试报告"+rootPath);
        }
        // 将结果列表转换为字符串数组并返回
        return sitePaths;
    }

    /**
     * @Description:  复制module的单测报告到report路径下
     * @param: sitePath
     * @param: coverageReport
     * @return * @return void
     * @author panpeng
     * @date 2024/2/22 20:35
    */
    public static List<String> copyReport(List<String> sitePath, CoverageReportEntity coverageReport){
        String gitName = coverageReport.getGitName();
        List<String> newSitePaths = new ArrayList<>();
        for (String path:sitePath){
            String moduleName = extractModuleName(gitName+"/"+path);
            try {
                //先创建目标路径
                String[] cppCmd_1 = new String[]{"mkdir -p "+ REPORT_PATH + coverageReport.getUuid() +"/" +gitName +"/" +moduleName + "/"};
                CmdExecutor.executeCmd(cppCmd_1, CMD_TIMEOUT);
                //再执行复制
                String[] cppCmd = new String[]{"cp -rf " + path + " " + REPORT_PATH + coverageReport.getUuid() +"/" +gitName +"/" +moduleName + "/"};
                CmdExecutor.executeCmd(cppCmd, CMD_TIMEOUT);
                newSitePaths.add(REPORT_PATH + coverageReport.getUuid() +"/" +gitName +"/" +moduleName + "/");
            }catch (Exception e){
               log.info("复制单元测试报告时失败"+path);
            }
        }
        return newSitePaths;
    }

    private static String extractModuleName(String path) {
        // 查找 "/target/site" 前一个斜杠在字符串中的索引位置
        int lastIndex = path.lastIndexOf("/", path.lastIndexOf("/target/site") - 1);

        // 确保找到了这个斜杠
        if (lastIndex != -1) {
            // 提取最后一个斜杠与倒数第二个斜杠之间的部分，即子模块名称
            return path.substring(lastIndex + 1, path.lastIndexOf("/target/site"));
        } else {
            throw new IllegalArgumentException("Invalid path. Could not extract module name from the provided path.");
        }
    }


    /**
     * 检查给定的目录是否包含pom.xml文件
     * @param dir 需要检查的目录
     * @return 如果目录包含pom.xml文件则返回true，否则返回false
     */
    private static boolean hasPomXml(File dir) {
        return new File(dir, "pom.xml").exists();
    }

    /**
     * @Description: 构建一个html，包含各个结果的路径,并把结果放到 ~/report下
     * 废弃不用，使用动态html
     * @param:
     * @return * @return void
     * @author panpeng
     * @date 2024/2/29 09:59
    */
    public static void buildHtmlReport(HashMap<String,Integer> nums,List<String> paths,String destFile){
        int a = nums.get("Tests");
        int b = nums.get("Errors");
        int c = nums.get("Failures");
        int d = nums.get("Skipped");
        // 首先计算分子
        double numerator = a - b - c - d;
        // 分母是a
        double denominator = a;
        // 防止分母为0的情况
        if (denominator == 0) {
            throw new IllegalArgumentException("Cannot calculate percentage with a denominator of zero.");
        }
        // 计算百分比
        double percentage = (numerator / denominator) * 100;
        // 四舍五入到两位小数
        double ans = Math.round(percentage * 100.0) / 100.0;

        //基础单元测试 html模版
        String htmlSchema = "<!DOCTYPE html>\n" +
                "<html xmlns:th=\"http://www.thymeleaf.org\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>测试报告</title>\n" +
                "</head>\n" +
                "    <h2>单元测试结果展示</h2>\n" +
                "\n" +
                "<body>\n" +
                "    <table border=\"1\" cellpadding=\"5\">\n" +
                "        <thead>\n" +
                "            <tr>\n" +
                "                <th>Test Case</th>\n" +
                "                <th>Error</th>\n" +
                "                <th>Fail</th>\n" +
                "                <th>Skip</th>\n" +
                "                <th>单元测试通过率</th>\n" +
                "            </tr>\n" +
                "        </thead>\n" +
                "        <tbody>\n" +
                "            <tr>\n" +
                "                <td th:text=\"${testCase}\">"+ a +"</td>\n" +
                "                <td th:text=\"${error}\">"+ b +"</td>\n" +
                "                <td th:text=\"${fail}\">"+ c +"</td>\n" +
                "                <td th:text=\"${skip}\">"+ d +"</td>\n" +
                "                <td th:text=\"${skip}\">"+ ans +"</td>\n" +

                "            </tr>\n" +
                "        </tbody>\n" +
                "    </table>\n" +
                "\n"+
                "    <h2>本地HTML链接列表：</h2>\n" +
                "    <ul>\n" +
                "        <li th:each=\"link : ${localLinks}\">\n";
        for (String path : paths){
//            String pathName = extractModuleName(path+"/site");
            htmlSchema = htmlSchema + "\n<a th:href=file:///"+path+"surefire-report.html th:text=\"${link.name}\">"+path+"</a>\n";
        }
        htmlSchema = htmlSchema+
                "\n        </li>\n" +
                "    </ul>\n" +
                "</body>\n" +
                "</html>";

        try{
            Document docSchema = Jsoup.parse(htmlSchema);
            FileWriter writer = new FileWriter(destFile);
            writer.write(docSchema.toString());
            writer.flush();
        }catch (IOException e){
            log.info("生成html文件失败"+e);
        }
    }

    /**
     * @Description: 计算子模块的单测结果，进行合并
     * @param:
     * @return * @return void
     * @author panpeng
     * @date 2024/2/29 10:01
    */
    public static HashMap<String,String> calculateUnitTestResult(List<String> paths){
        HashMap<String,String> res = new HashMap<>();
        int a = 0;
        int b = 0;
        int c = 0;
        int d = 0;
        if (Objects.isNull(paths)||paths.size()==0){
            log.info("子模块的单测结果时，输入路径有误");
            return res;
        }
        for (String path:paths){
            try {
                Document doc = Jsoup.parse(new File(path + "site/surefire-report.html"), "UTF-8");
//                System.out.println(path);
                //找到单元测试结果字段
                Elements rows = doc.select("tr.b");
                for (Element row : rows) {
                    if (row.hasClass("b")) {
                        Element firstTd = row.selectFirst("td[align='left']");

                        if (firstTd != null) {
                            try {
                                a += Integer.parseInt(firstTd.text());
                                // 获取剩余三个<td>元素
                                Elements otherTds = row.select("td:not([align='left'])");
                                b += Integer.parseInt(otherTds.get(0).text());
                                c += Integer.parseInt(otherTds.get(1).text());
                                d += Integer.parseInt(otherTds.get(2).text());
                            } catch (NumberFormatException e) {
                                log.info("无法将某些字段转换为整数: " + e.getMessage());
                            }
                        }
                    }
                }
            }catch (IOException e){
                log.info("读取文件时发生错误: " + path + " - " + e.getMessage());
            }
        }
        int e = a - b - c - d;
        res.put("Tests",String.valueOf(a));
        res.put("Errors",String.valueOf(b));
        res.put("Failures",String.valueOf(c));
        res.put("Skipped",String.valueOf(d));
        res.put("Success",String.valueOf(e));
        // 首先计算分子
        double numerator = a - b - c - d;
        // 分母是a
        double denominator = a;
        // 防止分母为0的情况
        double percentage = 0;
        if (a == 0) {
             percentage = 0;
            throw new IllegalArgumentException("Cannot calculate percentage with a denominator of zero.");
        }else {
             percentage = (numerator / denominator) * 100;
        }
        // 计算百分比
        // 四舍五入到两位小数
        double ans = Math.round(percentage * 100.0) / 100.0;
        res.put("PassRate",String.valueOf(ans));
        return res;
    }

//    public static List<String>  buildWebPath(List<String> paths,CoverageReportEntity coverageReport){
//        List<String> res = new ArrayList<>();
//        String uuid = coverageReport.getUuid();
//        for (String path:paths){
//            int start = path.indexOf("/" + uuid);
//            if (start != -1) {
//                start = start + uuid.length() + 1;
//                // 找到".html"出现的位置
//                int end = path.lastIndexOf(".html");
//                // 截取所需子串，注意加上"/"和".html"
//                res.add(path.substring(start, end + 5));
//            }
//        }
//        return res;
//    }
public static List<String> buildWebPath(List<String> paths, CoverageReportEntity coverageReport) {
    List<String> res = new ArrayList<>();
    String uuid = coverageReport.getUuid();
    for (String path : paths) {
        path = path + "site/surefire-report.html";
        int start = path.indexOf("/" + uuid);
        if (start != -1) {
            start = start + uuid.length() + 1;
            // 找到".html"出现的位置
            int end = path.lastIndexOf(".html");

            // 添加一个额外的检查，确保end不为-1（即".html"存在），并且start小于end
            if (end != -1 && start < end) {
                // 截取所需子串，注意加上"/"和".html"
                res.add("/"+uuid+path.substring(start, end + 5));
            }
        }
    }
    return res;
}

}


