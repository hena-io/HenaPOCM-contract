package io.nuls.pocm.contract.model;

public class DepositPolicy {
    public long policyTime;
    public double rewardRate;

      public DepositPolicy(long policyTime, double rewardRate){
          this.policyTime = policyTime;
          this.rewardRate = rewardRate;
      }

      @Override
      public String toString(){
          return "{policyTime:"+policyTime+", rewardRate:"+rewardRate+"}";
      }
}
