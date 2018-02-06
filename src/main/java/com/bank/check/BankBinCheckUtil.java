package com.bank.check;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 卡bin校验工具类
 */
public class BankBinCheckUtil {

    public static final String[] READ_FILE_HEADER = {"类型", "渠道编号", "渠道上送账户编号", "渠道来源",
            "收款账户名称", "收款账号", "收款银行名称","商户id", "打款失败次数", "最近打款时间"};

    public static final String[] WRITE_FILE_HEADER = {"类型", "渠道编号", "渠道上送账户编号", "渠道来源",
            "收款账户名称", "收款账号", "收款银行名称","商户id", "正确的收款银行名称", "打款失败次数", "最近打款时间"};

    private static final String BANK_CARD = "BANK_CARD";

    private static final String bankNamePath = "bank_name.json";

    private static final Map bankMap = getBankMap();


    /**
     * 获取银行列表
     *
     * @return
     */
    private static Map getBankMap() {
        FileReader reader = null;
        try {
            ClassLoader classLoader = BankBinCheck.class.getClassLoader();
            URL url = classLoader.getResource(bankNamePath);
            reader = new FileReader(url.getFile());
            BufferedReader br = new BufferedReader(reader);
            StringBuilder str = new StringBuilder();
            String temp = "";
            while (null != temp) {
                str.append(temp);
                temp = br.readLine();
            }
            return JSONObject.parseObject(str.toString(), HashMap.class);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Response getCardInfo(String inAccountNumber) throws Exception {
        String url = "https://ccdcapi.alipay.com/validateAndCacheCardInfo.json?_input_charset=utf-8&cardNo=" + BANK_CARD + "&cardBinCheck=true";
        url = url.replace(BANK_CARD, inAccountNumber);
        HttpGet request = new HttpGet(url);
        // 获取当前客户端对象
        HttpClient httpClient = new DefaultHttpClient();
        // 通过请求对象获取响应对象
        HttpResponse response = httpClient.execute(request);

        Response responseObj = new Response();
        // 判断网络连接状态码是否正常(0--200都数正常)
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            String result = EntityUtils.toString(response.getEntity(), "utf-8");
            System.out.println("result:" + result);
            responseObj = JSONObject.parseObject(result, Response.class);
        }
        return responseObj;
    }


    /**
     * 产生绑卡错误的数据
     *
     * @param fileNamePath
     * @param descFileName
     * @throws Exception
     */
    public static void generateBindErrorBanks(String fileNamePath, String descFileName) {
        Reader in = null;
        Writer out = null;
        try {
            in = new FileReader(fileNamePath);
            // 这里显式地配置一下CSV文件的Header，然后设置跳过Header
            CSVFormat readFormat = CSVFormat.DEFAULT.withHeader(READ_FILE_HEADER).withSkipHeaderRecord();
            Iterable<CSVRecord> records = readFormat.parse(in);
            //写文件
            out = new FileWriter(descFileName);
            CSVFormat writeFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
            CSVPrinter printer = new CSVPrinter(out, writeFormat);
            printer.printRecord(WRITE_FILE_HEADER);
            List<CSVRecord> recordList = IteratorUtils.toList(records.iterator());
            System.out.println("需查询" + recordList.size() + "条数据。");
            int i = 1;
            for (CSVRecord record : recordList) {
                System.out.println("当前第" + i + "条");
                Response response = getCardInfo(record.get("收款账号"));
                String bankName = (String) bankMap.get(response.getBank());
                if (StringUtils.isNotBlank(bankName) && !record.get("收款银行名称").equals(bankName) && !("中国"+record.get("收款银行名称")).equals(bankName)) {
                    System.out.println("银行名称不匹配");
                    List<String> datas = new ArrayList<>();
                    datas.add(record.get("类型"));
                    datas.add(record.get("渠道编号"));
                    datas.add(record.get("渠道上送账户编号"));
                    datas.add(record.get("渠道来源"));
                    datas.add(record.get("收款账户名称"));
                    datas.add(record.get("收款账号"));
                    datas.add(record.get("收款银行名称"));
                    datas.add(record.get("商户id"));
                    if (response.isValidated()) {
                        if (StringUtils.isNotBlank(bankName)) {
                            datas.add(bankName);
                        } else {
                            datas.add(response.getBank());
                        }
                    } else {
                        datas.add(response.getMessages().get(0).getErrorCodes());
                    }

                    datas.add(record.get("打款失败次数"));
                    datas.add(record.get("最近打款时间"));

                    printer.printRecord(datas);
                }
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查是否存在绑卡错误的数据
     * @param fileNamePath
     */
    public static void checkBindErrorBanks(String fileNamePath,String[] READ_FILE_HEADER){
        Reader in = null;
        try {
            in = new FileReader(fileNamePath);
            // 这里显式地配置一下CSV文件的Header，然后设置跳过Header
            CSVFormat readFormat = CSVFormat.DEFAULT.withHeader(READ_FILE_HEADER).withSkipHeaderRecord();
            Iterable<CSVRecord> records = readFormat.parse(in);
            List<CSVRecord> recordList = IteratorUtils.toList(records.iterator());
            System.out.println("需查询" + recordList.size() + "条数据。");
            int i = 1;
            for (CSVRecord record : recordList) {
                System.out.println("当前第" + i + "条");
                Response response = getCardInfo(record.get("收款账号"));
                String bankName = (String) bankMap.get(response.getBank());
                if (StringUtils.isNotBlank(bankName) && !record.get("正确的收款银行名称").equals(bankName)) {
                    System.out.println("银行名称不匹配-------------");
                    System.out.println("old bank data: " + JSONObject.toJSONString(record.toMap()));
                    System.out.println("new bank data: " + JSONObject.toJSONString(response));
                    return;
                }
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
