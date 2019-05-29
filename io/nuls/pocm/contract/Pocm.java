/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
    private long minimumLocked;
    private Map<String, DepositInfo> depositUsers = new HashMap<String, DepositInfo>();
    private Map<Integer, DepositPolicy> deposiPolicys = new HashMap<Integer, DepositPolicy>();

    //private final long DAY_SEC = 60*60*24;
    private final long DAY_SEC = 2;
    private final long MONTH_SEC = DAY_SEC*30;
    private final long YEAR_SEC = MONTH_SEC*12;
    private BigInteger totalDeposit;

    private int totalDepositAddressCount;

    private static long DEPOSIT_NUMBER=1L;
    private static long USER_ID = 0;

    private BigInteger totHena;
    private BigInteger remainHena;

    private boolean miningFinish = false;

    private long POCMStartTime;

    private long rewardTokenLockMonth;

    BigDecimal henaPrice;
    BigDecimal nulsPrice;

    private final int decimals = 8;
    private BigDecimal minimumRewardRateInYear = BigDecimal.valueOf(0.1);

    private Address token;

    private boolean endOfPOCM = false;

    public Pocm(@Required Address tokenAddress) {
        super(new Address("Nse3yoTsQ9C4WMHGCrk3QJM9hnfNVgFh"), new Address("Nse3XYuRqyZBAEjnwTUPmzDqfmWSGfZK"));

        token = tokenAddress;
        createTime = getTime();
        totalDeposit = BigInteger.ZERO;
        totalDepositAddressCount = 0;

        minimumDeposit = PocmUtil.toNa(BigDecimal.valueOf(0.01)); //최소 1널스 이상
        totHena = PocmUtil.toNa(BigDecimal.valueOf(5000)); //50,000,000 Hena
        remainHena = totHena;

        rewardTokenLockMonth = 3; //3 month

        henaPrice = BigDecimal.valueOf(0.088);
        nulsPrice = BigDecimal.valueOf(0.92);


        minimumLocked = 60; //1분

        //POCMStartTime = 1555408748;  //생성자나 함수를 통해 시작시간 입력 받아야 한다
        POCMStartTime = 555408748;  //무조건 바로시작
        //policy setting
        int[] month ={3, 6, 9, 12};//month
        double[] rewardRate = {0.045, 0.125, 0.2625, 0.4};//18%, 25%, 35%, 40% rate of year
        setDepositPolicy(month,rewardRate);
    }

    public void setRewardTokenLockMonth(long lockMonth){
        requireManager(Msg.sender());
        this.rewardTokenLockMonth = lockMonth;
    }

    @View
    public long getRewardTokenLockMonth(){
        return this.rewardTokenLockMonth;
    }

    public void setDepositPolicy(@Required int[] months, @Required double[] rewardRates){
        requireManager(Msg.sender());
        for(int i=0; i< months.length; i++ ){
            long policyMonthSec = months[i]*MONTH_SEC;
            deposiPolicys.put(new Integer(months[i]), new DepositPolicy( policyMonthSec, rewardRates[i]));
        }
    }

    @View
    public String getDepositPolicy(){
        String str = "{";
        str+= "months:[";
        for (Integer key : deposiPolicys.keySet()) {
            str+= key.toString()+",";
        }
        str+= "],";

        str+= "depositPolicys:[";
        for (Integer key : deposiPolicys.keySet()) {
            str+= deposiPolicys.get(key).toString()+",";
        }
        str+= "],";

        str +="}";
        return str;
    }

    public void setPOCMStartTime(long startTime){
        requireManager(Msg.sender());
        this.POCMStartTime = startTime;
    }

    @View
    public String getPocmInfo(){

        String retStr = "{";
        retStr+="createTime:"+                 	createTime;
        retStr+=",minimumDeposit:"+              minimumDeposit.toString();
        retStr+=",minimumLocked:"+               minimumLocked;
        retStr+=",totalDeposit:"+              	 totalDeposit.toString();
        retStr+=",totalDepositAddressCount:"+     totalDepositAddressCount;
        retStr+=",DEPOSIT_NUMBER:"+               DEPOSIT_NUMBER;
        retStr+=",USER_ID:"+                      USER_ID;
        retStr+=",totHena:"+                     totHena.toString();
        retStr+=",remainHena:"+                   remainHena.toString();
        retStr+=",miningFinish:"+                  miningFinish;

        retStr+=",henaPrice:"+                     henaPrice.toString();
        retStr+=",nulsPrice:"+                     nulsPrice.toString();
        retStr+= "}";
        return retStr;
    }

    @View
    public String getDeposiitInfo(){
        String retStr = "{";
        Set<String> set = new HashSet<String>(depositUsers.keySet());
        for(String address:set){
            DepositInfo depositInfo = depositUsers.get(address);
            retStr+= depositInfo.toString();
        }
        retStr+= "}";
        return retStr;
    }

    private BigDecimal nuls2Hena(BigDecimal nulsAmount){
        return nulsAmount.multiply( nulsPrice.divide(henaPrice, decimals, BigDecimal.ROUND_DOWN));
    }

    private BigDecimal hena2Nuls(BigDecimal henaAmount){
        return henaAmount.multiply( henaPrice.divide(nulsPrice, decimals, BigDecimal.ROUND_DOWN));
    }

    public void setTokenAddress(@Required Address tokenAddress){
        requireManager(Msg.sender());
        this.token = tokenAddress;
    }

    @View
    public String getTokenAddress(){
        return this.token.toString();
    }

    public String updateHenaNulsPirce(@Required float nulsPrice,@Required float henaPrice){
        requireManager(Msg.sender());
        this.nulsPrice = new BigDecimal(nulsPrice);
        this.henaPrice = new BigDecimal(henaPrice);
        return "{nulsPrice:"+this.nulsPrice.toString()+", henaPrice:"+this.henaPrice.toString();
    }

    @View
    public String getHenaNulsPirce(){
        return "{nulsPrice:"+this.nulsPrice.toString()+", henaPrice:"+this.henaPrice.toString();
    }

    @View
    public String getTotPredictiveRewardAmount(){ //총 디파짓 된 량의 이자 총량을 계산한다
        Set<String> set = new HashSet<String>(depositUsers.keySet());
        BigInteger totalReward = BigInteger.ZERO;
        String retStr = "{[";
        for(String address:set){
            DepositInfo depositInfo = depositUsers.get(address);
            retStr += depositInfo.toString()+", ";
            Map<String,BigInteger> rewardResult= new HashMap<String, BigInteger>();
            totalReward = totalReward.add(calcRewardHena(depositInfo, rewardResult));
        }
        retStr += "], totalReward:"+totalReward.toString()+",  currentTime:"+ getTime() +"}";
        return retStr;
    }


    public boolean isEndOfPOCM(){
        //BigInteger currentTotalReward = getTotPredictiveRewardAmount();
//        if(remainHena.compareTo(currentTotalReward) <= 0){
//            endOfPOCM = true;
//            return true;
//        }
        return false;
    }


    private String receiveHena(DepositInfo depositInfo) {
        String resultStr = "";
        Map<String,BigInteger> rewardResult= new HashMap<String, BigInteger>();
        calcRewardHena(depositInfo, rewardResult);
        Set<String> set = new HashSet<String>(rewardResult.keySet());
        for(String rewardAddress:set){
            BigInteger rewardValue= rewardResult.get(rewardAddress);
            long tokenLockTime = getTime() + (MONTH_SEC * rewardTokenLockMonth);
            String[][] args = new String[][]{{rewardAddress},{ rewardValue.toString()}, {Long.toString(tokenLockTime)}};
            token.callWithReturnValue("transferPOCM", null, args, null);
            resultStr += ", rewardAddress:"+rewardAddress +", rewardValue:"+ rewardValue.toString()+", ";
        }
        return resultStr;
    }

    private BigInteger getRewardAmount(BigInteger  _depositAmount, long havingTime, DepositPolicy policy) {
        BigDecimal henaDepositAmount  =  nuls2Hena(new BigDecimal(_depositAmount.toString()));
        if (havingTime >= policy.policyTime) { // 디파짓 량에 대한 보상율 지급
            return henaDepositAmount.multiply(BigDecimal.valueOf(policy.rewardRate)).toBigInteger();
        } else if (havingTime >= policy.policyTime/2) { // 미니멈 보상 10%
            return henaDepositAmount.multiply(minimumRewardRate(policy, havingTime)).toBigInteger();
        }
        return BigInteger.ZERO;
    }

    private BigDecimal minimumRewardRate(DepositPolicy policy, long havingTime){
        
        return BigDecimal.valueOf(0.1).multiply(BigDecimal.valueOf(havingTime)).divide(BigDecimal.valueOf(YEAR_SEC), decimals, BigDecimal.ROUND_DOWN);
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

    private BigInteger calcRewardHena(DepositInfo depositInfo, Map<String,BigInteger> rewardResult) {
        BigInteger totalReward = BigInteger.ZERO;
        Map<Long,DepositDetailInfo> detailInfos=depositInfo.getDepositDetailInfos();
        long currentTime = getTime();
        for (Long key : detailInfos.keySet()) {
            DepositDetailInfo detailInfo = detailInfos.get(key);
            BigInteger reward = calcDepositDetailReward(detailInfo);
            String address = depositInfo.getDepositorAddress();
            totalReward = totalReward.add(reward);
            if( rewardResult.containsKey(address)){
                rewardResult.put(address,rewardResult.get(address).add(reward));
            }else{
                rewardResult.put(address,reward);
            }
        }
        return totalReward;
    }

    public String totalTest(){

        testDeposit(12);
        return "";
    }

    public void testDeposit( @Required int depositMonth) {
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

        BigInteger realValue = Msg.value();
        //require(value.compareTo(minimumDeposit) >= 0, "Please deposit more than "+minimumDeposit);
        long depositNumber =DEPOSIT_NUMBER++;

        DepositDetailInfo detailInfo = new DepositDetailInfo();

        BigInteger value = BigInteger.valueOf(100000000000L);
        detailInfo.setDepositAmount(value);

        long currentTime = getTime();
        if( currentTime < POCMStartTime)
            currentTime = POCMStartTime;

        detailInfo.setDepositTime(currentTime);
        detailInfo.setMiningAddress(Msg.sender().toString());
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
    public String testDeposit(@Required int depositMonth,@Required long havingDay) {

        String returnStr = "";

        DepositPolicy policy = deposiPolicys.get(new Integer(depositMonth));
        require( policy != null, "There is no policy for "+depositMonth + " month");

        String userAddress= Msg.sender().toString();

        DepositInfo depositInfo =new DepositInfo();
        depositInfo.setUserID(USER_ID++);
        //depositUsers.put(userAddress,info);

        BigInteger realValue = Msg.value();//디시멀이 붙어 있는값이지 체크해야한다

        returnStr += ", realValue:"+ realValue.toString();
        //require(value.compareTo(minimumDeposit) >= 0, "Please deposit more than "+minimumDeposit);
        long depositNumber =DEPOSIT_NUMBER++;

        DepositDetailInfo detailInfo = new DepositDetailInfo();
        BigInteger value = BigInteger.valueOf(100000000000L);
        detailInfo.setDepositAmount(value);
        //detailInfo.setDepositHeight(currentHeight);

        long currentBlockTime = getTime()- (DAY_SEC * havingDay);
        //if( currentBlockTime < POCMStartTime)
        //     currentBlockTime = POCMStartTime;

        detailInfo.setDepositTime(currentBlockTime);
        detailInfo.setMiningAddress(userAddress);
        detailInfo.setDepositNumber(depositNumber);

        detailInfo.setDepositPolicy(policy);

        depositInfo.setDepositorAddress(userAddress);
        depositInfo.getDepositDetailInfos().put(depositNumber,detailInfo);
        depositInfo.setDepositTotalAmount(depositInfo.getDepositTotalAmount().add(value));
        depositInfo.setDepositCount(depositInfo.getDepositCount()+1);

        //初始化挖矿信息
        //initMingInfo(currentBlockTime,miningAddress.toString(),userStr,depositNumber);
        //totalDeposit = totalDeposit.add(value);
        //emit(new DepositInfoEvent(info));

        //long depositNumber=0;
        //if(number!=null&&number.trim().length()>0){
          //  require(PocmUtil.canConvertNumeric(number.trim(),String.valueOf(Long.MAX_VALUE)),"The deposit number is incorrect.");
//            depositNumber= Long.valueOf(number.trim());
  //      }
        //Address user = Msg.sender();
        //DepositInfo depositInfo =getDepositInfo(user.toString());
        // 发放奖励
        //동완 아래쪽에서 checkAllDepositLocked를 호출하고 만약 최소 유지기간이 되지 않을경우
        //revert시키는데 아래 receiveHena에서 이미 transfer해버리면 문제의 소지가 있다.
        //시점을 변경해야한다.!!

            //DepositDetailInfo detailInfo =depositInfo.getDepositDetailInfoByNumber(depositNumber);
            //long unLockedTime = checkDepositLocked(detailInfo);
            //require(unLockedTime == -1, "최소유지기간이 경과하지 않았습니다" + unLockedTime);

        //기간이 충족되었을경우 해나를 보상으로 받는다.

        BigDecimal decimalResult = nuls2Hena(BigDecimal.valueOf(1)).multiply(BigDecimal.valueOf(policy.rewardRate));
        returnStr += receiveHena(depositInfo);
//
//        BigInteger deposit=BigInteger.ZERO;
//        if(depositNumber==0){
//            deposit=depositInfo.getDepositTotalAmount();
//            depositInfo.clearDepositDetailInfos();
//        }else{
//            DepositDetailInfo detailInfo =depositInfo.getDepositDetailInfoByNumber(depositNumber);
//            depositInfo.removeDepositDetailInfoByNumber(depositNumber);
//            deposit = detailInfo.getDepositAmount();
//            depositInfo.setDepositTotalAmount(depositInfo.getDepositTotalAmount().subtract(deposit));
//            depositInfo.setDepositCount(depositInfo.getDepositCount()-1);
//        }

        //totalDeposit = totalDeposit.subtract(deposit);
        //
        //Msg.sender().transfer(deposit);

        return returnStr +" nuls2hena_decimalResult:" + decimalResult.toString();

    }

    public String test(){
        //Address add = new Address(this);
        Address add = Msg.address();
        String[][] args = new String[][]{{add.toString()}};
        String balance = token.callWithReturnValue("balanceOf", null, args, null);
        remainHena = new BigInteger(balance);
        String retStr = "{";
        retStr += "add:" + add.toString();
        retStr += ",balance:" + balance;
        retStr += "}";
            return retStr;
        //addBalance(user, mingValue);
        //Address token; Address 형의 토큰 컨트렉주소에 대고
        //String decimal = token.callWithReturnValue("decimals", null, null, null); //디시멀 얻기 셈플
        //String[][] args = new String[][]{{beneficiary.toString()}, {tokenAmount.toPlainString()}}; //
        //token.call("transfer", null, args, null);
        //emit(new TransferEvent(null, user, mingValue));
    }


    public String testTrans(){
        //Address add = new Address(this);
        //Address add = Msg.address();
        BigInteger tokenAmount = BigInteger.valueOf(100000000); //1개를 보낸다.
        Address add = new Address("TTau7kAxyhc4yMomVJ2QkMVECKKZK1uG");
        //String[][] args = new String[][]{{add.toString()},{ tokenAmount.toString()}};
        //String balance = token.callWithReturnValue("transfer", null, args, null);
        String[][] args = new String[][]{{add.toString()},{ tokenAmount.toString()}, {"1558577197"}};
        String result = token.callWithReturnValue("transferPOCM", null, args, null);
        String retStr = "{";
        retStr += "add:" + add.toString();
        retStr += ",result:" + result;
        retStr += "}";
        return retStr;
        //addBalance(user, mingValue);
        //Address token; Address 형의 토큰 컨트렉주소에 대고
        //String decimal = token.callWithReturnValue("decimals", null, null, null); //디시멀 얻기 셈플
        //String[][] args = new String[][]{{beneficiary.toString()}, {tokenAmount.toPlainString()}}; //
        //token.call("transfer", null, args, null);
        //emit(new TransferEvent(null, user, mingValue));
    }



    @Payable
    public void depositForOther(@Required Address miningAddress, @Required int depositMonth) {

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
        //detailInfo.setDepositHeight(currentHeight);

        long currentTime = getTime();
        if( currentTime < POCMStartTime)
            currentTime = POCMStartTime;

        detailInfo.setDepositTime(currentTime);
        detailInfo.setMiningAddress(miningAddress.toString());
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
    public String deposit(@Required int depositMonth) {

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

        BigInteger value = Msg.value();//디시멀이 붙어 있는값이지 체크해야한다
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

        return getDepositInfo(Msg.sender()).toString() + getPocmInfo();
    }


    public void quitHena(String number) {
        long depositNumber=0;
        if(number!=null&&number.trim().length()>0){
            require(PocmUtil.canConvertNumeric(number.trim(),String.valueOf(Long.MAX_VALUE)),"The deposit number is incorrect.");
            depositNumber= Long.valueOf(number.trim());
        }
        Address user = Msg.sender();
        DepositInfo depositInfo =getDepositInfo(user.toString());
        if(depositNumber == 0){
            long result= checkAllDepositLocked(depositInfo);
            require(result == -1, "There is a deposit that has not passed the minimum period." );
        }else {
            DepositDetailInfo detailInfo =depositInfo.getDepositDetailInfoByNumber(depositNumber);
            long unLockedTime = checkDepositLocked(detailInfo);
            require(unLockedTime == -1, "The deposit has not passed the minimum period." + unLockedTime);
        }

        receiveHena(depositInfo);

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

    @View
    public DepositInfo getDepositInfo(@Required Address address){
        return getDepositInfo(address.toString());
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

    public void setMinimumDeposit(BigInteger minimumDeposit){
        requireManager(Msg.sender());
        this.minimumDeposit = minimumDeposit;
    }

    @View
    public long createTime() {
        return createTime;
    }

    @View
    public int totalDepositAddressCount() {
        return totalDepositAddressCount;
    }

    @View
    public String totalDeposit() {
        return PocmUtil.toNuls(totalDeposit).toPlainString();
    }

    @View
    public BigInteger minimumDeposit() {
        return this.minimumDeposit;
    }
    @View
    public long minimumLocked() {
        return this.minimumLocked;
    }

    @View
    public long getTime(){
        return Block.timestamp()/1000;
    }

    @View
    public BigInteger getMinimumDeposit(){
        return this.minimumDeposit;
    }

}
