
package io.nuls.pocm.contract;

import io.nuls.pocm.contract.event.DepositInfoEvent;
import io.nuls.pocm.contract.model.*;
import io.nuls.pocm.contract.ownership.Ownable;
import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Block;
import io.nuls.contract.sdk.Contract;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.Payable;
import io.nuls.contract.sdk.annotation.Required;
import io.nuls.contract.sdk.annotation.View;
import io.nuls.pocm.contract.util.PocmUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;

public class Pocm extends Ownable implements Contract {

    private final long createTime;
    private BigInteger minimumDeposit;
    private long totalDepositAddressCount;
    private long minimumLocked;
    private double minimumRewardRate = 0.1;

    private Map<String, DepositInfo> depositUsers = new HashMap<String, DepositInfo>();
    private Map<Integer, DepositPolicy> deposiPolicys = new HashMap<Integer, DepositPolicy>();

    //test
    //private final long DAY_SEC = 2;
    private final long DAY_SEC = 60*60*24;
    private final long MONTH_SEC = DAY_SEC*30;
    private final long YEAR_SEC = MONTH_SEC*12;
    private BigInteger totalDeposit;

    private static long DEPOSIT_NUMBER=1;
    private static long USER_ID = 0;

    private BigInteger totHena;
    private BigInteger remainHena;
    private BigInteger minimumRemainHena;

    private boolean miningFinish = false;
    private long POCMStartTime;
    private long rewardTokenLockMonth;
    private boolean endOfPOCM = false;
    private long POCMEndTime = 0;

    BigDecimal henaPrice;
    BigDecimal nulsPrice;

    private final int decimals = 8;
    private BigDecimal minimumRewardRateInYear = BigDecimal.valueOf(0.1);
    private Address henaTokenAddress;


    public Pocm(@Required Address tokenAddress,  @Required Address ownerAddress) {
        super(ownerAddress);

        henaTokenAddress = tokenAddress;
        createTime = getTime();
        totalDeposit = BigInteger.ZERO;
        totalDepositAddressCount = 0;

        //test
        //minimumDeposit = PocmUtil.toNa(new BigDecimal("0.1")); //0.1 nuls
        minimumDeposit = PocmUtil.toNa(BigDecimal.valueOf(100)); //100 nuls
        totHena = PocmUtil.toNa(BigDecimal.valueOf(50000000)); //50,000,000 Hena
        remainHena = totHena;
        minimumRemainHena = PocmUtil.toNa(BigDecimal.valueOf(10000));
        rewardTokenLockMonth = 3; //3 month
        henaPrice = BigDecimal.valueOf(0.088);//dolor
        nulsPrice = BigDecimal.valueOf(0.8); //dolor
        minimumLocked = MONTH_SEC*2; //2month
        POCMStartTime = getTime()+ MONTH_SEC;

        //policy setting
        int[] month ={3, 6, 9, 12};//month
        double[] rewardRate = {0.045, 0.125, 0.2625, 0.4};//18%, 25%, 35%, 40% rate of year
        setDepositPolicy(month,rewardRate);

    }

    public void setRewardTokenLockMonth(long lockMonth){
        requireManager(Msg.sender());
        this.rewardTokenLockMonth = lockMonth;
    }

    public void setDepositPolicy(@Required int[] months, @Required double[] rewardRates){
        requireManager(Msg.sender());
        for(int i=0; i< months.length; i++ ){
            long policyMonthSec = months[i]*MONTH_SEC;
            deposiPolicys.put(new Integer(months[i]), new DepositPolicy( policyMonthSec, rewardRates[i]));
        }
    }

    public void setPOCMStartTime(long startTime){
        requireManager(Msg.sender());
        this.POCMStartTime = startTime;
    }

    public void setMinimumDeposit(BigInteger minimumDeposit){
        requireManager(Msg.sender());
        this.minimumDeposit = minimumDeposit;
    }

    public String updateHenaNulsPrice(@Required double nulsPrice,@Required double henaPrice){
        requireManager(Msg.sender());
        this.nulsPrice = new BigDecimal(nulsPrice);
        this.henaPrice = new BigDecimal(henaPrice);
        return "{nulsPrice:"+this.nulsPrice.toString()+", henaPrice:"+this.henaPrice.toString()+"}";
    }


    @Payable
    public void depositForOther(@Required Address rewardAddress, @Required int depositMonth) {

        require(!isEndOfPOCM(), "The POCM is over");
        DepositPolicy policy = deposiPolicys.get(new Integer(depositMonth));
        require( policy != null, "There is no policy for "+depositMonth + " month");

        String userAddress = Msg.sender().toString();
        DepositInfo info =depositUsers.get(userAddress);
        if(info==null){
            info =new DepositInfo();
            info.setUserID(USER_ID++);
            depositUsers.put(userAddress,info);
            totalDepositAddressCount += 1;
        }

        BigInteger value = Msg.value();
        require(value.compareTo(minimumDeposit) >= 0, "Please deposit more than "+minimumDeposit);
        long depositNumber =DEPOSIT_NUMBER++;

        DepositDetailInfo detailInfo = new DepositDetailInfo();
        detailInfo.setDepositAmount(value);

        long currentTime = getTime();
        if( currentTime < POCMStartTime)
            currentTime = POCMStartTime;

        detailInfo.setDepositTime(currentTime);
        detailInfo.setMiningAddress(rewardAddress.toString());
        detailInfo.setDepositNumber(depositNumber);
        detailInfo.setDepositPolicy(policy);

        info.setDepositorAddress(userAddress);
        info.getDepositDetailInfos().put(depositNumber,detailInfo);
        info.setDepositTotalAmount(info.getDepositTotalAmount().add(value));
        info.setDepositCount(info.getDepositCount()+1);

        totalDeposit = totalDeposit.add(value);
        emit(new DepositInfoEvent(info));
    }

    @Payable
    public void deposit(@Required int depositMonth) {

        require(!isEndOfPOCM(), "The POCM is over");
        DepositPolicy policy = deposiPolicys.get(new Integer(depositMonth));
        require( policy != null, "There is no policy for "+depositMonth + " month");

        String userAddress= Msg.sender().toString();
        DepositInfo info =depositUsers.get(userAddress);
        if(info==null){

            info =new DepositInfo();
            info.setUserID(USER_ID++);
            depositUsers.put(userAddress,info);
            totalDepositAddressCount += 1;
        }

        BigInteger value = Msg.value();
        require(value.compareTo(minimumDeposit) >= 0, "Please deposit more than "+minimumDeposit);
        long depositNumber =DEPOSIT_NUMBER++;

        DepositDetailInfo detailInfo = new DepositDetailInfo();
        detailInfo.setDepositAmount(value);

        long currentBlockTime = getTime();
        if( currentBlockTime < POCMStartTime)
            currentBlockTime = POCMStartTime;

        detailInfo.setDepositTime(currentBlockTime);
        detailInfo.setMiningAddress(userAddress);
        detailInfo.setDepositNumber(depositNumber);
        detailInfo.setDepositPolicy(policy);

        info.setDepositorAddress(userAddress);
        info.getDepositDetailInfos().put(depositNumber,detailInfo);
        info.setDepositTotalAmount(info.getDepositTotalAmount().add(value));
        info.setDepositCount(info.getDepositCount()+1);

        totalDeposit = totalDeposit.add(value);
        emit(new DepositInfoEvent(info));
    }

    public void quitHena(String number) {
        long depositNumber=0;
        if(number!=null&&number.trim().length()>0){
            require(PocmUtil.canConvertNumeric(number.trim(),String.valueOf(Long.MAX_VALUE)),"The deposit number is incorrect.");
            depositNumber= Long.valueOf(number.trim());
        }
        Address user = Msg.sender();
        DepositInfo depositInfo =getDepositInfo(user.toString());

        Map<String,BigInteger> rewardResult= new HashMap<String, BigInteger>();
        if(depositNumber == 0){
            require(checkAllDepositLocked(depositInfo) == -1, "There is a deposit that has not passed the minimum period." );
            calcDepositInfoReward(depositInfo, rewardResult);

        }else {
            DepositDetailInfo detailInfo =depositInfo.getDepositDetailInfoByNumber(depositNumber);
            require(checkDepositLocked(detailInfo) == -1, "The deposit has not passed the minimum period.");

            BigInteger reward = calcDepositDetailReward(detailInfo);
            String address = detailInfo.getMiningAddress();
            rewardResult.put(address,reward);
        }

        receiveHena(depositInfo, rewardResult);

        BigInteger deposit=BigInteger.ZERO;
        if(depositNumber==0){
            deposit=depositInfo.getDepositTotalAmount();
            depositInfo.clearDepositDetailInfos();
        }else{
            DepositDetailInfo detailInfo =depositInfo.getDepositDetailInfoByNumber(depositNumber);
            depositInfo.removeDepositDetailInfoByNumber(depositNumber);
            deposit = detailInfo.getDepositAmount();
            depositInfo.setDepositTotalAmount(depositInfo.getDepositTotalAmount().subtract(deposit));
            depositInfo.setDepositCount(depositInfo.getDepositCount()-1);
        }

        totalDeposit = totalDeposit.subtract(deposit);
        if(depositInfo.getDepositDetailInfos().size()==0){
            totalDepositAddressCount -= 1;
            depositUsers.remove(user.toString());
        }
        Msg.sender().transfer(deposit);
    }

    public boolean isEndOfPOCM(){
        if(endOfPOCM){
            return true;
        }else {
            BigInteger currentTotalReward = getTotPredictiveRewardAmount();
            if(remainHena.compareTo(currentTotalReward.add(minimumRemainHena)) <= 0){
                POCMEndTime = getTime();
                endOfPOCM = true;
                return true;
            }
            return false;
        }
    }


    private DepositInfo getDepositInfo(String userStr) {
        DepositInfo depositInfo = depositUsers.get(userStr);
        require(depositInfo != null, "There is no deposit info.");
        return depositInfo;
    }

    private long checkAllDepositLocked(DepositInfo depositInfo) {
        long result;
        Map<Long,DepositDetailInfo> infos =depositInfo.getDepositDetailInfos();
        for (Long key : infos.keySet()) {
            result =checkDepositLocked(infos.get(key));
            if(result!=-1){
                return result;
            }
        }
        return -1;
    }

    private long checkDepositLocked(DepositDetailInfo detailInfo) {
        //long currentHeight = Block.number();
        long currentTime = getTime();
        //long unLockedHeight = detailInfo.getDepositHeight() + minimumLocked + 1;
        long unLockedTime = detailInfo.getDepositTime() + minimumLocked + 1;
        if(unLockedTime > currentTime) {
            return unLockedTime;
        }
        return -1;
    }

    private String receiveHena(DepositInfo depositInfo, Map<String,BigInteger> rewardResult) {
        String resultStr = "";

        Set<String> set = new HashSet<String>(rewardResult.keySet());
        for(String rewardAddress:set){
            BigInteger rewardValue= rewardResult.get(rewardAddress);
            long tokenLockTime = getTime() + (MONTH_SEC * rewardTokenLockMonth);
            remainHena = remainHena.subtract(rewardValue);
            String[][] args = new String[][]{{rewardAddress},{ rewardValue.toString()}, {Long.toString(tokenLockTime)}};
            henaTokenAddress.callWithReturnValue("transferPOCM", null, args, null);
            resultStr += ", rewardAddress:"+rewardAddress +", rewardValue:"+ rewardValue.toString()+", ";
        }
        return resultStr;
    }

    private BigInteger getRewardAmount(BigInteger  depositAmount, long havingTime, DepositPolicy policy) {
        BigDecimal henaDepositAmount  =  nuls2Hena(new BigDecimal(depositAmount.toString()));
        if (havingTime >= policy.policyTime) {
            return henaDepositAmount.multiply(BigDecimal.valueOf(policy.rewardRate)).toBigInteger();
        } else if (havingTime >= policy.policyTime/2) {
            return henaDepositAmount.multiply(minimumRewardRateCalc(havingTime)).toBigInteger();
        }
        return BigInteger.ZERO;
    }

    private BigDecimal minimumRewardRateCalc(long havingTime){

        return BigDecimal.valueOf(minimumRewardRate).multiply(BigDecimal.valueOf(havingTime)).divide(BigDecimal.valueOf(YEAR_SEC), decimals, BigDecimal.ROUND_DOWN);
    }

    private BigInteger calcDepositDetailReward( DepositDetailInfo detailInfo){

        long havingTime = getTime() - detailInfo.getDepositTime();
        DepositPolicy depositPolicy = detailInfo.getDepositPolicy();
        long rewardLoop = havingTime / depositPolicy.policyTime;
        BigInteger depositDetailReward = BigInteger.ZERO;
        for(int i =0; i< rewardLoop; i++){
            depositDetailReward = depositDetailReward.add(getRewardAmount(detailInfo.getDepositAmount(), depositPolicy.policyTime, depositPolicy ));
        }
        long remainTime  = havingTime - (depositPolicy.policyTime * rewardLoop);
        depositDetailReward = depositDetailReward.add(getRewardAmount(detailInfo.getDepositAmount(), remainTime, depositPolicy ));
        return depositDetailReward;
    }

    private BigInteger calcDepositInfoReward(DepositInfo depositInfo, Map<String,BigInteger> rewardResult) {
        BigInteger totalReward = BigInteger.ZERO;
        Map<Long,DepositDetailInfo> detailInfos=depositInfo.getDepositDetailInfos();
        long currentTime = getTime();
        for (Long key : detailInfos.keySet()) {
            DepositDetailInfo detailInfo = detailInfos.get(key);
            BigInteger reward = calcDepositDetailReward(detailInfo);
            String address = detailInfo.getMiningAddress();
            totalReward = totalReward.add(reward);
            if( rewardResult.containsKey(address)){
                rewardResult.put(address,rewardResult.get(address).add(reward));
            }else{
                rewardResult.put(address,reward);
            }
        }
        return totalReward;
    }


    private BigDecimal nuls2Hena(BigDecimal nulsAmount){
        return nulsAmount.multiply( nulsPrice.divide(henaPrice, decimals, BigDecimal.ROUND_DOWN));
    }
    private BigDecimal hena2Nuls(BigDecimal henaAmount){
        return henaAmount.multiply( henaPrice.divide(nulsPrice, decimals, BigDecimal.ROUND_DOWN));
    }


    @View
    public long getRewardTokenLockMonth(){
        return this.rewardTokenLockMonth;
    }

    @View
    public String getDepositPolicy(){
        String monthsStr = "";
        int index = 0;
        for (Integer key : deposiPolicys.keySet()) {
            if (index > 0)
                monthsStr += ",";
            monthsStr += key.toString();
            index++;
        }

        String depositPolicysStr = "";
        index = 0;
        for (Integer key : deposiPolicys.keySet()) {
            if (index > 0)
                depositPolicysStr += ",";
            depositPolicysStr+= deposiPolicys.get(key).toString();
            index++;
        }
        return "{months:["+monthsStr+"], depositPolicys:["+depositPolicysStr+"]}";
    }

    @View
    public String getDeposiitInfo(){
        String depositUsersStr = "";
        int index = 0;
        for (String key : depositUsers.keySet()) {
            if (index > 0)
                depositUsersStr += ",";
            depositUsersStr += depositUsers.get(key).toString();
            index++;
        }
        return "{depositUsers:["+depositUsersStr+"]}";
    }

    @View
    public String getTokenAddress(){
        return this.henaTokenAddress.toString();
    }
    @View
    public String getHenaNulsPrice(){
        return "{nulsPrice:"+this.nulsPrice.toString()+", henaPrice:"+this.henaPrice.toString()+"}";
    }

    @View
    public BigInteger getTotPredictiveRewardAmount(){ //총 디파짓 된 량의 이자 총량을 계산한다
        BigInteger totalReward = BigInteger.ZERO;
        for (String key : depositUsers.keySet()) {
            totalReward = totalReward.add(calcDepositInfoReward(depositUsers.get(key), new HashMap<String, BigInteger>()));
        }
        return totalReward;
    }

    @View
    public DepositInfo getDepositInfoStr(@Required Address address){
        return getDepositInfo(address.toString());
    }

    @View
    public long createTime() {
        return createTime;
    }

    @View
    public long totalDepositAddressCount() {
        return totalDepositAddressCount;
    }

    @View
    public String totalDeposit() {
        return PocmUtil.toNuls(totalDeposit).toPlainString();
    }

    @View
    public BigInteger getMinimumDeposit(){
        return this.minimumDeposit;
    }

    @View
    public long minimumLocked() {
        return this.minimumLocked;
    }
    @View
    public double minimumRewardRate() {
        return this.minimumRewardRate;
    }

    @View
    public BigInteger getRemainHena(){
        return this.remainHena;
    }



    @View
    public long getTime(){
        if( endOfPOCM ){
            return POCMEndTime;
        }else{
            return Block.timestamp()/1000;
        }
    }

}
