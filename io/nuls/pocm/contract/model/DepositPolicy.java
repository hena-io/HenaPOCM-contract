package io.nuls.pocm.contract.model;

public class DepositPolicy {
    public long policyTime; //예치기간
    public double rewardRate;

      public DepositPolicy(long policyTime, double rewardRate){
          this.policyTime = policyTime;
          this.rewardRate = rewardRate;
      }

      @Override
      public String toString(){
          String str = "{";
          str += "policyTime:"+policyTime;
          str += ", rewardRate:"+rewardRate;
          return str += "}";
      }
}
