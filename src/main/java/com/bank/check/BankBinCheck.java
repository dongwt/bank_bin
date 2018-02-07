package com.bank.check;

/**
 * Created by dongwt on 2018/2/5.
 */
public class BankBinCheck {

    public static void main(String[] args){
//        BankBinCheckUtil.generateBindErrorBanks("/Users/dongwt/Desktop/test.csv","/Users/dongwt/Desktop/result.csv");

//        BankBinCheckUtil.checkBindErrorBanks("/Users/dongwt/Desktop/result.csv",BankBinCheckUtil.WRITE_FILE_HEADER);


        String[] fileHeader = {"类型", "渠道编号", "渠道上送账户编号", "渠道来源",
                "收款账户名称", "收款账号", "收款银行名称","正确的收款银行名称", "打款失败次数", "最近打款时间"};
        BankBinCheckUtil.getNoMatchBank("/Users/dongwt/Desktop/result_all.csv","/Users/dongwt/Desktop/result_not_match.csv",fileHeader);
    }
}
