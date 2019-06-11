package io.nuls.pocm.contract.ownership;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.Required;
import io.nuls.contract.sdk.annotation.View;
import static io.nuls.contract.sdk.Utils.require;

public class Ownable {

    private Address owner;
    private Address manager;

    public Ownable(Address owner) {
        this.owner = owner;
        this.manager = owner;
    }

    @View
    public Address viewOwner() {
        return owner;
    }

    @View
    public Address viewManager() {
        return manager;
    }

    public void setManager(@Required Address mgrAddress){
        requireOwner(Msg.sender());
        this.manager = mgrAddress;
    }

    protected void requireOwner(Address address) {
        require(owner.equals(address), "Only owners are allowed");
    }

    protected void requireManager(Address address) {
        require(address.equals(owner) || address.equals(manager), "owners or manager are allowed");
    }
}
