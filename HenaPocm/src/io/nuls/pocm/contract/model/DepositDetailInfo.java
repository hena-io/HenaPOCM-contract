package io.nuls.pocm.contract.model;

import java.math.BigInteger;

public class DepositDetailInfo {

    private long depositNumber;
    private BigInteger depositAmount=BigInteger.ZERO;
    private long depositTime;

    public long policyTime;
    public double rewardRate;

    private String miningAddress;

    public BigInteger getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigInteger depositAmount) {
        this.depositAmount = depositAmount;
    }

    public long getDepositTime() {
        return depositTime;
    }

    public void setDepositTime(long depositTime) {
        this.depositTime = depositTime;
    }

    public String getMiningAddress() {
        return miningAddress;
    }

    public void setMiningAddress(String miningAddress) {
        this.miningAddress = miningAddress;
    }

    public long getDepositNumber() {
        return depositNumber;
    }

    public void setDepositNumber(long depositNumber) {
        this.depositNumber = depositNumber;
    }

    public DepositPolicy getDepositPolicy(){
        return new DepositPolicy(this.policyTime, this.rewardRate);
    }
    public void setDepositPolicy( DepositPolicy depositPolicy){
        this.policyTime = depositPolicy.policyTime;
        this.rewardRate = depositPolicy.rewardRate;
    }

    @Override
    public String toString(){

        return "{depositNumber:"+depositNumber+
                ", miningAddress:"+miningAddress+
                ", depositAmount:"+depositAmount+
                ", depositTime:"+depositTime+
                ", depositPolicy:"+getDepositPolicy().toString()+"}";
    }
}
