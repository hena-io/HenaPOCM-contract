package io.nuls.pocm.contract.model;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.contract.sdk.Utils.require;

public class DepositInfo {

    private long userID;

    private String  depositorAddress;

    private BigInteger depositTotalAmount;

    private int depositCount;

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
        String detailInfosStr = "";
        int index = 0;
        for (Long key : depositDetailInfos.keySet()) {
            if (index > 0)
                detailInfosStr += ",";
            detailInfosStr += depositDetailInfos.get(key).toString();
            index++;
        }

        return String.format("{depositTotalAmount:%s, depositorAddress:%s, depositCount:%s, depositDetailInfos:[%s]}",
                depositTotalAmount,
                depositorAddress,
                depositCount,
                detailInfosStr
                );
    }

}
