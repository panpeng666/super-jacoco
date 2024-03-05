package com.xiaoju.basetech.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @description:  定义一个名为MergeReportHtml的公共类，用于合并多个JaCoCo覆盖率报告生成一个HTML格式的汇总报告
 * @author: gaoweiwei_v
 * @time: 2019/12/12 8:41 AM
 */
public class MergeReportHtml {
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
}