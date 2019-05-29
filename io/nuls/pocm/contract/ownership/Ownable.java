package io.nuls.pocm.contract.ownership;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.annotation.View;
import static io.nuls.contract.sdk.Utils.require;

public class Ownable {

    private Address owner;
    private Address manager;

    public Ownable(Address owner, Address manager) {
        this.owner = owner;
        this.manager = manager;
    }

    @View
    public Address viewOwner() {
        return owner;
    }

    @View
    public Address viewManager() {
        return manager;
    }


    protected void requireOwner(Address address) {
        require(owner.equals(address), "Only owners are allowed");
    }

    protected void requireManager(Address address) {
        require(address.equals(owner) || address.equals(manager), "owners or manager are allowed");
    }
}
