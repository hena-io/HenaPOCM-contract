package io.nuls.pocm.contract.model;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.contract.sdk.Utils.require;

public class DepositInfo {

    private long userID;
    //抵押者地址 모기지 주소
    private String  depositorAddress;

    // 抵押金额 모기지 금액
    private BigInteger depositTotalAmount;

    //抵押笔数 모기지 수
    private int depositCount;

    //抵押详细信息列表 모기지 세부 정보 목록
    private Map<Long,DepositDetailInfo> depositDetailInfos =new HashMap<Long,DepositDetailInfo>();

    public DepositInfo(){
        this.depositTotalAmount=BigInteger.ZERO;
        this.depositCount=0;
    }

    public DepositInfo(DepositInfo info){
        this.depositorAddress=info.depositorAddress;
        this.depositTotalAmount=info.depositTotalAmount;
        this.depositCount=info.depositCount;
        this.depositDetailInfos=info.depositDetailInfos;
    }

    public BigInteger getDepositTotalAmount() {
        return depositTotalAmount;
    }

    public void setDepositTotalAmount(BigInteger depositTotalAmount) {
        this.depositTotalAmount = depositTotalAmount;
    }

    public Map<Long, DepositDetailInfo> getDepositDetailInfos() {
        return depositDetailInfos;
    }

    public void setDepositDetailInfos(Map<Long, DepositDetailInfo> depositDetailInfos) {
        this.depositDetailInfos = depositDetailInfos;
    }

    public long getuserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    public int getDepositCount() {
        return depositCount;
    }

    public void setDepositCount(int depositCount) {
        this.depositCount = depositCount;
    }

    public String getDepositorAddress() {
        return depositorAddress;
    }

    public void setDepositorAddress(String depositorAddress) {
        this.depositorAddress = depositorAddress;
    }

    public DepositDetailInfo getDepositDetailInfoByNumber(long depositNumber){
        DepositDetailInfo info=depositDetailInfos.get(depositNumber);
        require(info != null, "未找到此抵押编号的抵押详细信息");
        return info;
    }

    public void removeDepositDetailInfoByNumber(long depositNumber){
        depositDetailInfos.remove(depositNumber);
    }

    public void clearDepositDetailInfos(){
        depositDetailInfos.clear();
        depositCount=0;
        depositTotalAmount=BigInteger.ZERO;
    }

    @Override
    public String toString(){
        return  "{depositTotalAmount:"+depositTotalAmount+",depositorAddress:"+depositorAddress
                +",depositCount:"+depositCount+",depositDetailInfos:"+convertMapToString()+"}}";
    }

    private  String convertMapToString(){
        String detailinfo ="{";
        String temp="";
        for (Long key : depositDetailInfos.keySet()) {
            DepositDetailInfo detailInfo=  depositDetailInfos.get(key);
            temp =detailInfo.toString();
            detailinfo=detailinfo+temp+",";
        }
        detailinfo=detailinfo.substring(0,detailinfo.length()-1);
        detailinfo=detailinfo+"}";
        return detailinfo;
    }
}
